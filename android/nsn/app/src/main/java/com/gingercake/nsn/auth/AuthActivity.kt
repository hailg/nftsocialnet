package com.gingercake.nsn.auth

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.gingercake.nsn.BuildConfig
import com.gingercake.nsn.R
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.auth.viewmodel.AuthViewModel
import com.gingercake.nsn.auth.viewmodel.AuthViewModelFactory
import com.gingercake.nsn.databinding.ActivityAuthBinding
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.framework.displayToast
import com.gingercake.nsn.framework.hideKeyboard
import com.gingercake.nsn.main.MainActivity
import com.gingercake.nsn.model.user.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import nu.aaro.gustav.passwordstrengthmeter.PasswordStrengthCalculator
import java.util.*
import javax.inject.Inject
import kotlin.math.log

class AuthActivity : DaggerAppCompatActivity() {
    @Inject
    lateinit var authViewModelFactory: AuthViewModelFactory
    private lateinit var viewModel: AuthViewModel

    @Inject
    lateinit var db: FirebaseFirestore

    @Inject
    lateinit var functions: FirebaseFunctions

    @Inject
    lateinit var auth: FirebaseAuth

    private val passwordStrengthCalc = object : PasswordStrengthCalculator {
        private val specialRegex = Regex.fromLiteral(".*[!@#€£©§|≈$%^&*].*")
        private val numberRegex = Regex.fromLiteral(".*\\d.*")

        override fun calculatePasswordSecurityLevel(password: String): Int {
            var level = 0
            val minLength = minimumLength
            // Checks if password meets minimum length
            if (password.length < minLength) {
                return 0
            } else {
                ++level
            }
            // Give level for extra long password
            if (password.length >= minLength * (3.0 / 2)) ++level
            val hasUppercase = password != password.toLowerCase(Locale.getDefault())
            val hasLowercase = password != password.toUpperCase(Locale.getDefault())

            // Give level for password containing both upper and lower case letters
            if (hasLowercase && hasUppercase) ++level

            // Give level for password containing digits
            val hasNumber = password.matches(numberRegex)
            if (hasNumber) ++level

            // Give level for password containing special characters
            val hasSpecial = password.matches(specialRegex)
            if (hasSpecial) ++level
            return level
        }

        override fun getMinimumLength(): Int {
            return 8
        }

        override fun passwordAccepted(level: Int): Boolean {
            return level > 0
        }

        override fun onPasswordAccepted(password: String?) {}

    }
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this, authViewModelFactory).get(AuthViewModel::class.java)
        binding.passwordInputMeter.setEditText(binding.password.editText)
        binding.passwordInputMeter.setPasswordStrengthCalculator(passwordStrengthCalc)
        binding.nextBtn.setOnClickListener {
            binding.username.error = null
            binding.password.error = null
            binding.password2.error = null
            if (!validateUsername(binding.username)) {
                return@setOnClickListener
            }
            if (!validatePasswords(binding.password, binding.password2)) {
                return@setOnClickListener
            }
            binding.form.isVisible = false
            binding.status.isVisible = true
            hideKeyboard(binding.password2)
            hideKeyboard(binding.password)
            hideKeyboard(binding.username)
            val data = hashMapOf(
                "username" to binding.username.editText?.text.toString(),
                "password" to binding.password.editText?.text.toString(),
            )
            lifecycleScope.launch {
                try {
                    functions
                        .getHttpsCallable("blockchain_account-createAccount")
                        .call(data)
                        .continueWith { task ->
                            Log.d(TAG, "" + task.result)
                            task.result?.data
                        }
                        .await()
                    SessionManager.currentUser.username = binding.username.editText?.text.toString()
                    launchMainActivity()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create account", e)
                    binding.form.isVisible = true
                    binding.status.isVisible = false
                    val msg = if (e is FirebaseFunctionsException) {
                        e.details.toString()
                    } else {
                        "Failed to create your account. Please try again!"
                    }
                    MaterialAlertDialogBuilder(this@AuthActivity)
                        .setTitle(resources.getString(R.string.app_name))
                        .setMessage(msg)
                        .setPositiveButton(resources.getString(R.string.retry)) { _, _ ->
                        }
                        .show()
                }
            }
        }
        val user = auth.currentUser
        if (user == null) {
            login()
        } else {
            userSignedIn(user)
        }
    }

    private fun userSignedIn(user: FirebaseUser) {
        lifecycleScope.launch {
            try {
                var nsnUser = FirebaseFirestore
                    .getInstance().collection(Constants.USERS_COLLECTION)
                    .document(user.uid)
                    .get().await().toObject(User::class.java)
                if (nsnUser == null) {
                    nsnUser = User(user.uid,
                        user.email ?: "",
                        user.displayName ?: "",
                        user.photoUrl?.toString() ?: "")
                    viewModel.saveUser(nsnUser)
                }
                SessionManager.currentUser = nsnUser
                registerFCM(nsnUser);
                if (nsnUser.username.isEmpty()) {
                    binding.scrollView.isVisible = true;
                } else {
                    launchMainActivity()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sign you in. Please try again!", e)
                login();
            }
        }
    }

    private fun validatePasswords(password: TextInputLayout, password2: TextInputLayout): Boolean {
        val passwordTxt = password.editText?.text.toString() ?: ""
        val password2Txt = password2.editText?.text.toString() ?: ""
        if (passwordTxt.length < 8) {
            password.error = "Password must have at least 8 characters."
            password.editText?.setText("")
            password2.editText?.setText("")
            return false
        }

        if (passwordStrengthCalc.calculatePasswordSecurityLevel(passwordTxt) < 2) {
            password.error = "Password is too weak. Please use a combination of uppercase, lowercase, number and special characters."
            password.editText?.setText("")
            password2.editText?.setText("")
            return false
        }

        if (passwordTxt != password2Txt) {
            password2.error = "You'll need to retype the exact password"
            password.editText?.setText("")
            password2.editText?.setText("")
            return false
        }
        return true
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun validateUsername(username: TextInputLayout): Boolean {
        val usernameText = username.editText?.text.toString() ?: ""
        if (usernameText.length != 12) {
            username.error = "Your username must be exact 12 characters."
            return false
        }
        usernameText.find { !VALID_CHARACTERS.contains(it) }?.let {
            username.error = "Invalid character $it. Please only characters: a to z, 1 to 5 for your username."
            return false
        }
        username.endIconMode = TextInputLayout.END_ICON_CUSTOM
        username.endIconDrawable = getDrawable(R.drawable.ic_baseline_check_circle_outline_24)
        return true
    }

    private fun launchMainActivity() {
        val mainIntent = Intent(this, MainActivity::class.java)
        intent.extras?.let {
            for (key in it.keySet()) {
                mainIntent.putExtra(key, it.getString(key))
            }
        }
        startActivity(mainIntent)
        finish()
    }

    private fun registerFCM(user: User) {
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

        Firebase.messaging.token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            db
                    .collection(Constants.USERS_COLLECTION)
                    .document(user.uid)
                    .update("fcmTokens", FieldValue.arrayUnion(token))
                    .addOnFailureListener {
                        Log.w(TAG, "onUpdatingFCMToken: failed", it)
                    }
                    .addOnSuccessListener {
                        Log.d(TAG, "onUpdatingFCMToken: Successful")
                    }
        })

        Firebase.messaging.subscribeToTopic("general")
                .addOnCompleteListener { task ->
                    Log.d(TAG, "Subscribe to firebase general topic reuslt ${task.isSuccessful}")
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val user = auth.currentUser
            if (user != null) {
                userSignedIn(user)
                return
            }
            val response = IdpResponse.fromResultIntent(data)
            if (response == null) { // User pressed back button
                Log.d(TAG, "User press back button. Try to login again.")
                login()
                return
            }
            Log.e(TAG, "Failed to login$response", response.error)
            if (response.error?.errorCode == ErrorCodes.NO_NETWORK) {
                this.displayToast("Please check your network connection!")
                return;
            }
            this.displayToast("Unknown error. Please try again!")
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
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                        .build(),
                RC_SIGN_IN
        )
    }

    companion object {
        const val TAG = "AuthActivity"
        const val RC_SIGN_IN = 1000
        val VALID_CHARACTERS = "12345abcdefghijklmnopqrstuvwxyz".toCharArray().toSet()
    }
}