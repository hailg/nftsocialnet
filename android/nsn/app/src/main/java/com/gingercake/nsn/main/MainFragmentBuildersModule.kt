package com.gingercake.nsn.main

import com.gingercake.nsn.main.postdetail.PostDetailFragment
import com.gingercake.nsn.main.profile.ui.ProfileFragment
import com.gingercake.nsn.main.profile.di.ProfileScope
import com.gingercake.nsn.main.profile.di.ProfileViewModelModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MainFragmentBuildersModule {
    @ProfileScope
    @ContributesAndroidInjector(modules = [
        ProfileViewModelModule::class
    ])
    abstract fun contributePostsFragment(): ProfileFragment

    @ProfileScope
    @ContributesAndroidInjector
    abstract fun contributePosstDetailsFragment(): PostDetailFragment
}