package com.gingercake.nsn.main

import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module

@Module
abstract class ViewModelFactoryModule {
    @Binds
    internal abstract fun bindViewModelFactory(vmProvidersFactory: ViewModelProvidersFactory): ViewModelProvider.Factory
}