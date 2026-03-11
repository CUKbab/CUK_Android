package com.cukbab.data

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class AppVersion(
    val versionCode: Int,
    val versionName: String
)

interface VersionService {
    @GET("CUKbab/CUK_Menu/refs/heads/main/version.json")
    suspend fun getLatestVersion(): AppVersion
}

object VersionRepository {
    private const val BASE_URL = "https://raw.githubusercontent.com/"

    private val service: VersionService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VersionService::class.java)
    }

    suspend fun checkForUpdate(context: Context): AppVersion? {
        return withContext(Dispatchers.IO) {
            try {
                val latest = service.getLatestVersion()
                val current = VersionPreferences.getCurrentVersion(context)
                if (latest.versionCode > current) {
                    latest
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()))
        }
    }
}
