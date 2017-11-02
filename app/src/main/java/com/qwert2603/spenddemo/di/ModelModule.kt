package com.qwert2603.spenddemo.di

import android.arch.persistence.room.Room
import android.content.Context
import com.qwert2603.spenddemo.BuildConfig
import com.qwert2603.spenddemo.model.local_db.LocalDB
import com.qwert2603.spenddemo.model.remote_db.RemoteDB
import com.qwert2603.spenddemo.model.remote_db.RemoteDBImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ModelModule {
    @Provides
    @Singleton
    fun localDB(appContext: Context) = Room
            .databaseBuilder(appContext, LocalDB::class.java, "local_db")
            .build()

    @Provides
    @Singleton
    fun remoteDB(): RemoteDB = /*RemoteDBStub()*/ RemoteDBImpl(
            "jdbc:postgresql://192.168.1.26:5432/spend",
            "postgres",
            "1234"
    )

    @Provides
    @RemoteTableName
    fun remoteTableName(): String = BuildConfig.REMOTE_TABLE_NAME
}