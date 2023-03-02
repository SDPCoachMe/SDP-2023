package com.github.sdpcoachme.dependencyInjection

import com.github.sdpcoachme.repositories.FakeRepositoryAccess
import com.github.sdpcoachme.repositories.RepositoryAccess
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object AppModuleTest {
    @Provides
    @Singleton
    fun provideRepositoryAccess(): RepositoryAccess {
        return FakeRepositoryAccess()
    }
}