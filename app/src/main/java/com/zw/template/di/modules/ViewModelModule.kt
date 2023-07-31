package com.zw.template.di.modules

import com.zw.template.viewmodels.LocationViewModel
import dagger.Module
import dagger.Provides

@Module
class ViewModelModule {

    @Provides
    fun provideLocationViewModel(): LocationViewModel {
        return LocationViewModel()
    }
}