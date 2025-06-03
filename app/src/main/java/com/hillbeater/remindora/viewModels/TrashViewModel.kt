package com.hillbeater.remindora.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hillbeater.remindora.database.Reminder
import com.hillbeater.remindora.database.RemindoraDatabase
import com.hillbeater.remindora.database.UserInformation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrashViewModel(application: Application) : AndroidViewModel(application) {

    private val database = RemindoraDatabase.getDatabase(application)
    private val reminderDao = database.reminderDao()
    private val userInfoDao = database.userInfoDao()

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private val _userInfo = MutableStateFlow<UserInformation?>(null)
    val userInfo: StateFlow<UserInformation?> = _userInfo.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedItems = MutableStateFlow<MutableList<Int>>(mutableListOf())
    val selectedItems: StateFlow<MutableList<Int>> = _selectedItems.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _reminders.value = reminderDao.getAllTrashedReminders()
        }
        viewModelScope.launch {
            _userInfo.value = userInfoDao.getUserInfo()

        }
    }

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedItems.value = mutableListOf()
        }
    }

    fun toggleReminderSelection(reminderId: Int) {
        val currentSelection = _selectedItems.value.toMutableList()
        if (currentSelection.contains(reminderId)) {
            currentSelection.remove(reminderId)
        } else {
            currentSelection.add(reminderId)
        }
        _selectedItems.value = currentSelection
    }

    fun recoverSelectedReminders() {
        viewModelScope.launch {
            if (_selectedItems.value.isNotEmpty()) {
                _selectedItems.value.forEach { id ->
                    reminderDao.recoverReminder(id)
                }
            } else {
                reminderDao.recoverAllReminders()
            }
            refreshReminders()
        }
    }

    fun deleteSelectedRemindersPermanently() {
        viewModelScope.launch {
            if (_selectedItems.value.isNotEmpty()) {
                _selectedItems.value.forEach { id ->
                    reminderDao.deleteReminderPermanently(id)
                }
            } else {
                reminderDao.deleteAllTrashedReminders()
            }
            refreshReminders()
        }
    }

    private fun refreshReminders() {
        viewModelScope.launch {
            _reminders.value = reminderDao.getAllTrashedReminders()
            _selectedItems.value = mutableListOf()
            _isSelectionMode.value = false
        }
    }

    fun refreshUserInfo() {
        viewModelScope.launch {
            _userInfo.emit(userInfoDao.getUserInfo())
        }
    }
}
