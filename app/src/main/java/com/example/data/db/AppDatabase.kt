
package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.SongItem
import com.example.model.VideoStoryboard
import com.example.model.VirtualArtist

@Database(
    entities = [
        SongItem::class,
        VirtualArtist::class,
        VideoStoryboard::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao

    abstract fun virtualArtistDao(): VirtualArtistDao

    abstract fun videoStoryboardDao(): VideoStoryboardDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    "ALTER TABLE songs ADD COLUMN audioUrl TEXT"
                )

                database.execSQL(
                    "ALTER TABLE songs ADD COLUMN audioFilePath TEXT"
                )

                database.execSQL(
                    "ALTER TABLE songs ADD COLUMN durationSeconds INTEGER"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_music_studio.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}
