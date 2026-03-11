package com.cukbab.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query as RetrofitQuery

data class IssueRequest(
    val title: String,
    val body: String,
    val labels: List<String>,
    val userEmail: String? = null,
    val userId: String? = null
)

@IgnoreExtraProperties
data class Report(
    @get:Exclude val id: String = "",
    val title: String = "",
    val body: String = "",
    val labels: List<String> = emptyList(),
    val userId: String? = null,
    val userEmail: String? = null,
    val timestamp: Long = 0L
)

@IgnoreExtraProperties
data class BlockedUser(
    @get:Exclude val id: String = "",
    val email: String? = null,
    val timestamp: Long = 0L,
    val blockedUntil: Long? = null
)

// Wrapper for Admin reports to handle states
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
    private const val BASE_URL = "https://github-reporter.cukbab.workers.dev/"
    private val db get() = FirebaseFirestore.getInstance()

    val service: ReporterService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReporterService::class.java)
    }

    suspend fun getBlockedUser(userId: String): BlockedUser? {
        return try {
            val doc = db.collection("blocked_users").document(userId).get().await()
            if (!doc.exists()) return null
            
            val user = doc.toObject(BlockedUser::class.java)?.copy(id = doc.id)
            if (user?.blockedUntil != null && System.currentTimeMillis() > user.blockedUntil) {
                // Auto unblock if time has passed
                db.collection("blocked_users").document(userId).delete().await()
                android.util.Log.d("ReporterClient", "Auto-unblocked user: $userId")
                null
            } else {
                user
            }
        } catch (e: Exception) {
            android.util.Log.e("ReporterClient", "Error fetching blocked user: $userId", e)
            null
        }
    }

    suspend fun isUserBlocked(userId: String): Boolean {
        return getBlockedUser(userId) != null
    }

    suspend fun logReport(request: IssueRequest) {
        try {
            db.collection("reports").add(
                mapOf(
                    "title" to request.title,
                    "body" to request.body,
                    "labels" to request.labels,
                    "userId" to request.userId,
                    "userEmail" to request.userEmail,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
            android.util.Log.d("ReporterClient", "Report logged successfully in Firestore")
        } catch (e: Exception) {
            android.util.Log.e("ReporterClient", "Failed to log report to Firestore", e)
            // We don't rethrow here to allow the GitHub report to still proceed if Firestore fails
        }
    }

    suspend fun fetchChangelog(lang: String): String? {
        return try {
            val response = service.getChangelog(lang)
            if (response.isSuccessful) {
                response.body()
            } else {
                android.util.Log.e("ReporterClient", "Failed to fetch changelog: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ReporterClient", "Error fetching changelog", e)
            null
        }
    }

    // Admin Methods
    fun getAllReports(): Flow<ReportsResult> = callbackFlow {
        val subscription = try {
            db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("ReporterClient", "Reports snapshot listener error", error)
                        trySend(ReportsResult(error = error.localizedMessage, isLoading = false))
                        return@addSnapshotListener
                    }
                    val reports = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(Report::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) { 
                            android.util.Log.e("ReporterClient", "Error parsing report document: ${doc.id}", e)
                            null 
                        }
                    } ?: emptyList()
                    trySend(ReportsResult(reports = reports, isLoading = false))
                }
        } catch (e: Exception) {
            android.util.Log.e("ReporterClient", "Error in getAllReports flow", e)
            trySend(ReportsResult(error = e.localizedMessage, isLoading = false))
            null
        }
        awaitClose { subscription?.remove() }
    }

    fun getAllBlockedUsers(): Flow<BlockedUsersResult> = callbackFlow {
        val subscription = try {
            db.collection("blocked_users")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("ReporterClient", "Blocked users snapshot listener error", error)
                        trySend(BlockedUsersResult(error = error.localizedMessage, isLoading = false))
                        return@addSnapshotListener
                    }
                    val users = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(BlockedUser::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) { 
                            android.util.Log.e("ReporterClient", "Error parsing blocked user document: ${doc.id}", e)
                            null 
                        }
                    } ?: emptyList()
                    trySend(BlockedUsersResult(users = users, isLoading = false))
                }
        } catch (e: Exception) {
            android.util.Log.e("ReporterClient", "Error in getAllBlockedUsers flow", e)
            trySend(BlockedUsersResult(error = e.localizedMessage, isLoading = false))
            null
        }
        awaitClose { subscription?.remove() }
    }

    suspend fun blockUser(userId: String, email: String?, durationDays: Int? = null) {
        val blockedUntil = durationDays?.let { 
            System.currentTimeMillis() + (it * 24 * 60 * 60 * 1000L) 
        }
        
        db.collection("blocked_users").document(userId).set(
            mapOf(
                "email" to email,
                "timestamp" to System.currentTimeMillis(),
                "blockedUntil" to blockedUntil
            )
        ).await()
    }

    suspend fun unblockUser(userId: String) {
        db.collection("blocked_users").document(userId).delete().await()
    }

    suspend fun deleteReport(reportId: String) {
        db.collection("reports").document(reportId).delete().await()
    }
}
