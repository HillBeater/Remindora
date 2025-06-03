package com.hillbeater.remindora.uiScreens

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.hillbeater.remindora.R
import com.hillbeater.remindora.database.RemindoraDatabase
import com.hillbeater.remindora.database.UserInformation
import com.hillbeater.remindora.ui.theme.RemindoraTheme
import com.hillbeater.remindora.utils.GoogleSignInHelper
import com.hillbeater.remindora.viewModels.GoogleViewModel
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private val googleViewModel: GoogleViewModel by viewModels()

    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RemindoraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CreateAccountScreen()
                }
            }
        }
    }

    @Composable
    fun CreateAccountScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.colorBackground)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.colorText))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.splash_logo),
                        contentDescription = stringResource(id = R.string.app_name),
                        modifier = Modifier.size(180.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Welcome",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.colorBackground),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Sign in with Google",
                        fontSize = 18.sp,
                        color = colorResource(id = R.color.colorBackground),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = colorResource(id = R.color.colorPrimary),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .clickable { onGoogleSignInClick() }
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google),
                                contentDescription = "Google Image",
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = stringResource(id = R.string.sign_in),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(id = R.color.colorText)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = colorResource(id = R.color.colorPrimary)
                )
            }
        }
    }

    private fun onGoogleSignInClick() {
        val signInIntent = GoogleSignInHelper.getClient(this).signInIntent
        resultLauncher.launch(signInIntent)
    }

    private fun redirectToHome(name: String, userEmail: String, photoUrl: String) {
        val user = UserInformation(
            userName = name,
            userEmail = userEmail,
            userImageUrl = photoUrl
        )

        val db = RemindoraDatabase.getDatabase(this)

        lifecycleScope.launch {
            db.userInfoDao().insertUserInformation(user)

            startActivity(Intent(this@SignUpActivity, HomeActivity::class.java))
            finish()
        }
        isLoading = false
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            isLoading = true
            val data: Intent? = result.data

            googleViewModel.handleSignInResult(data) { account ->
                googleViewModel.signInWithGoogle(account,
                    onSuccess = {
                        val email = account.email ?: ""
                        val displayName = account.displayName ?: ""
                        val photoUrl = account.photoUrl?.toString() ?: ""

                        redirectToHome(name = displayName, userEmail = email, photoUrl = photoUrl)
                    },
                    onFailure = { message ->
                        isLoading = false
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } else {
            isLoading = false
        }
    }
}
