package com.hillbeater.remindora.viewModels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hillbeater.remindora.database.RemindoraDatabase
import com.hillbeater.remindora.database.UserInfoDao
import com.hillbeater.remindora.database.UserInformation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingViewModel(
    application: Application,
    private val userDao: UserInfoDao
) : AndroidViewModel(application) {

    private val sharedPrefs: SharedPreferences =
        application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val DARK_MODE_KEY = "dark_mode_enabled"
        private const val NOTIFICATION_KEY = "notification_enabled"
    }

    private val _isDarkModeEnabled = MutableStateFlow(
        sharedPrefs.getBoolean(DARK_MODE_KEY, false)
    )
    val isDarkModeEnabled: StateFlow<Boolean> = _isDarkModeEnabled.asStateFlow()

    private val _isNotificationEnabled = MutableStateFlow(
        sharedPrefs.getBoolean(NOTIFICATION_KEY, false)
    )
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled.asStateFlow()

    private val _userInfo = MutableStateFlow<UserInformation?>(null)
    val userInfo: StateFlow<UserInformation?> = _userInfo.asStateFlow()

    init {
        applyTheme(_isDarkModeEnabled.value)
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            _userInfo.value =  userDao.getUserInfo()
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(DARK_MODE_KEY, enabled).apply()
        _isDarkModeEnabled.value = enabled
        applyTheme(enabled)
    }

    fun toggleNotifications(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(NOTIFICATION_KEY, enabled).apply()
        _isNotificationEnabled.value = enabled
    }

    private fun applyTheme(isDark: Boolean) {
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun refreshUserInfo() {
        viewModelScope.launch {
            _userInfo.emit(userDao.getUserInfo())
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingViewModel::class.java)) {
                val userDao = RemindoraDatabase.getDatabase(application).userInfoDao()
                @Suppress("UNCHECKED_CAST")
                return SettingViewModel(application, userDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}