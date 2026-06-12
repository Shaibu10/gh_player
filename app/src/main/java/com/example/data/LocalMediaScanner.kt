package com.example.data

import android.content.Context
import android.provider.MediaStore
import android.util.Log

class LocalMediaScanner(private val context: Context) {

    fun scanVideos(): List<LocalMediaItem> {
        val list = mutableListOf<LocalMediaItem>()
        val localList = mutableListOf<LocalMediaItem>()

        // Now attempt to scan real local storage videos if permission exists
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED
            )

            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val pathCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val folderCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (c.moveToNext()) {
                    val id = c.getString(idCol)
                    val title = c.getString(titleCol)
                    val duration = c.getLong(durCol)
                    val path = c.getString(pathCol)
                    val size = c.getLong(sizeCol)
                    val folder = c.getString(folderCol) ?: "Videos"
                    val dateAdded = c.getLong(dateCol) * 1000L

                    localList.add(
                        LocalMediaItem(
                            id = "local_v_$id",
                            title = title,
                            path = path,
                            duration = duration,
                            size = size,
                            folder = folder,
                            mediaType = "video",
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("LocalMediaScanner", "Error scanning videos: ${e.message}")
        }

        if (localList.isNotEmpty()) {
            list.addAll(localList)
        } else {
            // Add preloaded Ghana-themed streaming videos only as a fallback
            list.addAll(getPreloadedVideos())
        }

        return list
    }

    fun scanAudios(): List<LocalMediaItem> {
        val list = mutableListOf<LocalMediaItem>()
        val localList = mutableListOf<LocalMediaItem>()

        // Now attempt to scan real local storage audios if permission exists
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED
            )

            // Query ALL audio files (including ringtones, downloads, whapp voice, audio clips) by setting selection to null
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val pathCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (c.moveToNext()) {
                    val id = c.getString(idCol)
                    val title = c.getString(titleCol)
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val duration = c.getLong(durCol)
                    val path = c.getString(pathCol)
                    val size = c.getLong(sizeCol)
                    val dateAdded = c.getLong(dateCol) * 1000L

                    localList.add(
                        LocalMediaItem(
                            id = "local_a_$id",
                            title = title,
                            artist = artist,
                            path = path,
                            duration = duration,
                            size = size,
                            folder = "Phone Storage",
                            mediaType = "audio",
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("LocalMediaScanner", "Error scanning audios: ${e.message}")
        }

        if (localList.isNotEmpty()) {
            list.addAll(localList)
        } else {
            // Add preloaded Ghana-themed streaming audios only as a fallback
            list.addAll(getPreloadedAudios())
        }

        return list
    }

    companion object {
        fun getPreloadedVideos(): List<LocalMediaItem> {
            return listOf(
                LocalMediaItem(
                    id = "stream_v_1",
                    title = "Ghana Tourism: Explore the Gold Coast",
                    artist = "Ghana Tourism Authority",
                    path = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    duration = 596000L,
                    size = 15800000L,
                    folder = "Downloads",
                    mediaType = "video",
                    isStream = true
                ),
                LocalMediaItem(
                    id = "stream_v_2",
                    title = "Sankofa Heritage - Official Film Trailer",
                    artist = "Accra Movie Guild",
                    path = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    duration = 653000L,
                    size = 18400000L,
                    folder = "Camera",
                    mediaType = "video",
                    isStream = true
                ),
                LocalMediaItem(
                    id = "stream_v_3",
                    title = "Ghanaian Cocoa Farms - Golden Seeds Documentary",
                    artist = "Agrik Ghana",
                    path = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    duration = 15000L,
                    size = 2500000L,
                    folder = "WhatsApp Video",
                    mediaType = "video",
                    isStream = true
                ),
                LocalMediaItem(
                    id = "stream_v_4",
                    title = "Accra Street Vibe - Independence Parade Short",
                    artist = "GH Shorts",
                    path = "https://storage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                    duration = 47000L,
                    size = 4000000L,
                    folder = "WhatsApp Video",
                    mediaType = "video",
                    isStream = true
                ),
                LocalMediaItem(
                    id = "stream_v_5",
                    title = "Ghana Highway Cruise - Cape Coast Road",
                    artist = "Travel Africa",
                    path = "https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
                    duration = 32000L,
                    size = 3100000L,
                    folder = "Instagram",
                    mediaType = "video",
                    isStream = true
                )
            )
        }

        fun getPreloadedAudios(): List<LocalMediaItem> {
            return listOf(
                LocalMediaItem(
                    id = "stream_a_1",
                    title = "Ghana National Anthem (Yɛn Ara Asaase Ni - Instrumental)",
                    artist = "Ghana Military Band",
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    duration = 372000L,
                    size = 5820000L,
                    folder = "Ghana Highlife",
                    mediaType = "audio",
                    isStream = true
                ),
                LocalMediaItem(
                    id = "stream_a_2",
                    title = "Kweku Afrobeat Jam - Accra Bounce",
                    artist = "Kweku & The Stars",
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    duration = 423000L,
                    size = 6410000L,
                    folder = "Ghana Highlife",
                    mediaType = "audio",
                    isStream = true
                ),
                LocalMediaItem(
                    id = "stream_a_3",
                    title = "Shatta Gold Coast (Club Anthem)",
                    artist = "Shatta Boy",
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    duration = 344000L,
                    size = 5120000L,
                    folder = "Downloads",
                    mediaType = "audio",
                    isStream = true
                ),
                LocalMediaItem(
                    id = "stream_a_4",
                    title = "Stonebwoy Tribute (Highlife Medley)",
                    artist = "Independence Band",
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    duration = 502000L,
                    size = 7950000L,
                    folder = "Ghana Highlife",
                    mediaType = "audio",
                    isStream = true
                ),
                LocalMediaItem(
                    id = "stream_a_5",
                    title = "Sarkodie Fast Rap Drill Beat",
                    artist = "Kumasi Drillers",
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    duration = 361000L,
                    size = 5510000L,
                    folder = "Downloads",
                    mediaType = "audio",
                    isStream = true
                )
            )
        }
    }
}
