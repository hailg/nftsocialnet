package com.gingercake.nsn

import com.gingercake.nsn.auth.AuthActivity
import com.gingercake.nsn.auth.AuthViewModelModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityBuilderModule {

    @ContributesAndroidInjector(modules = [
        AuthViewModelModule::class
    ])
    abstract fun contributeAuthActivity(): AuthActivity

}