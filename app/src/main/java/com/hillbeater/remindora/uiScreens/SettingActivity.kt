package com.hillbeater.remindora.uiScreens

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hillbeater.remindora.R
import com.hillbeater.remindora.viewModels.SettingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SettingActivity : AppCompatActivity() {

    private val viewModel: SettingViewModel by viewModels {
        SettingViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkModeEnabled by viewModel.isDarkModeEnabled.collectAsState()
            val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsState()
            val userInfo by viewModel.userInfo.collectAsState()

            val colorScheme = if (isDarkModeEnabled) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()

                userInfo?.let {
                    AppDrawer(
                        scaffoldState = drawerState,
                        userInfo = it,
                        selectedMenuItem = "Settings",
                        onMenuItemSelected = {},
                        coroutineScope = coroutineScope,
                        openActivity = { screen ->
                            val intent = when (screen) {
                                "Home" -> Intent(this@SettingActivity, HomeActivity::class.java)
                                "Trash" -> Intent(this@SettingActivity, TrashActivity::class.java)
                                else -> null
                            }
                            intent?.let {
                                startActivity(it)
                            }
                        }
                    ) {
                        Scaffold(
                            containerColor = colorResource(id = R.color.colorBackground),
                            topBar = {
                                TopAppBarWithTitle("Settings", drawerState, coroutineScope)
                            }
                        ) { paddingValues ->
                            Column(
                                modifier = Modifier
                                    .padding(paddingValues)
                                    .padding(16.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                SettingSwitchItem(
                                    title = "Dark Mode",
                                    subtitleOn = "Dark mode is now enabled for a darker UI experience.",
                                    subtitleOff = "Dark mode is turned off. UI will use light theme.",
                                    isChecked = isDarkModeEnabled,
                                    onToggle = { enabled ->
                                        viewModel.toggleDarkMode(enabled)
                                        showToast("Dark Mode ${if (enabled) "ON" else "OFF"}")
                                    }
                                )

                                SettingSwitchItem(
                                    title = "Notifications",
                                    subtitleOn = "Notifications are enabled",
                                    subtitleOff = "Notifications are disabled",
                                    isChecked = isNotificationEnabled,
                                    onToggle = { enabled ->
                                        viewModel.toggleNotifications(enabled)
                                        showToast("Notifications ${if (enabled) "ON" else "OFF"}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SettingSwitchItem(
        title: String,
        subtitleOn: String,
        subtitleOff: String,
        isChecked: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorResource(id = R.color.colorText)
                )
                Text(
                    text = if (isChecked) subtitleOn else subtitleOff,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorResource(id = R.color.colorText),
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp)
                )
            }
            Switch(
                checked = isChecked,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorResource(id = R.color.colorPrimary),
                    checkedTrackColor = colorResource(id = R.color.colorSecondary),
                    uncheckedThumbColor = colorResource(id = R.color.colorText),
                    uncheckedTrackColor = colorResource(id = R.color.colorSecondary)
                )
            )
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopAppBarWithTitle(
        title: String,
        scaffoldState: DrawerState,
        coroutineScope: CoroutineScope
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(id = R.color.colorBackground)),
            title = {
                Text(
                    title,
                    color = colorResource(id = R.color.colorText),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            navigationIcon = {
                IconButton(onClick = { coroutineScope.launch { scaffoldState.open() } }) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = colorResource(id = R.color.colorPrimary)
                    )
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUserInfo()
    }
}