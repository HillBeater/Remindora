package com.hillbeater.remindora.viewModels

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class GoogleViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun signInWithGoogle(account: GoogleSignInAccount?, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        account?.let {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        task.exception?.message?.let { message ->
                            onFailure(message)
                        }
                    }
                }
        } ?: onFailure("Google Sign-In failed")
    }

    fun handleSignInResult(data: Intent?, onSuccess: (GoogleSignInAccount) -> Unit) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        task.result?.let {
            onSuccess(it)
        }
    }
}