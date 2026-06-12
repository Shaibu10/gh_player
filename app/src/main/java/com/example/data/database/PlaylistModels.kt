package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songPath"])
data class PlaylistSong(
    val playlistId: Int,
    val songPath: String,
    val songTitle: String,
    val songArtist: String,
    val songDuration: Long,
    val songFolder: String,
    val songSize: Long,
    val isStream: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)
