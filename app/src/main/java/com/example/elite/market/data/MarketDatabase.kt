package com.example.elite.market.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.elite.ships.data.ShipUpgradeDao
import com.example.elite.ships.data.ShipUpgradeEntity

@Database(
    entities = [
        SystemCommodityEntity::class,
        SystemEconomyEntity::class,
        MarketPriceHistoryEntity::class,
        ShipUpgradeEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MarketDatabase : RoomDatabase() {

    abstract fun marketDao(): MarketDao
    abstract fun shipUpgradeDao(): ShipUpgradeDao

    companion object {
        @Volatile
        private var INSTANCE: MarketDatabase? = null

        fun getDatabase(context: Context): MarketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarketDatabase::class.java,
                    "elite_market_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
