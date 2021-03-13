package com.gingercake.nsn.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.gingercake.nsn.R
import com.gingercake.nsn.ViewModelProvidersFactory
import com.gingercake.nsn.databinding.ActivityAuthBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

class AuthActivity : DaggerAppCompatActivity() {
    @Inject
    lateinit var vmProviderFactory: ViewModelProvidersFactory

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, vmProviderFactory).get(AuthViewModel::class.java)

        checkUser()
    }

    override fun onResume() {
        super.onResume()
        checkUser()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                Log.d(
                    TAG,
                    "OnActivityResult User ${user.email}, ${user.displayName}, ${user.photoUrl}"
                )
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                Log.d(TAG, "Login failed")
            }
        }
    }

    private fun checkUser() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
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
        } else {
            Log.d(TAG, "On checkUser User ${user.email}, ${user.displayName}, ${user.photoUrl}")

            CheckBalanceTask(object : CheckBalanceTask.CheckBalanceTaskCallback {
                override fun update(updateContent: String) {
                    Log.d(TAG, "CheckBalanceTask update ${updateContent}")
                }

                override fun finish(success: Boolean, updateContent: String?, balance: String?) {
                    Log.d(TAG, "CheckBalanceTask finish ${success}, ${updateContent}, $balance")
                }
            }).execute("https://3a921a5762d4.ngrok.io", "bob")
        }
    }

    companion object {
        const val TAG = "AuthActivity"
        const val RC_SIGN_IN = 1000
    }
}