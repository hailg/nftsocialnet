package com.gingercake.nsn.main.di

import com.gingercake.nsn.main.blockchain.BlockChainFragment
import com.gingercake.nsn.main.home.ui.HomeFragment
import com.gingercake.nsn.main.hotposts.HotPostsFragment
import com.gingercake.nsn.main.newpost.NewPostFragment
import com.gingercake.nsn.main.newpost.NewPostSaleSettingFragment
import com.gingercake.nsn.main.postdetail.PostDetailFragment
import com.gingercake.nsn.main.profile.ui.ProfileFragment
import com.gingercake.nsn.main.profile.di.ProfileScope
import com.gingercake.nsn.main.profile.di.ProfileViewModelModule
import com.gingercake.nsn.main.purchasepost.PurchasePostFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MainFragmentBuildersModule {
    @ProfileScope
    @ContributesAndroidInjector(modules = [
        ProfileViewModelModule::class
    ])
    abstract fun contributeProfilesFragment(): ProfileFragment

    @ContributesAndroidInjector
    abstract fun contributePostDetailsFragment(): PostDetailFragment

    @ContributesAndroidInjector
    abstract fun contributeNewPostFragment(): NewPostFragment

    @ContributesAndroidInjector
    abstract fun contributeNewPostSaleSettingFragment(): NewPostSaleSettingFragment

    @ContributesAndroidInjector
    abstract fun contributeHomeFragment(): HomeFragment

    @ContributesAndroidInjector
    abstract fun contributeHotPostFragment(): HotPostsFragment

    @ContributesAndroidInjector
    abstract fun contributePurchasePostFragment(): PurchasePostFragment

    @ContributesAndroidInjector
    abstract fun contributeBlockchainFragment(): BlockChainFragment
}