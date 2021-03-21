package com.gingercake.nsn.main.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gingercake.nsn.main.MainViewModel
import com.gingercake.nsn.main.MainViewModelFactory
import com.gingercake.nsn.main.MainViewModelKey
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
    @MainViewModelKey(MainViewModel::class)
    internal abstract fun bindMainViewModel(viewModel: MainViewModel): ViewModel
}