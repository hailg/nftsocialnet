package com.gingercake.nsn

import com.gingercake.nsn.main.ViewModelFactoryModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidSupportInjectionModule::class,
    ActivityBuilderModule::class,
    AppModule::class,
    ViewModelFactoryModule::class
])
interface AppComponent : AndroidInjector<NSNApplication> {
    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: NSNApplication): AppComponent
    }
}