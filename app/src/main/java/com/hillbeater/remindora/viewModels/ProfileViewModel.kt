package com.hillbeater.remindora.viewModels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.hillbeater.remindora.R
import com.hillbeater.remindora.database.RemindoraDatabase
import com.hillbeater.remindora.database.UserInformation
import com.hillbeater.remindora.uiScreens.ProfileActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val database = RemindoraDatabase.getDatabase(application)
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    private val _userInfo = MutableLiveData<UserInformation?>()
    val userInfo: LiveData<UserInformation?> = _userInfo

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _userImageUri = MutableLiveData<String?>()
    val userImageUri: LiveData<String?> = _userImageUri

    private val _isUpdated = MutableLiveData<Boolean>()
    val isUpdated: LiveData<Boolean> = _isUpdated

    private val _showLogoutDialog = MutableLiveData<Boolean>()
    val showLogoutDialog: LiveData<Boolean> = _showLogoutDialog

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _navigateToSignUp = MutableLiveData<Boolean>()
    val navigateToSignUp: LiveData<Boolean> = _navigateToSignUp

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(application.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(application, gso)

        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val user = database.userInfoDao().getUserInfo()
            _userInfo.value = user
            _userName.value = user?.userName ?: ""
            _userEmail.value = user?.userEmail ?: ""
            _userImageUri.value = user?.userImageUrl?: ""
            checkIfUpdated()
        }
    }

    fun setUserName(name: String) {
        _userName.value = name
        checkIfUpdated()
    }

    fun setUserImageUri(uriString: String?) {
        _userImageUri.value = uriString
        checkIfUpdated()
    }

    fun checkIfUpdated() {
        val originalName = _userInfo.value?.userName ?: ""
        val originalEmail = _userInfo.value?.userEmail ?: ""
        val originalImageUri = _userInfo.value?.userImageUrl?: ""

        _isUpdated.value = _userName.value != originalName ||
                _userEmail.value != originalEmail ||
                _userImageUri.value != originalImageUri
    }

    fun updateProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserInfo = _userInfo.value
                if (currentUserInfo != null) {
                    val newUserInfo = UserInformation(
                        id = currentUserInfo.id,
                        userName = _userName.value ?: "",
                        userEmail = _userEmail.value ?: "",
                        userImageUrl = _userImageUri.value?.toString() ?: ""
                    )
                    database.userInfoDao().insertOrUpdate(newUserInfo)
                    withContext(Dispatchers.Main) {
                        _toastMessage.value = "Profile updated"
                        _toastMessage.value = null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _toastMessage.value = "Update failed"
                    _toastMessage.value = null
                }
            }
        }
    }

    fun showLogoutConfirmation() {
        _showLogoutDialog.value = true
    }

    fun dismissLogoutConfirmation() {
        _showLogoutDialog.value = false
    }

    fun logoutUser() {
        mGoogleSignInClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        database.userInfoDao().clearAllData()
                        withContext(Dispatchers.Main) {
                            _navigateToSignUp.value = true
                            _navigateToSignUp.value = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            _toastMessage.value = "Logout failed: Database error"
                            _toastMessage.value = null
                        }
                    }
                }
            } else {
                Log.e("ProfileViewModel", "Google sign out failed", task.exception)
                _toastMessage.value = "Logout failed: Google sign out"
                _toastMessage.value = null
            }
        }
    }

    class ProfileViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}