package com.gingercake.nsn.auth

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.facebook.AccessToken
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.gingercake.nsn.R
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.auth.viewmodel.AuthViewModel
import com.gingercake.nsn.auth.viewmodel.AuthViewModelFactory
import com.gingercake.nsn.databinding.ActivityAuthBinding
import com.gingercake.nsn.framework.displayToast
import com.gingercake.nsn.main.MainActivity
import com.gingercake.nsn.model.user.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.default_notification_channel_name)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(
                NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_LOW)
            )
        }

        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras?.get(key)
                Log.d(TAG, "Notification Key: $key Value: $value")
            }
        }

        Firebase.messaging.subscribeToTopic("general")
            .addOnCompleteListener { task ->
                Log.d(TAG, "Subscribe to firebase general topic reuslt ${task.isSuccessful}")
            }

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
                SessionManager.currentUser = nsnUser
                viewModel.saveUser(nsnUser)
                Log.d(
                    TAG,
                    "OnActivityResult User ${user.email}, ${user.displayName}, ${user.photoUrl}"
                )
                val mainIntent = Intent(this, MainActivity::class.java)
                intent.extras?.let {
                    for (key in it.keySet()) {
                        mainIntent.putExtra(key, it.getString(key))
                    }
                }
                startActivity(mainIntent)
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode != Activity.RESULT_OK) {
                this.displayToast(R.string.sign_in_error)
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