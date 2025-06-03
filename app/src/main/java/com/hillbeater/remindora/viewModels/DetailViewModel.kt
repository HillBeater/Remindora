package com.hillbeater.remindora.viewModels

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hillbeater.remindora.database.Reminder
import com.hillbeater.remindora.database.RemindoraDatabase
import com.hillbeater.remindora.utils.ReminderHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database = RemindoraDatabase.getDatabase(application)
    private val reminderHelper = ReminderHelper(application)

    // LiveData for UI state
    private val _itemId = MutableLiveData(0)
    val itemId: LiveData<Int> = _itemId

    private val _title = MutableLiveData("")
    val title: LiveData<String> = _title

    private val _date = MutableLiveData("")
    val date: LiveData<String> = _date

    private val _time = MutableLiveData("")
    val time: LiveData<String> = _time

    private val _content = MutableLiveData("")
    val content: LiveData<String> = _content

    private val _bgColor = MutableLiveData(androidx.compose.ui.graphics.Color.White)
    val bgColor: LiveData<androidx.compose.ui.graphics.Color> = _bgColor

    private val _textColor = MutableLiveData(androidx.compose.ui.graphics.Color.Black)
    val textColor: LiveData<androidx.compose.ui.graphics.Color> = _textColor

    private val _pushNotification = MutableLiveData(false)
    val pushNotification: LiveData<Boolean> = _pushNotification

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isEditMode = MutableLiveData(false)
    val isEditMode: LiveData<Boolean> = _isEditMode

    private val _showDeleteDialog = MutableLiveData(false)
    val showDeleteDialog: LiveData<Boolean> = _showDeleteDialog

    // Event for showing Date/Time Pickers (since they need Context from Activity)
    private val _showDatePickerEvent = MutableLiveData(false)
    val showDatePickerEvent: LiveData<Boolean> = _showDatePickerEvent

    private val _showTimePickerEvent = MutableLiveData(false)
    val showTimePickerEvent: LiveData<Boolean> = _showTimePickerEvent

    // Event for requesting exact alarm permission
    private val _requestExactAlarmPermission = MutableLiveData(false)
    val requestExactAlarmPermission: LiveData<Boolean> = _requestExactAlarmPermission

    // Event for navigation back to previous screen
    private val _navigateBack = MutableLiveData(false)
    val navigateBack: LiveData<Boolean> = _navigateBack

    // Original values to check for changes
    private var originalTitle: String = ""
    private var originalDate: String = ""
    private var originalTime: String = ""
    private var originalContent: String = ""
    private var originalBgColor: Int = 0
    private var originalTextColor: Int = 0
    private var originalPushNotification: Boolean = false

    // Derived state for save button enablement using MediatorLiveData
    val isSaveEnabled: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        fun updateSaveEnabled() {
            val currentTitle = _title.value ?: ""
            val currentDate = _date.value ?: ""
            val currentTime = _time.value ?: ""
            val currentContent = _content.value ?: ""
            val currentBgColor = (_bgColor.value ?: androidx.compose.ui.graphics.Color.White).toArgb()
            val currentTextColor = (_textColor.value ?: androidx.compose.ui.graphics.Color.Black).toArgb()
            val currentPushNotification = _pushNotification.value ?: false

            value = currentTitle != originalTitle ||
                    currentDate != originalDate ||
                    currentTime != originalTime ||
                    currentContent != originalContent ||
                    currentBgColor != originalBgColor ||
                    currentTextColor != originalTextColor ||
                    currentPushNotification != originalPushNotification
        }

        addSource(_title) { updateSaveEnabled() }
        addSource(_date) { updateSaveEnabled() }
        addSource(_time) { updateSaveEnabled() }
        addSource(_content) { updateSaveEnabled() }
        addSource(_bgColor) { updateSaveEnabled() }
        addSource(_textColor) { updateSaveEnabled() }
        addSource(_pushNotification) { updateSaveEnabled() }
    }


    fun loadReminder(reminderId: Int) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val item = database.reminderDao().getReminderById(reminderId)
            item?.let {
                _itemId.postValue(it.id)
                _title.postValue(it.title)
                _content.postValue(it.content ?: "")
                _date.postValue(it.date)
                _time.postValue(it.time)
                _bgColor.postValue(androidx.compose.ui.graphics.Color(it.bgColor))
                _textColor.postValue(androidx.compose.ui.graphics.Color(it.textColor))
                _pushNotification.postValue(it.pushNotification)

                // Store original values for comparison
                originalTitle = it.title
                originalContent = it.content ?: ""
                originalDate = it.date
                originalTime = it.time
                originalBgColor = it.bgColor
                originalTextColor = it.textColor
                originalPushNotification = it.pushNotification
            }
            _isLoading.postValue(false)
        }
    }

    fun setTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun setContent(newContent: String) {
        _content.value = newContent
    }

    fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        val selectedDate = Calendar.getInstance().apply {
            set(year, month, dayOfMonth)
        }
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        _date.value = formatter.format(selectedDate.time)
    }

    fun setTime(hourOfDay: Int, minute: Int) {
        _time.value = String.format("%02d:%02d", hourOfDay, minute)
    }

    fun setBgColor(color: androidx.compose.ui.graphics.Color) {
        _bgColor.value = color
    }

    fun setTextColor(color: androidx.compose.ui.graphics.Color) {
        _textColor.value = color
    }

    fun setPushNotification(enabled: Boolean) {
        _pushNotification.value = enabled
    }

    fun enableEditMode() {
        _isEditMode.value = true
    }

    fun cancelEditMode() {
        // Reset fields to original values when cancelling edit
        _title.value = originalTitle
        _content.value = originalContent
        _date.value = originalDate
        _time.value = originalTime
        _bgColor.value = androidx.compose.ui.graphics.Color(originalBgColor)
        _textColor.value = androidx.compose.ui.graphics.Color(originalTextColor)
        _pushNotification.value = originalPushNotification

        _isEditMode.value = false
    }

    fun showDeleteConfirmation() {
        _showDeleteDialog.value = true
    }

    fun dismissDeleteConfirmation() {
        _showDeleteDialog.value = false
    }

    fun showDatePicker() {
        _showDatePickerEvent.value = true
        // No need to reset immediately here, LaunchedEffect handles it.
    }

    fun showTimePicker() {
        _showTimePickerEvent.value = true
        // No need to reset immediately here, LaunchedEffect handles it.
    }


    fun updateReminder() {
        viewModelScope.launch(Dispatchers.IO) {
            val reminderData = Reminder(
                id = _itemId.value ?: 0,
                title = _title.value ?: "",
                date = _date.value ?: "",
                time = _time.value ?: "",
                content = _content.value ?: "",
                bgColor = (_bgColor.value ?: androidx.compose.ui.graphics.Color.White).toArgb(),
                textColor = (_textColor.value ?: androidx.compose.ui.graphics.Color.Black).toArgb(),
                pushNotification = _pushNotification.value ?: false,
                isDeleted = false // Ensure it's not marked as deleted during update
            )

            try {
                database.reminderDao().updateReminder(reminderData)

                if (reminderData.pushNotification) {
                    reminderHelper.cancelReminder(reminderData.id) // Cancel old if exists
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (canScheduleExactAlarms()) {
                            reminderHelper.setReminder(reminderData.id, reminderData.time, reminderData.date, reminderData.title)
                        } else {
                            _requestExactAlarmPermission.postValue(true) // Request permission
                        }
                    } else {
                        reminderHelper.setReminder(reminderData.id, reminderData.time, reminderData.date, reminderData.title)
                    }
                } else {
                    // If notification was disabled, cancel it
                    reminderHelper.cancelReminder(reminderData.id)
                }

                _isEditMode.postValue(false)
                _navigateBack.postValue(true) // Signal activity to navigate back

            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error updating reminder: ${e.message}")
                // Handle error, maybe expose a Toast message LiveData
            }
        }
    }

    fun deleteReminder(reminderId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reminderToDelete = database.reminderDao().getReminderById(reminderId)
                reminderToDelete?.let {
                    val updatedReminder = it.copy(isDeleted = true)
                    database.reminderDao().updateReminder(updatedReminder)
                    reminderHelper.cancelReminder(it.id) // Cancel any pending alarm
                    _navigateBack.postValue(true) // Signal activity to navigate back
                }
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error deleting reminder: ${e.message}")
                // Handle error
            } finally {
                _showDeleteDialog.postValue(false)
            }
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    class DetailViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DetailViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}