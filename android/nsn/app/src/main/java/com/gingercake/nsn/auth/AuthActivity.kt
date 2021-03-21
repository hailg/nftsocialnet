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
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.auth.viewmodel.AuthViewModel
import com.gingercake.nsn.auth.viewmodel.AuthViewModelFactory
import com.gingercake.nsn.databinding.ActivityAuthBinding
import com.gingercake.nsn.framework.displayToast
import com.gingercake.nsn.main.MainActivity
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
                SessionManager.currentUser = nsnUser
                viewModel.saveUser(nsnUser)
                Log.d(
                    TAG,
                    "OnActivityResult User ${user.email}, ${user.displayName}, ${user.photoUrl}"
                )
//                FirebaseFirestore
//                    .getInstance()
//                    .collection(Constants.POSTS_COLLECTION)
//                    .add(Post(
//                        id = "B5CCVKUTOZwl5CiSyziE",
//                        owner = "Hai Le Gia",
//                        title = "Test Title",
//                        content = "",
//                        resource = "https://canary.contestimg.wish.com/api/webimage/5d24728ab67b3f28509126f2-large.jpg?cache_buster=c0f26b46a60b47a2b2381cc718ecde21",
//                        timestamp = System.currentTimeMillis()
//                    ))
//                    .addOnSuccessListener {
//                            Log.d(TAG, "DocumentSnapshot successfully written!")
//                    }
//                    .addOnFailureListener {
//                            e -> Log.w(TAG, "Error writing document", e)
//                    }
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
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