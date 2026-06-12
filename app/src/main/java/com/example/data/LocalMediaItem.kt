package com.example.data

data class LocalMediaItem(
    val id: String,
    val title: String,
    val artist: String = "Unknown",
    val path: String,
    val duration: Long, // in ms
    val size: Long, // in bytes
    val folder: String, // WhatsApp, Bluetooth, Downloads, Camera, etc.
    val mediaType: String, // "video" or "audio"
    val isStream: Boolean = false,
    val audioTracks: List<String> = listOf("Twi (Ghana) [Commentary]", "English [Stereo]", "French [Surround]"),
    val subtitleUrl: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)
