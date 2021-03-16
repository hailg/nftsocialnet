package com.gingercake.nsn.main

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.gingercake.nsn.R
import com.gingercake.nsn.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.android.support.DaggerAppCompatActivity

class MainActivity : DaggerAppCompatActivity() {
    lateinit var bottomNavView: BottomNavigationView
    private var backPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bottomNavView = binding.bottomNavView
        setupViews(binding)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        Log.d(TAG, "onOptionsItemSelected: ")
        return true
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
                R.id.searchFragment,
                R.id.hotPostsFragment,
                R.id.profileFragment,
                R.id.postDetailFragment
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

    companion object {
        private const val TAG = "MainActivity"
    }
}