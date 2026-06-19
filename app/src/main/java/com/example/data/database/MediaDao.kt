package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<MediaHistory>>

    @Query("SELECT * FROM media_history WHERE mediaType = :mediaType ORDER BY timestamp DESC")
    fun getHistoryByType(mediaType: String): Flow<List<MediaHistory>>

    @Query("SELECT * FROM media_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<MediaHistory>>

    @Query("SELECT * FROM media_history WHERE path = :path LIMIT 1")
    suspend fun getMediaByPath(path: String): MediaHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaHistory)

    @Update
    suspend fun updateMedia(media: MediaHistory)

    @Query("DELETE FROM media_history WHERE id = :id")
    suspend fun deleteHistory(id: Int)

    @Query("DELETE FROM media_history")
    suspend fun clearHistory()

    @Query("UPDATE media_history SET isFavorite = 0")
    suspend fun clearAllFavorites()

    // Playlist Operations
    @Query("SELECT * FROM playlists ORDER BY dateCreated DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: Int): Playlist?

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY dateAdded ASC")
    fun getSongsForPlaylist(playlistId: Int): Flow<List<PlaylistSong>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(song: PlaylistSong)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Int)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteSongsOfPlaylist(playlistId: Int)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songPath = :songPath")
    suspend fun deletePlaylistSong(playlistId: Int, songPath: String)
}
