package com.gingercake.nsn

import com.gingercake.nsn.auth.AuthActivity
import com.gingercake.nsn.auth.AuthScope
import com.gingercake.nsn.auth.viewmodel.AuthViewModelModule
import com.gingercake.nsn.main.MainActivity
import com.gingercake.nsn.main.MainFragmentBuildersModule
import com.gingercake.nsn.main.MainScope
import com.gingercake.nsn.main.MainViewModelModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityBuilderModule {

    @AuthScope
    @ContributesAndroidInjector(modules = [
        AuthViewModelModule::class
    ])
    abstract fun contributeAuthActivity(): AuthActivity

    @MainScope
    @ContributesAndroidInjector(modules = [
//        MainViewModelModule::class,
        MainFragmentBuildersModule::class
    ])
    abstract fun contributeMainActivity(): MainActivity
}