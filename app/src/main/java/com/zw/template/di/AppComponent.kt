package com.zw.template.di

import com.zw.template.activities.ChooseLocationActivity
import com.zw.template.activities.MainActivity
import com.zw.template.activities.NewSearchLocationActivity
import com.zw.template.di.modules.ContextModule
import com.zw.template.di.modules.ViewModelModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [ContextModule::class, ViewModelModule::class]
)
interface AppComponent {
    // Classes that can be injected by this Component
    fun inject(application: ZwApplication)
    fun inject(activity: MainActivity)
    fun inject(activity: ChooseLocationActivity)
    fun inject(activity: NewSearchLocationActivity)
}