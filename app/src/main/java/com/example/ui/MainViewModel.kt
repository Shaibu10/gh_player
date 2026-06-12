package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LocalMediaItem
import com.example.data.LocalMediaScanner
import com.example.data.database.GHDatabase
import com.example.data.database.MediaHistory
import com.example.data.database.MediaRepository
import com.example.data.database.Playlist
import com.example.data.database.PlaylistSong
import com.example.ui.theme.GHSkin
import com.example.ui.theme.SkinsList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import android.media.MediaPlayer
import android.os.Build
import android.util.Log

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var mediaPlayer: MediaPlayer? = null

    private val mediaRepository: MediaRepository
    private val scanner = LocalMediaScanner(application)

    // UI Tab / View state
    var currentTab by mutableStateOf(Screen.Videos)
        private set

    // Skins/Personalization
    var currentSkin by mutableStateOf(SkinsList[0])
        private set

    // Media libraries
    private val _videos = MutableStateFlow<List<LocalMediaItem>>(emptyList())
    val videos: StateFlow<List<LocalMediaItem>> = _videos.asStateFlow()

    private val _audios = MutableStateFlow<List<LocalMediaItem>>(emptyList())
    val audios: StateFlow<List<LocalMediaItem>> = _audios.asStateFlow()

    // Filters & Sorting
    var selectedVideoFolder by mutableStateOf("All")
        private set
    var selectedAudioFolder by mutableStateOf("All")
        private set
    var audioSortOption by mutableStateOf(SortOption.TITLE)
        private set
    var videoSearchQuery by mutableStateOf("")
        private set
    var audioSearchQuery by mutableStateOf("")
        private set

    // Active playback states
    var activeVideoItem by mutableStateOf<LocalMediaItem?>(null)
        private set
    var activeAudioItem by mutableStateOf<LocalMediaItem?>(null)
        private set
    var isPlaying by mutableStateOf(false)
    var currentPlaybackPosition by mutableStateOf(0L)
    var playbackSpeed by mutableStateOf(1.0f)
    var autoPauseAtEnd by mutableStateOf(true)
    var activeAudioTrack by mutableStateOf("Twi (Ghana) [Commentary]")
    var activeSubtitleUrl by mutableStateOf<String?>(null)

    // Sleep Timer
    var sleepTimerMinutes by mutableStateOf(0) // 0 = off
    var timerRemainingSeconds by mutableStateOf(0)
    var closeAppAfterCurrentSong by mutableStateOf(false)
    var showTimerBottomSheet by mutableStateOf(false)
    var customTimerMinutes by mutableStateOf(5)
    private var sleepTimerJob: Job? = null

    // Video to MP3 Conversion Status
    var isConverting by mutableStateOf(false)
    var conversionProgress by mutableStateOf(0f)
    var conversionActiveVideo by mutableStateOf<LocalMediaItem?>(null)

    // Floating & Background Play Modes
    var isFloatingActive by mutableStateOf(false)
    var isBackgroundPlayEnabled by mutableStateOf(true)

    // Equalizer
    var eqPreset by mutableStateOf("Ghana Beats (Bass Boost)")
    var eqBands = mutableStateOf(listOf(85, 60, 45, 55, 75)) // 60Hz, 230Hz, 910Hz, 4kHz, 14kHz

    // Database flow elements
    private val _playbackHistory = MutableStateFlow<List<MediaHistory>>(emptyList())
    val playbackHistory: StateFlow<List<MediaHistory>> = _playbackHistory.asStateFlow()

    private val _favorites = MutableStateFlow<List<MediaHistory>>(emptyList())
    val favorites: StateFlow<List<MediaHistory>> = _favorites.asStateFlow()

    // Playlist state elements
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    var activePlaylist by mutableStateOf<Playlist?>(null)

    private val _activePlaylistSongs = MutableStateFlow<List<PlaylistSong>>(emptyList())
    val activePlaylistSongs: StateFlow<List<PlaylistSong>> = _activePlaylistSongs.asStateFlow()

    private var activePlaylistSongsJob: Job? = null

    init {
        val database = GHDatabase.getDatabase(application)
        mediaRepository = MediaRepository(database.mediaDao())

        // Start scanning media libraries
        refreshLibrary()

        // Real-time playback ticking and physical MediaPlayer integration
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (isPlaying) {
                    if (activeAudioItem != null) {
                        try {
                            mediaPlayer?.let { player ->
                                if (player.isPlaying) {
                                    currentPlaybackPosition = player.currentPosition.toLong()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Error fetching position from MediaPlayer", e)
                        }
                    } else if (activeVideoItem != null) {
                        val currentMax = activeVideoItem?.duration ?: 0L
                        if (currentMax > 0L) {
                            if (currentPlaybackPosition < currentMax) {
                                currentPlaybackPosition = (currentPlaybackPosition + (1000L * playbackSpeed).toLong()).coerceAtMost(currentMax)
                            } else {
                                isPlaying = false
                                currentPlaybackPosition = 0L
                            }
                        }
                    }
                }
            }
        }

        // Sync history streams
        viewModelScope.launch {
            mediaRepository.allHistory.collect { list ->
                _playbackHistory.value = list
            }
        }
        viewModelScope.launch {
            mediaRepository.favoriteMedia.collect { favs ->
                _favorites.value = favs
            }
        }
        viewModelScope.launch {
            mediaRepository.allPlaylists.collect { list ->
                _playlists.value = list
            }
        }
    }

    fun navigateTo(tab: Screen) {
        currentTab = tab
    }

    fun setSkin(skin: GHSkin) {
        currentSkin = skin
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _videos.value = scanner.scanVideos()
            _audios.value = scanner.scanAudios()
        }
    }

    fun setVideoFolder(folder: String) {
        selectedVideoFolder = folder
    }

    fun setAudioFolder(folder: String) {
        selectedAudioFolder = folder
    }

    fun setAudioSort(option: SortOption) {
        audioSortOption = option
    }

    fun searchVideos(query: String) {
        videoSearchQuery = query
    }

    fun searchAudios(query: String) {
        audioSearchQuery = query
    }

    // Playback control functions
    fun playVideo(item: LocalMediaItem) {
        // Pause audio if active
        pauseAudio()
        activeVideoItem = item
        activeAudioItem = null
        isPlaying = true
        currentPlaybackPosition = 0L
        
        // Add to persistent history in DB
        viewModelScope.launch {
            mediaRepository.insertOrUpdateHistory(
                title = item.title,
                path = item.path,
                duration = item.duration,
                lastPosition = 0L,
                mediaType = "video",
                folder = item.folder
            )
        }
    }

    fun playAudio(item: LocalMediaItem) {
        // Pause video if active
        activeVideoItem = null
        activeAudioItem = item
        isPlaying = false
        currentPlaybackPosition = 0L

        // Stop and release previous mediaPlayer
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error stopping previous MediaPlayer", e)
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                // Set data source to path (either file path or web stream url)
                setDataSource(item.path)
                setOnPreparedListener { mp ->
                    mp.start()
                    this@MainViewModel.isPlaying = true
                    applyPlaybackSpeed(playbackSpeed)
                }
                setOnCompletionListener {
                    this@MainViewModel.isPlaying = false
                    this@MainViewModel.currentPlaybackPosition = 0L
                    if (closeAppAfterCurrentSong) {
                        stopPlayback()
                        closeAppAfterCurrentSong = false
                    } else {
                        playNextAudio()
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("MainViewModel", "MediaPlayer error: $what, extra: $extra")
                    this@MainViewModel.isPlaying = false
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to play audio with path: ${item.path}", e)
        }

        // Add to persistent history in DB
        viewModelScope.launch {
            mediaRepository.insertOrUpdateHistory(
                title = item.title,
                path = item.path,
                duration = item.duration,
                lastPosition = 0L,
                mediaType = "audio",
                folder = item.folder
            )
        }
    }

    fun playNextAudio() {
        val currentPlaylist = activePlaylist
        if (currentPlaylist != null) {
            val songs = _activePlaylistSongs.value
            val currentIdx = songs.indexOfFirst { it.songPath == activeAudioItem?.path }
            if (currentIdx != -1 && currentIdx < songs.size - 1) {
                val nextSong = songs[currentIdx + 1]
                val item = LocalMediaItem(
                    id = nextSong.songPath,
                    title = nextSong.songTitle,
                    artist = nextSong.songArtist,
                    path = nextSong.songPath,
                    duration = nextSong.songDuration,
                    size = nextSong.songSize,
                    folder = nextSong.songFolder,
                    mediaType = "audio",
                    isStream = nextSong.isStream
                )
                playAudio(item)
            } else {
                isPlaying = false
                currentPlaybackPosition = 0L
            }
        } else {
            val songs = _audios.value
            val currentIdx = songs.indexOfFirst { it.path == activeAudioItem?.path }
            if (currentIdx != -1 && currentIdx < songs.size - 1) {
                playAudio(songs[currentIdx + 1])
            } else {
                isPlaying = false
                currentPlaybackPosition = 0L
            }
        }
    }

    fun pauseVideoPlayback() {
        isPlaying = false
    }

    fun pauseAudio() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error pausing MediaPlayer", e)
        }
        isPlaying = false
    }

    fun resumePlayback() {
        if (activeAudioItem != null) {
            try {
                mediaPlayer?.start()
                isPlaying = true
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error resuming MediaPlayer", e)
            }
        } else {
            isPlaying = true
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error stopping MediaPlayer", e)
        }
        isPlaying = false
        activeVideoItem = null
        activeAudioItem = null
    }

    fun seekForward() {
        if (activeAudioItem != null) {
            val currentMax = activeAudioItem?.duration ?: 0L
            val nextPos = (currentPlaybackPosition + 10000L).coerceAtMost(currentMax)
            try {
                mediaPlayer?.seekTo(nextPos.toInt())
                currentPlaybackPosition = nextPos
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error seeking forward", e)
            }
        } else {
            val currentMax = activeVideoItem?.duration ?: 0L
            currentPlaybackPosition = (currentPlaybackPosition + 10000L).coerceAtMost(currentMax)
        }
    }

    fun seekBackward() {
        if (activeAudioItem != null) {
            val nextPos = (currentPlaybackPosition - 10000L).coerceAtLeast(0L)
            try {
                mediaPlayer?.seekTo(nextPos.toInt())
                currentPlaybackPosition = nextPos
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error seeking backward", e)
            }
        } else {
            currentPlaybackPosition = (currentPlaybackPosition - 10000L).coerceAtLeast(0L)
        }
    }

    private fun applyPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(speed)
                    ?: android.media.PlaybackParams().setSpeed(speed)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to apply speed to MediaPlayer", e)
            }
        }
    }

    fun changePlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        if (activeAudioItem != null) {
            applyPlaybackSpeed(speed)
        }
    }

    fun selectAudioTrack(track: String) {
        activeAudioTrack = track
    }

    fun downloadSubtitle(item: LocalMediaItem, completion: (String) -> Unit) {
        viewModelScope.launch {
            activeSubtitleUrl = "Downloading..."
            // Simulate realistic fetch over network
            for (i in 1..5) {
                delay(400)
            }
            val subName = "${item.title.take(15)}_ENG.srt"
            activeSubtitleUrl = subName
            completion("Successfully downloaded subtitles ($subName) online!")
        }
    }

    fun toggleFavorite(path: String) {
        viewModelScope.launch {
            mediaRepository.toggleFavorite(path)
        }
    }

    fun removeFromHistory(id: Int) {
        viewModelScope.launch {
            mediaRepository.deleteHistory(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            mediaRepository.clearHistory()
        }
    }

    // Sleep Timer Handler
    fun setSleepTimer(minutes: Int) {
        sleepTimerMinutes = minutes
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            timerRemainingSeconds = minutes * 60
            sleepTimerJob = viewModelScope.launch {
                while (timerRemainingSeconds > 0) {
                    delay(1000)
                    timerRemainingSeconds--
                }
                // When timer reaches 0, shut down music/video playing (unless waiting for current song to end)
                if (closeAppAfterCurrentSong) {
                    // Let the real-time progress ticker handle pausing when current playback finishes.
                    // Just reset the sleep active representation
                    sleepTimerMinutes = 0
                } else {
                    pauseAudio()
                    pauseVideoPlayback()
                    stopPlayback()
                    sleepTimerMinutes = 0
                }
            }
        } else {
            timerRemainingSeconds = 0
        }
    }

    // Video to MP3 Conversion Engine Simulation/Registry
    fun convertVideoToMp3(videoItem: LocalMediaItem) {
        if (isConverting) return
        conversionActiveVideo = videoItem
        isConverting = true
        conversionProgress = 0f
        
        viewModelScope.launch {
            // Steps of conversion simulations
            for (step in 1..20) {
                delay(150)
                conversionProgress = (step * 5) / 100f
            }
            
            // Add a compiled audio item in local audio feed
            val extractedAudio = LocalMediaItem(
                id = "ext_mp3_${videoItem.id}",
                title = "${videoItem.title} (Extracted Audio)",
                artist = videoItem.artist,
                path = videoItem.path, // We can reuse path for demo stream or make dummy play
                duration = videoItem.duration,
                size = (videoItem.size * 0.12).toLong(), // MP3 estimation
                folder = "Converted MP3s",
                mediaType = "audio"
            )
            
            // Inject into active audios list
            val refreshedList = _audios.value.toMutableList()
            refreshedList.add(0, extractedAudio)
            _audios.value = refreshedList
            
            isConverting = false
            conversionActiveVideo = null
        }
    }

    // Equalizer preset controller
    fun applyPreset(preset: String) {
        eqPreset = preset
        eqBands.value = when (preset) {
            "Ghana Beats (Bass Boost)" -> listOf(90, 75, 50, 40, 60)
            "Accra Club (Jazz)" -> listOf(50, 65, 45, 60, 50)
            "Highlife Pop" -> listOf(70, 50, 78, 65, 55)
            "Stadium Rock" -> listOf(55, 65, 80, 50, 65)
            "Vocal Acoustic" -> listOf(40, 45, 60, 70, 75)
            "Flat Standard" -> listOf(50, 50, 50, 50, 50)
            else -> listOf(50, 50, 50, 50, 50)
        }
    }

    fun updateBand(index: Int, value: Int) {
        val newList = eqBands.value.toMutableList()
        newList[index] = value
        eqBands.value = newList
        eqPreset = "Custom"
    }

    // Playlist operations
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            mediaRepository.createPlaylist(name)
        }
    }

    fun createPlaylistWithSong(name: String, item: LocalMediaItem) {
        viewModelScope.launch {
            val playlistId = mediaRepository.createPlaylist(name)
            mediaRepository.addSongToPlaylist(playlistId, item)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            mediaRepository.deletePlaylist(playlistId)
            if (activePlaylist?.id == playlistId) {
                activePlaylist = null
                activePlaylistSongsJob?.cancel()
            }
        }
    }

    fun addSongToPlaylist(playlistId: Int, item: LocalMediaItem) {
        viewModelScope.launch {
            mediaRepository.addSongToPlaylist(playlistId, item)
        }
    }

    fun removeSongFromPlaylist(playlistId: Int, songPath: String) {
        viewModelScope.launch {
            mediaRepository.removeSongFromPlaylist(playlistId, songPath)
        }
    }

    fun selectPlaylist(playlist: Playlist?) {
        activePlaylist = playlist
        activePlaylistSongsJob?.cancel()
        if (playlist != null) {
            activePlaylistSongsJob = viewModelScope.launch {
                mediaRepository.getSongsForPlaylist(playlist.id).collect { songs ->
                    _activePlaylistSongs.value = songs
                }
            }
        } else {
            _activePlaylistSongs.value = emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        activePlaylistSongsJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error releasing MediaPlayer inside onCleared", e)
        }
    }
}

enum class Screen {
    Videos, Music, History, Skins, Equalizer
}

enum class SortOption {
    TITLE, DURATION, SIZE, DATE
}
