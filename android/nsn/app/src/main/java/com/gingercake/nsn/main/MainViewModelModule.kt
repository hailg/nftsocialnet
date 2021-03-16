package com.gingercake.nsn.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gingercake.nsn.main.profile.ui.ProfileViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class MainViewModelModule {
    @MainScope
    @Binds
    internal abstract fun bindViewModelFactory(vmProvidersFactory: MainViewModelFactory): ViewModelProvider.Factory

    @MainScope
    @Binds
    @IntoMap
    @MainViewModelKey(ProfileViewModel::class)
    internal abstract fun bindPostViewModel(viewModel: ProfileViewModel): ViewModel
}