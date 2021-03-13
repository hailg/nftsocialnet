package com.gingercake.nsn

import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication

class NSNApplication : DaggerApplication() {
    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.factory().create(this)
    }
}