package com.gingercake.nsn.auth

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.gingercake.nsn.ViewModelProvidersFactory
import com.gingercake.nsn.databinding.ActivityAuthBinding
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
        Log.d(TAG, "on craete $vmProviderFactory, $viewModel")
    }

    companion object {
        const val TAG = "AuthActivity"
    }
}