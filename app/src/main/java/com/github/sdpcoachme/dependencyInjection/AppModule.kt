package com.github.sdpcoachme.dependencyInjection

import android.content.Context
import androidx.room.Room
import com.github.sdpcoachme.database.AppDB
import com.github.sdpcoachme.network.RequestPoolAPI
import com.github.sdpcoachme.utility.Constants.BASE_URL
import com.github.sdpcoachme.utility.Constants.DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context) =
            Room.databaseBuilder(context, AppDB::class.java, DATABASE_NAME).build()

    @Singleton
    @Provides
    fun provideDao(database: AppDB) = database.userDB()

    //Create a retrofit object that will handle the connection to the api and covert them into JSon
    //create an object that can handle the request fromm the pool of commands
    @Singleton
    @Provides
    fun provideApi(): RequestPoolAPI {
        return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(RequestPoolAPI::class.java)
    }
}