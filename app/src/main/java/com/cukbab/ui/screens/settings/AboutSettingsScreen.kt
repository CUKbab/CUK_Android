package com.cukbab.ui.screens.settings

import android.content.Intent
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.cukbab.R
import com.cukbab.data.AppVersion
import com.cukbab.data.VersionRepository
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import kotlinx.coroutines.launch

@Composable
fun AboutSettingsScreen(onBack: () -> Unit, onUpdateFound: (AppVersion) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCheckingUpdates by remember { mutableStateOf(false) }
    
    BackHandler(onBack = onBack)

    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "Unknown"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.back))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            setImageResource(R.mipmap.ic_launcher)
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.version_label, versionName ?: "Unknown"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Update Section
            Text(
                text = stringResource(R.string.update),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                AboutItem(
                    icon = if (isCheckingUpdates) Icons.Default.Sync else Icons.Default.Update,
                    title = stringResource(R.string.check_for_updates),
                    onClick = {
                        if (!isCheckingUpdates) {
                            scope.launch {
                                isCheckingUpdates = true
                                val update = VersionRepository.checkForUpdate(context)
                                if (update == null) {
                                    Toast.makeText(context, R.string.on_latest_version, Toast.LENGTH_SHORT).show()
                                } else {
                                    onUpdateFound(update)
                                }
                                isCheckingUpdates = false
                            }
                        }
                    }
                )
            }

            // Info Sections
            Text(
                text = "Information",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    AboutItem(
                        icon = Icons.Default.Description,
                        title = stringResource(R.string.license_info),
                        onClick = {
                            context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                        }
                    )
                    
                    AboutItem(
                        icon = Icons.Default.Policy,
                        title = stringResource(R.string.privacy_policy),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://cukbab.github.io/CUK_Menu/privacy.html".toUri())
                            context.startActivity(intent)
                        }
                    )

                    AboutItem(
                        icon = Icons.Default.Code,
                        title = stringResource(R.string.source_code),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/CUKbab/CUK_Menu".toUri())
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "© 2026 CUK밥",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
