package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.SongItem
import com.example.model.VideoStoryboard
import com.example.model.VirtualArtist

@Database(
    entities = [SongItem::class, VirtualArtist::class, VideoStoryboard::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun virtualArtistDao(): VirtualArtistDao
    abstract fun videoStoryboardDao(): VideoStoryboardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_music_studio.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
