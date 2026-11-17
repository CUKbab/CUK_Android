package com.cukbab.update

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.cukbab.data.VersionRepository
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface UpdateState {
    data object Idle : UpdateState
    data class Available(val versionName: String) : UpdateState
    /** [progress] in 0..1, or null while the size is unknown. */
    data class Downloading(val progress: Float?) : UpdateState
    data object ReadyToInstall : UpdateState
}

/**
 * Flexible in-app updates through Google Play. When the app was not installed from Play
 * (debug or sideloaded builds), falls back to version.json and opens the Play Store page.
 *
 * Must be created in [ComponentActivity.onCreate] (registers an activity result launcher).
 */
class AppUpdater(private val activity: ComponentActivity) : DefaultLifecycleObserver {

    private val manager = AppUpdateManagerFactory.create(activity)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var playInfo: AppUpdateInfo? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.d(TAG, "Update flow declined or failed: ${result.resultCode}")
        }
    }

    private val installListener = InstallStateUpdatedListener { installState ->
        when (installState.installStatus()) {
            InstallStatus.PENDING, InstallStatus.DOWNLOADING -> {
                val total = installState.totalBytesToDownload()
                val progress = if (total > 0) installState.bytesDownloaded().toFloat() / total else null
                _state.value = UpdateState.Downloading(progress)
            }
            InstallStatus.DOWNLOADED -> _state.value = UpdateState.ReadyToInstall
            InstallStatus.FAILED, InstallStatus.CANCELED -> _state.value = UpdateState.Idle
            else -> Unit
        }
    }

    init {
        manager.registerListener(installListener)
        activity.lifecycle.addObserver(this)
    }

    private val isFromPlay: Boolean
        get() = try {
            activity.packageManager.getInstallSourceInfo(activity.packageName)
                .installingPackageName == PLAY_STORE_PACKAGE
        } catch (e: Exception) {
            false
        }

    /** @return true if an update is available or already downloaded. */
    suspend fun check(): Boolean {
        if (isFromPlay) {
            try {
                val info = manager.requestAppUpdateInfo()
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _state.value = UpdateState.ReadyToInstall
                    return true
                }
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    playInfo = info
                    val name = VersionRepository.checkForUpdate(activity)?.versionName
                        ?: info.availableVersionCode().toString()
                    _state.value = UpdateState.Available(name)
                    return true
                }
                return false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Play update check failed, falling back to version.json", e)
            }
        }
        playInfo = null
        val latest = VersionRepository.checkForUpdate(activity) ?: return false
        _state.value = UpdateState.Available(latest.versionName)
        return true
    }

    /** Starts the flexible download (Play) or opens the Play Store page (fallback). */
    fun startUpdate() {
        val info = playInfo
        _state.value = UpdateState.Idle
        if (info == null) {
            VersionRepository.openPlayStore(activity)
            return
        }
        try {
            manager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not start Play update flow", e)
            VersionRepository.openPlayStore(activity)
        }
    }

    fun dismiss() {
        if (_state.value is UpdateState.Available) _state.value = UpdateState.Idle
    }

    /** Restarts the app to install a downloaded update. */
    fun completeUpdate() {
        manager.completeUpdate()
    }

    override fun onResume(owner: LifecycleOwner) {
        // A download may have finished while the app was in the background.
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _state.value = UpdateState.ReadyToInstall
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        manager.unregisterListener(installListener)
    }

    private companion object {
        const val TAG = "AppUpdater"
        const val PLAY_STORE_PACKAGE = "com.android.vending"
    }
}
