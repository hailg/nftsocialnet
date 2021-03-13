package com.gingercake.nsn.main

import com.gingercake.nsn.main.posts.PostsFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MainFragmentBuildersModule {
    @ContributesAndroidInjector
    abstract fun contributePostsFragment(): PostsFragment
}