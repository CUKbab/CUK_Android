package com.cukbab.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cukbab.R
import java.text.SimpleDateFormat
import java.util.*

import com.cukbab.BuildConfig

enum class SettingsSubMenu {
    Main, Account, Display, Notifications, Admin, Widget, About
}

const val ADMIN_EMAIL = BuildConfig.ADMIN_EMAIL

enum class ReportType(val label: String) {
    MenuError("menu-error"),
    Feature("feature"),
    Bug("bug")
}

@Composable
fun SettingsMenuItem(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeedbackButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(if (enabled && !isLoading) 1f else 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            LoadingIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(icon, contentDescription = null)
        }
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun BannedDialog(
    blockedUntil: Long?,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val message = if (blockedUntil != null) {
        val dateStr = sdf.format(Date(blockedUntil))
        stringResource(R.string.banned_until, dateStr)
    } else {
        stringResource(R.string.perma_banned)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.user_blocked),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(message)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun ReportPolicyDialog(
    onDismiss: () -> Unit,
    onAgreed: () -> Unit
) {
    var policyAgreed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.feedback_support),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.report_policy_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Checkbox(
                        checked = policyAgreed,
                        onCheckedChange = { policyAgreed = it }
                    )
                    Text(
                        text = stringResource(R.string.report_policy_understood),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAgreed,
                enabled = policyAgreed
            ) {
                Text(stringResource(R.string.submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ReportDialog(
    reportType: ReportType,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val headerRes = when (reportType) {
                ReportType.MenuError -> R.string.report_menu_error
                ReportType.Feature -> R.string.suggest_feature
                ReportType.Bug -> R.string.report_bug
            }
            Text(stringResource(headerRes))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.issue_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.issue_description)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, description) },
                enabled = title.isNotBlank() && description.isNotBlank()
            ) {
                Text(stringResource(R.string.submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ReportStatusDialog(
    isSuccess: Boolean,
    @androidx.annotation.StringRes messageRes: Int,
    githubUrl: String?,
    onDismiss: () -> Unit,
    onOpenGithub: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val titleRes = if (isSuccess) R.string.report_success else R.string.report_error
            Text(
                text = stringResource(titleRes),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(stringResource(messageRes))
        },
        confirmButton = {
            Row {
                if (isSuccess && githubUrl != null) {
                    TextButton(onClick = { onOpenGithub(githubUrl) }) {
                        Text(stringResource(R.string.open_github))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    )
}

