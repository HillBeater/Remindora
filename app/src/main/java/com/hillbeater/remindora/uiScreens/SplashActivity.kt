package com.hillbeater.remindora.uiScreens

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import com.hillbeater.remindora.R
import com.hillbeater.remindora.database.RemindoraDatabase
import com.hillbeater.remindora.ui.theme.RemindoraTheme

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val darkModePrefExists = sharedPreferences.contains("dark_mode_enabled")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            sharedPreferences.edit().putBoolean("notification_enabled", granted).apply()
        } else {
            sharedPreferences.edit().putBoolean("notification_enabled", true).apply()
        }

        if (darkModePrefExists) {
            val isDarkMode = sharedPreferences.getBoolean("dark_mode_enabled", false)
            val mode = if (isDarkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        } else {
            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isDarkMode = when (currentNightMode) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> false
            }

            sharedPreferences.edit().putBoolean("dark_mode_enabled", isDarkMode).apply()

            val mode = if (isDarkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        setContent {
            RemindoraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashScreen { isLoggedIn ->
                        val intent = if (isLoggedIn) {
                            val fromPush = intent.getBooleanExtra("FROM_PUSH", false)
                            val reminderId = intent.getIntExtra("REMINDER_ID", -1)

                            Intent(this, HomeActivity::class.java).apply {
                                putExtra("FROM_PUSH", fromPush)
                                putExtra("REMINDER_ID", reminderId)
                            }
                        } else {
                            Intent(this, SignUpActivity::class.java)
                        }

                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

    @Composable
    fun SplashScreen(onResult: (Boolean) -> Unit) {
        val context = LocalContext.current
        val db = remember { RemindoraDatabase.getDatabase(context) }
        val isDarkMode = isSystemInDarkTheme()

        LaunchedEffect(Unit) {
            val userInfo = db.userInfoDao().getUserInfo()
            val isLoggedIn = userInfo?.userEmail?.isNotBlank() == true
            onResult(isLoggedIn)
        }

        val backgroundColor = if (isDarkMode) Color.Black else Color.White

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "App Logo"
            )
        }
    }
}

