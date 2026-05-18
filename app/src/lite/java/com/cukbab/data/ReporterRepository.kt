package com.cukbab.data

import com.cukbab.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query as RetrofitQuery

/**
 * Lite version of ReporterRepository without Firebase/Firestore dependencies.
 * Reporting and admin features are disabled in this version.
 */

data class IssueRequest(
    val title: String,
    val body: String,
    val labels: List<String>,
    val userEmail: String? = null,
    val userId: String? = null
)

data class Report(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val labels: List<String> = emptyList(),
    val userId: String? = null,
    val userEmail: String? = null,
    val timestamp: Long = 0L
)

data class BlockedUser(
    val id: String = "",
    val email: String? = null,
    val timestamp: Long = 0L,
    val blockedUntil: Long? = null
)

data class ReportsResult(
    val reports: List<Report> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = true
)

data class BlockedUsersResult(
    val users: List<BlockedUser> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = true
)

data class IssueResponse(
    val message: String? = null,
    val html_url: String? = null
)

interface ReporterService {
    @POST("/")
    suspend fun reportIssue(@Body request: IssueRequest): Response<IssueResponse>

    @GET("/changelog")
    suspend fun getChangelog(@RetrofitQuery("lang") lang: String): Response<String>
}

object ReporterClient {
    private val BASE_URL = BuildConfig.REPORTER_BASE_URL

    val service: ReporterService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReporterService::class.java)
    }

    suspend fun getBlockedUser(userId: String): BlockedUser? = null

    suspend fun isUserBlocked(userId: String): Boolean = false

    suspend fun logReport(request: IssueRequest) {
        // No-op in lite version
    }

    suspend fun fetchChangelog(lang: String): String? {
        return try {
            val response = service.getChangelog(lang)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Admin Methods (Disabled)
    fun getAllReports(): Flow<ReportsResult> = flowOf(ReportsResult(isLoading = false))

    fun getAllBlockedUsers(): Flow<BlockedUsersResult> = flowOf(BlockedUsersResult(isLoading = false))

    suspend fun blockUser(userId: String, email: String?, durationDays: Int? = null) {}

    suspend fun unblockUser(userId: String) {}

    suspend fun deleteReport(reportId: String) {}
}
