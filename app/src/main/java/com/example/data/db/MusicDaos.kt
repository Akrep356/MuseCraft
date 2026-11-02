package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.SongItem
import com.example.model.VideoStoryboard
import com.example.model.VirtualArtist
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY createdAt DESC")
    fun getAllSongs(): Flow<List<SongItem>>

    @Query("SELECT * FROM songs WHERE isCover = 1 ORDER BY createdAt DESC")
    fun getCoverSongs(): Flow<List<SongItem>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): SongItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongItem)

    @Update
    suspend fun updateSong(song: SongItem)

    @Delete
    suspend fun deleteSong(song: SongItem)
}

@Dao
interface VirtualArtistDao {
    @Query("SELECT * FROM virtual_artists ORDER BY createdAt DESC")
    fun getAllArtists(): Flow<List<VirtualArtist>>

    @Query("SELECT * FROM virtual_artists WHERE id = :id")
    suspend fun getArtistById(id: String): VirtualArtist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: VirtualArtist)

    @Delete
    suspend fun deleteArtist(artist: VirtualArtist)
}

@Dao
interface VideoStoryboardDao {
    @Query("SELECT * FROM video_storyboards ORDER BY createdAt DESC")
    fun getAllStoryboards(): Flow<List<VideoStoryboard>>

    @Query("SELECT * FROM video_storyboards WHERE songId = :songId")
    suspend fun getStoryboardForSong(songId: String): VideoStoryboard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryboard(storyboard: VideoStoryboard)

    @Delete
    suspend fun deleteStoryboard(storyboard: VideoStoryboard)
}
