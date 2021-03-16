package com.gingercake.nsn.main.profile.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gingercake.nsn.main.MainViewModelKey
import com.gingercake.nsn.main.profile.ui.ProfileViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ProfileViewModelModule {
    @ProfileScope
    @Binds
    internal abstract fun bindViewModelFactory(vmProvidersFactory: ProfileViewModelFactory): ViewModelProvider.Factory

    @ProfileScope
    @Binds
    @IntoMap
    @MainViewModelKey(ProfileViewModel::class)
    internal abstract fun bindPostViewModel(viewModel: ProfileViewModel): ViewModel
}