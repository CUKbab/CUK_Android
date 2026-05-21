package com.cukbab.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cukbab.R
import com.cukbab.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminPanelScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Reports", "Blocked Users")

    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> ReportsTab()
            1 -> BlockedUsersTab()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReportsTab() {
    val reportsFlow = remember { ReporterClient.getAllReports() }
    val result by reportsFlow.collectAsState(initial = ReportsResult())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (result.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
    } else if (result.error != null) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(text = "Error: ${result.error}", color = MaterialTheme.colorScheme.error)
        }
    } else if (result.reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_reports))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(result.reports, key = { it.id }) { report ->
                AdminReportCard(
                    report = report,
                    onBlock = { userId, duration ->
                        scope.launch {
                            ReporterClient.blockUser(userId, report.userEmail, duration)
                            Toast.makeText(context, R.string.user_status_updated, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDelete = {
                        scope.launch {
                            try {
                                ReporterClient.deleteReport(report.id)
                                Toast.makeText(context, R.string.report_deleted, Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Delete failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BlockedUsersTab() {
    val usersFlow = remember { ReporterClient.getAllBlockedUsers() }
    val result by usersFlow.collectAsState(initial = BlockedUsersResult())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (result.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
    } else if (result.error != null) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(text = "Error: ${result.error}", color = MaterialTheme.colorScheme.error)
        }
    } else if (result.users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No blocked users")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(result.users, key = { it.id }) { user ->
                AdminBlockedUserCard(user) {
                    scope.launch {
                        ReporterClient.unblockUser(user.id)
                        Toast.makeText(context, R.string.user_status_updated, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminBlockedUserCard(user: BlockedUser, onUnblock: () -> Unit) {
    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.email ?: "Unknown Email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "UID: ${user.id}", style = MaterialTheme.typography.labelSmall)
                if (user.blockedUntil != null) {
                    val untilDate = remember(user.blockedUntil) { sdf.format(Date(user.blockedUntil)) }
                    Text(
                        text = "Banned until: $untilDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Permanently Banned",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Button(
                onClick = onUnblock,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.unblock_user), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun AdminReportCard(
    report: Report,
    onBlock: (String, Int?) -> Unit,
    onDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = report.labels.joinToString(", "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Report",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(text = report.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = report.body, style = MaterialTheme.typography.bodyMedium)
            
            val dateFormatted = remember(report.timestamp) { sdf.format(Date(report.timestamp)) }
            Text(
                text = dateFormatted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (report.userEmail != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.alpha(0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "From: ${report.userEmail}", style = MaterialTheme.typography.labelMedium)
                        Text(text = "UID: ${report.userId}", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { report.userId?.let { onBlock(it, 7) } },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(stringResource(R.string.block_user_7d), fontSize = 9.sp)
                        }
                        Button(
                            onClick = { report.userId?.let { onBlock(it, null) } },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(stringResource(R.string.block_user_perma), fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}
