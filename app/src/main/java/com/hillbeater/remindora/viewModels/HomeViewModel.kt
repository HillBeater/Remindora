package com.hillbeater.remindora.viewModels

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hillbeater.remindora.database.Reminder
import com.hillbeater.remindora.database.RemindoraDatabase
import com.hillbeater.remindora.database.UserInformation
import com.hillbeater.remindora.utils.ReminderHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeViewModel(private val database: RemindoraDatabase, private val context: Context, private val sharedPreferences: SharedPreferences) : ViewModel() {

    private val _isNotificationEnabled = MutableStateFlow(false)
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled.asStateFlow()

    private val _userInfo = MutableStateFlow<UserInformation?>(null)
    val userInfo: StateFlow<UserInformation?> = _userInfo.asStateFlow()

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private val _showFab = MutableStateFlow(true)
    val showFab: StateFlow<Boolean> = _showFab.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FilterType.ByAddedDateTime)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()

    val filteredReminders: StateFlow<List<Reminder>> = _reminders.asStateFlow().also { flow ->
        viewModelScope.launch {
            flow.collect { currentReminders ->
                _reminders.update {
                    if (_searchText.value.isEmpty()) {
                        currentReminders
                    } else {
                        currentReminders.filter { it.title.contains(_searchText.value, ignoreCase = true) }
                    }
                }
            }
        }
    }

    init {
        fetchUserInfo()
        fetchReminders()
    }

    private fun fetchUserInfo() {
        viewModelScope.launch {
            _userInfo.value = database.userInfoDao().getUserInfo()
        }
    }

    fun fetchReminders() {
        viewModelScope.launch {
            val list = if (_showFab.value) {
                withContext(Dispatchers.IO) { database.reminderDao().getAllRemindersByCreatedTime() }
            } else {
                val remList = withContext(Dispatchers.IO) { database.reminderDao().getAllRemindersByDateAndTime() }
                filterUpcomingReminders(remList)
            }
            _reminders.value = list
        }
    }

    fun onSearchTextChanged(text: String) {
        _searchText.value = text
        filterRemindersBasedOnSearchAndFilter()
    }

    fun onFilterSelected(filterType: FilterType) {
        _selectedFilter.value = filterType
        viewModelScope.launch {
            _showFab.value = when (filterType) {
                FilterType.ByAddedDateTime -> true
                FilterType.UpcomingLatest, FilterType.Expired -> false
            }
            val newList = when (filterType) {
                FilterType.ByAddedDateTime -> database.reminderDao().getAllRemindersByCreatedTime()
                FilterType.UpcomingLatest -> filterUpcomingReminders(database.reminderDao().getAllRemindersByDateAndTime())
                FilterType.Expired -> getAllExpiredReminders(database.reminderDao().getAllRemindersByCreatedTime())
            }
            _reminders.value = newList
        }
    }

    private fun filterRemindersBasedOnSearchAndFilter() {
        viewModelScope.launch {
            val currentList = when (_selectedFilter.value) {
                FilterType.ByAddedDateTime -> database.reminderDao().getAllRemindersByCreatedTime()
                FilterType.UpcomingLatest -> filterUpcomingReminders(database.reminderDao().getAllRemindersByDateAndTime())
                FilterType.Expired -> getAllExpiredReminders(database.reminderDao().getAllRemindersByCreatedTime())
            }

            _reminders.value = if (_searchText.value.isEmpty()) {
                currentList
            } else {
                currentList.filter { it.title.contains(_searchText.value, ignoreCase = true) }
            }
        }
    }

    fun onAddReminder(
        title: String,
        date: String,
        time: String,
        content: String,
        bgColor: Int,
        textColor: Int,
        notify: Boolean,
        onComplete: () -> Unit,
        requestExactAlarmPermission: () -> Unit
    ) {
        if (title.isBlank() || date.isBlank() || time.isBlank()) {
            onComplete()
            return
        }

        val reminderData = Reminder(
            title = title,
            date = date,
            time = time,
            content = content,
            bgColor = bgColor,
            textColor = textColor,
            pushNotification = notify
        )

        viewModelScope.launch(Dispatchers.IO) {
            val generatedId = database.reminderDao().insertReminder(reminderData)

            if (notify) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (canScheduleExactAlarms()) {
                        ReminderHelper(context).setReminder(generatedId.toInt(), time, date, title)
                    } else {
                        requestExactAlarmPermission()
                    }
                } else {
                    ReminderHelper(context).setReminder(generatedId.toInt(), time, date, title)
                }
            }

            fetchReminders()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private fun getAllExpiredReminders(newList: List<Reminder>): List<Reminder> {
        return newList.filter { reminder ->
            isReminderExpired(reminder.date, reminder.time)
        }
    }

    fun isReminderExpired(date: String, time: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            val reminderDateTime = formatter.parse("$date $time")?.time ?: 0L
            val currentTime = System.currentTimeMillis()
            currentTime > reminderDateTime
        } catch (e: Exception) {
            false
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun filterUpcomingReminders(reminders: List<Reminder>): List<Reminder> {
        val now = LocalDateTime.now()
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        return reminders.filter { reminder ->
            val reminderDate = LocalDate.parse(reminder.date, dateFormatter)
            val reminderTime = LocalTime.parse(reminder.time)
            val reminderDateTime = LocalDateTime.of(reminderDate, reminderTime)
            reminderDateTime.isAfter(now) || reminderDateTime.isEqual(now)
        }
    }

    fun refreshUserInfo() {
        viewModelScope.launch {
            _userInfo.emit(database.userInfoDao().getUserInfo())
        }
    }

    fun toggleNotifications(isEnabled: Boolean) {
        _isNotificationEnabled.value = isEnabled
        sharedPreferences.edit().putBoolean("notification_enabled", isEnabled).apply()
    }

    enum class FilterType {
        ByAddedDateTime,
        UpcomingLatest,
        Expired
    }

    class ViewModelFactory(private val database: RemindoraDatabase, private val context: Context, private val sharedPreferences: SharedPreferences) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(database, context, sharedPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}