package com.hillbeater.remindora.uiScreens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.hillbeater.remindora.R
import com.hillbeater.remindora.ui.theme.RemindoraTheme
import com.hillbeater.remindora.viewModels.ProfileViewModel

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RemindoraTheme {
                val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.ProfileViewModelFactory(
                    application
                )
                )
                UserProfileScreen(profileViewModel = profileViewModel, onBack = { finish() })
            }
        }
    }

    private fun getPathFromUri(context: Context, uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                path = it.getString(columnIndex)
            }
        }
        return path
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UserProfileScreen(profileViewModel: ProfileViewModel, onBack: () -> Unit) {
        val context = LocalContext.current

        val userName by profileViewModel.userName.observeAsState("")
        val userEmail by profileViewModel.userEmail.observeAsState("")
        val userImageUri by profileViewModel.userImageUri.observeAsState(null)
        val isUpdated by profileViewModel.isUpdated.observeAsState(false)
        val showLogoutDialog by profileViewModel.showLogoutDialog.observeAsState(false)
        val toastMessage by profileViewModel.toastMessage.observeAsState(null)
        val navigateToSignUp by profileViewModel.navigateToSignUp.observeAsState(false)

        LaunchedEffect(toastMessage) {
            toastMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }

        LaunchedEffect(navigateToSignUp) {
            if (navigateToSignUp) {
                Intent(context, SignUpActivity::class.java).also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(it)
                }
                finishAffinity()
            }
        }

        val focusManager = LocalFocusManager.current

        Scaffold(
            containerColor = colorResource(id = R.color.colorBackground),
            topBar = {
                TopAppBar(
                    title = {
                        Text("Profile",
                            color = colorResource(id = R.color.colorText),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { profileViewModel.showLogoutConfirmation() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.logout_icon), tint = colorResource(id = R.color.colorPrimary),
                                contentDescription = "Logout"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = colorResource(id = R.color.colorBackground)
                    )
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            profileViewModel.updateProfile()
                            finish()
                            },
                        enabled = isUpdated,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.colorPrimaryButton),
                            disabledContainerColor = Color.LightGray
                        )
                    ) {
                        Text("Update", color = colorResource(id = R.color.colorText))
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (userImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(userImageUri),
                        contentDescription = "Selected image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo), // Placeholder
                        contentDescription = "Profile Placeholder",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = userName,
                    onValueChange = { profileViewModel.setUserName(it) },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.clearFocus()
                        }
                    ),
                    textStyle = TextStyle(color = colorResource(id = R.color.colorText)),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = colorResource(id = R.color.colorPrimary),
                        unfocusedBorderColor = colorResource(id = R.color.colorText),
                        cursorColor = colorResource(id = R.color.colorText),
                        focusedLabelColor = colorResource(id = R.color.colorPrimary),
                        unfocusedLabelColor = colorResource(id = R.color.colorText)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userEmail,
                    onValueChange = { /* Email is not editable by the user */ },
                    label = { Text("Email") },
                    enabled = false, // Email is not editable
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = colorResource(id = R.color.colorText)),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = colorResource(id = R.color.colorPrimary),
                        unfocusedBorderColor = colorResource(id = R.color.colorText),
                        cursorColor = colorResource(id = R.color.colorText),
                        focusedLabelColor = colorResource(id = R.color.colorPrimary),
                        unfocusedLabelColor = colorResource(id = R.color.colorText)
                    )
                )
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { profileViewModel.dismissLogoutConfirmation() },
                title = { Text("Confirm Sign out") },
                text = { Text("Are you sure you want to Sign out? All local data will be deleted.") },
                confirmButton = {
                    TextButton(onClick = {
                        profileViewModel.logoutUser()
                        profileViewModel.dismissLogoutConfirmation()

                        val intent = Intent(this, SignUpActivity::class.java)
                        startActivity(intent)
                        finishAffinity()
                    }) {
                        Text("Yes", color = colorResource(id = R.color.colorPrimary))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        profileViewModel.dismissLogoutConfirmation()
                    }) {
                        Text("No", color = colorResource(id = R.color.colorPrimary))
                    }
                }
            )
        }
    }
}