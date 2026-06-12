package com.example.data.database

import com.example.data.LocalMediaItem
import kotlinx.coroutines.flow.Flow

class MediaRepository(private val mediaDao: MediaDao) {
    val allHistory: Flow<List<MediaHistory>> = mediaDao.getAllHistory()
    val favoriteMedia: Flow<List<MediaHistory>> = mediaDao.getFavorites()

    // Playlist fields
    val allPlaylists: Flow<List<Playlist>> = mediaDao.getAllPlaylists()

    fun getSongsForPlaylist(playlistId: Int): Flow<List<PlaylistSong>> {
        return mediaDao.getSongsForPlaylist(playlistId)
    }

    suspend fun createPlaylist(name: String): Int {
        return mediaDao.insertPlaylist(Playlist(name = name)).toInt()
    }

    suspend fun deletePlaylist(playlistId: Int) {
        mediaDao.deleteSongsOfPlaylist(playlistId)
        mediaDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Int, item: LocalMediaItem) {
        val song = PlaylistSong(
            playlistId = playlistId,
            songPath = item.path,
            songTitle = item.title,
            songArtist = item.artist,
            songDuration = item.duration,
            songFolder = item.folder,
            songSize = item.size,
            isStream = item.isStream
        )
        mediaDao.insertPlaylistSong(song)
    }

    suspend fun removeSongFromPlaylist(playlistId: Int, songPath: String) {
        mediaDao.deletePlaylistSong(playlistId, songPath)
    }

    fun getHistoryByType(mediaType: String): Flow<List<MediaHistory>> {
        return mediaDao.getHistoryByType(mediaType)
    }

    suspend fun getMediaByPath(path: String): MediaHistory? {
        return mediaDao.getMediaByPath(path)
    }

    suspend fun insertOrUpdateHistory(
        title: String,
        path: String,
        duration: Long,
        lastPosition: Long,
        mediaType: String,
        folder: String
    ) {
        val existing = mediaDao.getMediaByPath(path)
        if (existing != null) {
            val updated = existing.copy(
                lastPosition = lastPosition,
                timestamp = System.currentTimeMillis()
            )
            mediaDao.updateMedia(updated)
        } else {
            val element = MediaHistory(
                title = title,
                path = path,
                duration = duration,
                lastPosition = lastPosition,
                mediaType = mediaType,
                folder = folder
            )
            mediaDao.insertMedia(element)
        }
    }

    suspend fun toggleFavorite(path: String) {
        val existing = mediaDao.getMediaByPath(path)
        if (existing != null) {
            val updated = existing.copy(isFavorite = !existing.isFavorite)
            mediaDao.updateMedia(updated)
        }
    }

    suspend fun deleteHistory(id: Int) {
        mediaDao.deleteHistory(id)
    }

    suspend fun clearHistory() {
        mediaDao.clearHistory()
    }
}
