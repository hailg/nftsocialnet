package com.gingercake.nsn.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.gingercake.nsn.R
import com.gingercake.nsn.auth.AuthActivity
import com.gingercake.nsn.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {
    lateinit var bottomNavView: BottomNavigationView
    private var backPressedOnce = false

    @Inject
    lateinit var mainViewModelFactory: MainViewModelFactory
    lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(this, mainViewModelFactory).get(MainViewModel::class.java)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bottomNavView = binding.bottomNavView
        setupViews(binding)
        Log.d(TAG, "onCreate ${mainViewModel}")

        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras?.get(key)
                Log.d(TAG, "Notification Key: $key Value: $value")
            }
        }
        mainViewModel.getAccountBalance()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sign_out) {
            MaterialAlertDialogBuilder(this)
                    .setTitle(resources.getString(R.string.app_name))
                    .setMessage(resources.getString(R.string.are_you_sure_you_want_to_quit))
                    .setNeutralButton(resources.getString(R.string.cancel_stay)) { dialog, which ->
                        // Respond to neutral button press
                    }
                    .setPositiveButton(resources.getString(R.string.yes_quit)) { dialog, which ->
                        signOut()
                    }
                    .show()
        }
        return false
    }
    override fun onBackPressed() {
        val navController = findNavController(R.id.fragNavHost)

        if (navController.graph.startDestination == navController.currentDestination?.id) {
            // Check if back is already pressed. If yes, then exit the app.
            if (backPressedOnce) {
                super.onBackPressed()
                return
            }

            backPressedOnce = true
            Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                backPressedOnce = false
            }, 2000L)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupViews(binding: ActivityMainBinding) {
        val navController = findNavController(R.id.fragNavHost)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.hotPostsFragment,
                R.id.profileFragment,
                R.id.blockChainFragment,
                R.id.postDetailFragment,
                R.id.newPostFragment,
                R.id.newPostSaleSettingFragment,
                R.id.purchasePostFragment
            )
        )
        NavigationUI.setupWithNavController(binding.bottomNavView, navController)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
    }

    fun showBottomNavigation() {
        bottomNavView.visibility = View.VISIBLE
    }

    fun hideBottomNavigation() {
        bottomNavView.visibility = View.GONE
    }

    fun createPost(postId: String, title: String, content: String,
                   resourcePath: String, resourceType: Int, price: String, password: String) {
        mainViewModel.createPost(postId, title, content, resourcePath, resourceType, price, password)
    }

    fun gotoProfilePage() {
        bottomNavView.selectedItemId = R.id.profileFragment
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                val authIntent = Intent(this@MainActivity, AuthActivity::class.java)
                startActivity(authIntent)
                finish()
            }
        }, 1000)

    }

    companion object {
        private const val TAG = "MainActivity"
    }
}