package com.cukbab.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.cukbab.R
import com.cukbab.data.*
import kotlinx.coroutines.launch

data class ReportResultState(
    val isSuccess: Boolean = false,
    @StringRes val messageRes: Int = R.string.report_error,
    val githubUrl: String? = null,
    val isVisible: Boolean = false
)

@Composable
fun SettingsMainScreen(
    onNavigate: (SettingsSubMenu) -> Unit,
    onShowChangelog: () -> Unit,
    onDisplayCoordsMeasured: (LayoutCoordinates) -> Unit = {},
    onWidgetCoordsMeasured: (LayoutCoordinates) -> Unit = {},
    onNotifCoordsMeasured: (LayoutCoordinates) -> Unit = {},
    onFeedbackCoordsMeasured: (LayoutCoordinates) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf<ReportType?>(null) }
    var bannedUser by remember { mutableStateOf<BlockedUser?>(null) }
    var isReporting by remember { mutableStateOf(false) }
    var reportResult by remember { mutableStateOf(ReportResultState()) }
    val currentUser by AuthRepository.currentUser.collectAsState()

    fun handleFeedbackClick(type: ReportType) {
        scope.launch {
            if (currentUser != null) {
                val blocked = ReporterClient.getBlockedUser(currentUser!!.uid)
                if (blocked != null) {
                    bannedUser = blocked
                    return@launch
                }
            }
            showDialog = type
        }
    }

    fun submitReport(title: String, body: String, labels: List<String>) {
        scope.launch {
            isReporting = true
            val request = IssueRequest(
                title = title,
                body = body,
                labels = labels,
                userId = currentUser?.uid,
                userEmail = currentUser?.email
            )

            try {
                ReporterClient.logReport(request)
                val response = ReporterClient.service.reportIssue(request)
                val bodyResponse = response.body()
                val isSuccess = response.code() == 201
                val resId = when (response.code()) {
                    201 -> R.string.report_success
                    409, 422 -> R.string.report_duplicate
                    else -> R.string.report_error
                }
                reportResult = ReportResultState(
                    isSuccess = isSuccess,
                    messageRes = resId,
                    githubUrl = bodyResponse?.html_url,
                    isVisible = true
                )
            } catch (e: Exception) {
                reportResult = ReportResultState(
                    isSuccess = false,
                    messageRes = R.string.report_error,
                    isVisible = true
                )
            } finally {
                isReporting = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        if (currentUser?.email == ADMIN_EMAIL) {
            SettingsMenuItem(
                title = stringResource(R.string.admin_panel),
                icon = Icons.Default.AdminPanelSettings,
                onClick = { onNavigate(SettingsSubMenu.Admin) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).alpha(0.5f))
        }

        SettingsMenuItem(
            title = stringResource(R.string.account_settings),
            icon = Icons.Default.AccountCircle,
            imageUrl = currentUser?.photoUrl?.toString(),
            onClick = { onNavigate(SettingsSubMenu.Account) }
        )
        
        SettingsMenuItem(
            title = stringResource(R.string.display_settings),
            icon = Icons.Default.Palette,
            modifier = Modifier.onGloballyPositioned { onDisplayCoordsMeasured(it) },
            onClick = { onNavigate(SettingsSubMenu.Display) }
        )
        
        SettingsMenuItem(
            title = stringResource(R.string.widget_settings),
            icon = Icons.Default.Widgets,
            modifier = Modifier.onGloballyPositioned { onWidgetCoordsMeasured(it) },
            onClick = { onNavigate(SettingsSubMenu.Widget) }
        )
        
        SettingsMenuItem(
            title = stringResource(R.string.notification_settings),
            icon = Icons.Default.Notifications,
            modifier = Modifier.onGloballyPositioned { onNotifCoordsMeasured(it) },
            onClick = { onNavigate(SettingsSubMenu.Notifications) }
        )
        
        SettingsMenuItem(
            title = stringResource(R.string.whats_new),
            icon = Icons.Default.NewReleases,
            onClick = onShowChangelog
        )
        SettingsMenuItem(
            title = stringResource(R.string.about),
            icon = Icons.Default.Info,
            onClick = { onNavigate(SettingsSubMenu.About) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(modifier = Modifier.onGloballyPositioned { onFeedbackCoordsMeasured(it) }) {
            Text(
                text = stringResource(R.string.feedback_support),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            FeedbackButton(
                text = stringResource(R.string.report_menu_error),
                icon = Icons.Default.RestaurantMenu,
                isLoading = isReporting,
                onClick = { 
                    handleFeedbackClick(ReportType.MenuError)
                }
            )
            FeedbackButton(
                text = stringResource(R.string.suggest_feature),
                icon = Icons.Default.Lightbulb,
                enabled = currentUser != null,
                isLoading = isReporting,
                onClick = { 
                    handleFeedbackClick(ReportType.Feature)
                }
            )
            FeedbackButton(
                text = stringResource(R.string.report_bug),
                icon = Icons.Default.BugReport,
                enabled = currentUser != null,
                isLoading = isReporting,
                onClick = { 
                    handleFeedbackClick(ReportType.Bug)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (bannedUser != null) {
        BannedDialog(
            blockedUntil = bannedUser!!.blockedUntil,
            onDismiss = { bannedUser = null }
        )
    }

    var showPolicyDialog by remember { mutableStateOf(false) }

    if (showPolicyDialog) {
        ReportPolicyDialog(
            onDismiss = { 
                showPolicyDialog = false 
                showDialog = null
            },
            onAgreed = {
                ReportPreferences.setPolicyAgreed(context, true)
                showPolicyDialog = false
            }
        )
    }

    if (showDialog != null && !showPolicyDialog) {
        if (!ReportPreferences.hasAgreedToPolicy(context)) {
            showPolicyDialog = true
        } else {
            ReportDialog(
                reportType = showDialog!!,
                onDismiss = { showDialog = null },
                onSubmit = { title, body ->
                    submitReport(
                        title = title,
                        body = body,
                        labels = listOf("reported-via-app", showDialog!!.label)
                    )
                    showDialog = null
                }
            )
        }
    }

    if (reportResult.isVisible) {
        ReportStatusDialog(
            isSuccess = reportResult.isSuccess,
            messageRes = reportResult.messageRes,
            githubUrl = reportResult.githubUrl,
            onDismiss = { reportResult = reportResult.copy(isVisible = false) },
            onOpenGithub = { url ->
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                context.startActivity(intent)
            }
        )
    }
}

