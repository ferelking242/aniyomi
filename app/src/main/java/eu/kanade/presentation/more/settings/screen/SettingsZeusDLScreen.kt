package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.zeus.ZeusArch
import eu.kanade.tachiyomi.zeus.ZeusUpdateManager
import eu.kanade.tachiyomi.zeus.ZeusUpdateState
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding

object SettingsZeusDLScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        val manager = remember { ZeusUpdateManager(context) }
        val updateState by manager.state.collectAsState()

        val prefs = manager.prefs
        var githubRepo by remember { mutableStateOf(prefs.githubRepo) }
        var githubPat by remember { mutableStateOf(prefs.githubPat) }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = "ZeusDL",
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            ScrollbarLazyColumn(contentPadding = contentPadding) {
                item {
                    Preference.PreferenceItem.InfoPreference(
                        title = "Installed version",
                        subtitle = manager.currentVersion(),
                    )
                }
                item {
                    Preference.PreferenceItem.InfoPreference(
                        title = "Architecture",
                        subtitle = "${manager.currentArch()} (device: ${ZeusArch.current().suffix})",
                    )
                }
                item {
                    Preference.PreferenceItem.InfoPreference(
                        title = "Update status",
                        subtitle = updateState.label(),
                    )
                }
                item {
                    Preference.PreferenceItem.TextPreference(
                        title = "Check for updates",
                        subtitle = "Compare with latest GitHub release",
                        onClick = {
                            scope.launch { manager.checkForUpdate(forceCheck = true, silent = false) }
                        },
                    )
                }
                if (updateState is ZeusUpdateState.UpdateAvailable) {
                    val avail = updateState as ZeusUpdateState.UpdateAvailable
                    item {
                        Preference.PreferenceItem.TextPreference(
                            title = "Install ${avail.version}",
                            subtitle = "Download and install the new binary",
                            onClick = { manager.downloadAndInstall(avail.version, avail.downloadUrl) },
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = githubRepo,
                        onValueChange = {
                            githubRepo = it
                            prefs.githubRepo = it
                        },
                        label = { Text("GitHub repository") },
                        placeholder = { Text("owner/repo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = githubPat,
                        onValueChange = {
                            githubPat = it
                            prefs.githubPat = it
                        },
                        label = { Text("GitHub Personal Access Token (optional)") },
                        placeholder = { Text("ghp_…") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                    )
                }
                item {
                    Preference.PreferenceItem.TextPreference(
                        title = "Clear stored token",
                        subtitle = "Remove the saved GitHub PAT",
                        onClick = {
                            prefs.clearPat()
                            githubPat = ""
                        },
                    )
                }
            }
        }
    }
}

private fun ZeusUpdateState.label(): String = when (this) {
    is ZeusUpdateState.Idle -> "Not checked yet"
    is ZeusUpdateState.Checking -> "Checking…"
    is ZeusUpdateState.UpdateAvailable -> "Update available: $version"
    is ZeusUpdateState.UpToDate -> "Up to date"
    is ZeusUpdateState.Downloading -> "Downloading… $percent%"
    is ZeusUpdateState.InstallSuccess -> "Installed $version"
    is ZeusUpdateState.Error -> "Error: $message"
}
