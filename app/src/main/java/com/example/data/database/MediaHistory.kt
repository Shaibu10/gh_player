package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_history")
data class MediaHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val path: String,
    val duration: Long,
    val lastPosition: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mediaType: String, // "video" or "audio"
    val folder: String, // WhatsApp, Downloads, Camera, etc.
    val isFavorite: Boolean = false
)
