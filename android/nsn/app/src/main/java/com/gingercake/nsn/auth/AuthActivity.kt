package com.gingercake.nsn.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.facebook.AccessToken
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.gingercake.nsn.R
import com.gingercake.nsn.auth.viewmodel.AuthViewModel
import com.gingercake.nsn.auth.viewmodel.AuthViewModelFactory
import com.gingercake.nsn.databinding.ActivityAuthBinding
import com.gingercake.nsn.framework.StateMessageCallback
import com.gingercake.nsn.framework.displayToast
import com.gingercake.nsn.model.user.User
import com.google.firebase.auth.FirebaseAuth
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

class AuthActivity : DaggerAppCompatActivity() {
    @Inject
    lateinit var authViewModelFactory: AuthViewModelFactory
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, authViewModelFactory).get(AuthViewModel::class.java)
        FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                login()
            } else {
                Log.d(TAG, "User is here ${AccessToken.getCurrentAccessToken()}")
                val nsnUser = User(user.uid,
                    user.email ?: "",
                    user.displayName ?: "",
                    user.photoUrl?.toString() ?: "")
                viewModel.saveUser(nsnUser)
                Log.d(
                    TAG,
                    "OnActivityResult User ${user.email}, ${user.displayName}, ${user.photoUrl}"
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode != Activity.RESULT_OK) {
                this.displayToast(R.string.sign_in_error, object : StateMessageCallback {
                    override fun removeMessageFromStack() {
                    }
                })
                Log.d(TAG, "Login failed", response?.error)
            }
        }
    }

    private fun login() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.FacebookBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.mipmap.logo)
                .setTheme(R.style.LoginTheme)
                .build(),
            RC_SIGN_IN
        )
    }

    companion object {
        const val TAG = "AuthActivity"
        const val RC_SIGN_IN = 1000
    }
}