package com.fortune.tejiebox.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CustomerServiceInfo::class], version = 100, exportSchema = false)
abstract class CustomerServiceInfoDataBase : RoomDatabase() {
    abstract fun customerServiceInfoDao(): CustomerServiceInfoDao

    companion object {

        @Volatile
        private var INSTANCE: CustomerServiceInfoDataBase? = null

        fun getDataBase(context: Context): CustomerServiceInfoDataBase {
            return INSTANCE ?: synchronized(this) {
                var instance = Room.databaseBuilder(
                    context.applicationContext,
                    CustomerServiceInfoDataBase::class.java,
                    "customer_service.db"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }

    }
}