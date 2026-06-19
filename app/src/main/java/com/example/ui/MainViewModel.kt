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
import android.content.Intent
import com.example.service.PlaybackService

enum class RepeatMode {
    OFF, ALL, ONE
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var isShuffleEnabled by mutableStateOf(false)
    var repeatMode by mutableStateOf(RepeatMode.OFF)

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
    var lastManualSeekTimeMs by mutableStateOf(0L)
        private set
    var playbackSpeed by mutableStateOf(1.0f)
    var autoPauseAtEnd by mutableStateOf(true)
    var activeAudioTrack by mutableStateOf("Twi (Ghana) [Commentary]")
    var activeSubtitleUrl by mutableStateOf<String?>(null)

    // Robust Subtitle Settings and States
    var subtitleLanguage by mutableStateOf("English")
    var subtitleOffsetMs by mutableStateOf(0L) // Support dynamic sync delay (±3000ms)
    var subtitleTextSize by mutableStateOf(14f) // FontSize sp support (10..24)
    var subtitleColor by mutableStateOf("Yellow") // Yellow, White, Green
    var isDownloadingSubtitle by mutableStateOf(false)
    var subtitleDownloadError by mutableStateOf<String?>(null)

    // Dynamic state-driven subtitle track computation
    fun getDynamicSubtitleText(positionMs: Long): String? {
        val activeSub = activeSubtitleUrl ?: return null
        if (activeSub == "Downloading...") return "📥 Downloading subtitle track mappings..."
        
        val adjustedPos = (positionMs + subtitleOffsetMs).coerceAtLeast(0L)
        val currentMax = activeVideoItem?.duration ?: 0L
        if (currentMax > 0 && adjustedPos > currentMax) return null

        val sec = ((adjustedPos / 1000) % 60).toInt()
        val min = ((adjustedPos / 60000) % 60).toInt()
        val secStr = String.format("%02d:%02d", min, sec)

        val videoTitle = activeVideoItem?.title ?: ""

        // Map language tracks
        val isTwi = subtitleLanguage.contains("Twi", ignoreCase = true) || subtitleLanguage.contains("Akan", ignoreCase = true)
        val isFrench = subtitleLanguage.contains("French", ignoreCase = true)

        return if (videoTitle.contains("Explore the Gold Coast", ignoreCase = true) || videoTitle.contains("Tourism", ignoreCase = true)) {
            when (adjustedPos) {
                in 0L..4000L -> if (isTwi) "💬 [$secStr] Akwaaba kɔ Ghana, abibiman kpono kɛseɛ no." else if (isFrench) "💬 [$secStr] Bienvenue au Ghana, la porte d'or de l'Afrique." else "💬 [$secStr] Welcome to Ghana, the golden gate to Africa."
                in 4001L..8000L -> if (isTwi) "💬 [$secStr] Sera Cape Coast ne Elmina aban dedaw ahorow yi." else if (isFrench) "💬 [$secStr] Explorez les châteaux historiques de Cape Coast et d'Elmina." else "💬 [$secStr] Explore the historic castles of Cape Coast and Elmina."
                in 8001L..12000L -> if (isTwi) "💬 [$secStr] Hwɛ sɛnea wɔnwene kente fɛfɛɛfɛ wɔ Kumasi." else if (isFrench) "💬 [$secStr] Découvrez la fabrication traditionnelle du tissu Kente à Kumasi." else "💬 [$secStr] Experience the colorful kente weaving traditions in Kumasi."
                in 12001L..16000L -> if (isTwi) "💬 [$secStr] Gye wo nhomee wɔ Ada ne Busua mpoano fɛfɛ no." else if (isFrench) "💬 [$secStr] Détendez-vous sur les plages de sable fin d'Ada et de Busua." else "💬 [$secStr] Relax on the sandy shores of Ada and Busua beaches."
                in 16001L..20000L -> if (isTwi) "💬 [$secStr] Tee Ghana Jollof a ɛyɛ dɛ nnɛm-nnɛm no dɛ." else if (isFrench) "💬 [$secStr] Dégustez la saveur légendaire du riz Jollof ghanéen." else "💬 [$secStr] Taste the legendary flavor of freshly cooked authentic Jollof rice."
                in 20001L..25000L -> if (isTwi) "💬 [$secStr] Fa ahodwowɛ kɔ Accra anadwo amammerɛ nnwom mu." else if (isFrench) "💬 [$secStr] Ressentez l'énergie de la musique Highlife dans les nuits d'Accra." else "💬 [$secStr] And feel the highlife energy pulsing through Accra's night atmosphere."
                else -> if (isTwi) "💬 [$secStr] [Sankofa Sanku Nnwom bi reduru mpɔtam hwa.]" else if (isFrench) "💬 [$secStr] [Musique traditionnelle en cours de lecture...]" else "💬 [$secStr] [Upbeat acoustic guitar playing - Ghana welcomes you home.]"
            }
        } else if (videoTitle.contains("Sankofa Heritage", ignoreCase = true) || videoTitle.contains("Trailer", ignoreCase = true)) {
            when (adjustedPos) {
                in 0L..4000L -> if (isTwi) "💬 [$secStr] Yɛn nananom gyaw yɛn nyansa kwan bi." else if (isFrench) "💬 [$secStr] Nos ancêtres nous ont laissé un héritage de sagesse." else "💬 [$secStr] Our ancestors left us a trail of wisdom."
                in 4001L..8000L -> if (isTwi) "💬 [$secStr] Sankofa yɛ a yɛnkyiri, yɛbɔ mpae a ɛbɛba mu." else if (isFrench) "💬 [$secStr] Retourner chercher ce qui est oublié n'est pas un tabou." else "💬 [$secStr] To go back and fetch what is forgotten is not taboo."
                in 8001L..12000L -> if (isTwi) "💬 [$secStr] Yei ne Sankofa amammerɛ abakɔsɛm kɛse no." else if (isFrench) "💬 [$secStr] C'est la saga du Sankofa. Un voyage à travers le temps." else "💬 [$secStr] This is the saga of Sankofa. A journey across time."
                in 12001L..16000L -> if (isTwi) "💬 [$secStr] Wɔbɔɔ mmɔden sɛ wɔbɛsesa yɛn pɛpɛɛpɛ..." else if (isFrench) "💬 [$secStr] Ils ont essayé de réécrire nos chroniques..." else "💬 [$secStr] They tried to rewrite our chronicles..."
                in 16001L..20000L -> if (isTwi) "💬 [$secStr] Nanso dɔteɛ yi kae kyenene mmerɛ nyinaa." else if (isFrench) "💬 [$secStr] Mais la terre se souvient des rythmes du tambour." else "💬 [$secStr] But the soil remembers the drum beats."
                in 20001L..25000L -> if (isTwi) "💬 [$secStr] Ɛreba sinema ahorow mu wɔ Accra ne Kumasi nnansa yi." else if (isFrench) "💬 [$secStr] Bientôt dans les cinémas d'Accra et Kumasi cet été." else "💬 [$secStr] Coming to cinemas in Accra and Kumasi this summer."
                else -> if (isTwi) "💬 [$secStr] [Atumpan aponkyerɛne twene rekam dwoodwoo...]" else if (isFrench) "💬 [$secStr] [Roulements de tambours traditionnels s'estompant...]" else "💬 [$secStr] [Deep traditional drum rolls fading out slowly...]"
            }
        } else if (videoTitle.contains("Cocoa Farms", ignoreCase = true) || videoTitle.contains("Seeds", ignoreCase = true)) {
            when (adjustedPos) {
                in 0L..4000L -> if (isTwi) "💬 [$secStr] Anɔpa pɛɛ, wɔ Asante mantam kurotia mu baabi..." else if (isFrench) "💬 [$secStr] À l'aube, au cœur de la région d'Ashanti..." else "💬 [$secStr] At the break of dawn, deep in the Ashanti region..."
                in 4001L..8000L -> if (isTwi) "💬 [$secStr] Akuafoɔ de anigyeɛ tetɛ kookoo aba fɛfɛɛfɛ." else if (isFrench) "💬 [$secStr] Les agriculteurs récoltent avec soin les cabosses de cacao." else "💬 [$secStr] Farmers carefully harvest cocoa pods from towering trees."
                in 8001L..12000L -> if (isTwi) "💬 [$secStr] Kookoo aba yi ne sika kɔkɔɔ a ɛyɛ wiase nyinaa dɛ." else if (isFrench) "💬 [$secStr] Ce sont les graines d'or du cacao qui adoucissent le monde entier." else "💬 [$secStr] These are the golden seeds of cocoa that sweeten the entire world."
                in 12001L..16000L -> if (isTwi) "💬 [$secStr] Kookoo adwuma biara hia mmɔdenbɔ ne asoɔmerɛ." else if (isFrench) "💬 [$secStr] Chaque cabosse représente une dure journée de labeur et de passion." else "💬 [$secStr] Every pod represents a hard day's labor and extreme dedication."
                else -> if (isTwi) "💬 [$secStr] GHPlayer kyerɛ: Ashanti Sika Kookoo Abakɔsɛm." else if (isFrench) "💬 [$secStr] GHPlayer présente: Les Graines d'Or d'Ashanti." else "💬 [$secStr] GHPlayer presents: The Golden Seeds of the Ashanti."
            }
        } else if (videoTitle.contains("Accra Street Vibe", ignoreCase = true) || videoTitle.contains("Independence", ignoreCase = true)) {
            when (adjustedPos) {
                in 0L..4000L -> if (isTwi) "💬 [$secStr] Accra kuro ayɛ dɛ! Mpem pii ahyia redi ahofadi afahyɛ." else if (isFrench) "💬 [$secStr] Accra est vivante! Des milliers de personnes célèbrent l'indépendance." else "💬 [$secStr] Accra is alive! Thousands gather to celebrate Independence."
                in 4001L..8000L -> if (isTwi) "💬 [$secStr] Kɔkɔɔ, Sika, Kɔbɛ ne Nsroma Tuntum no repɛ n'anim asase so." else if (isFrench) "💬 [$secStr] Le rouge, l'or, le vert et l'étoile noire de l'Afrique brillent." else "💬 [$secStr] Red, gold, green, and the black star of Africa shine bright."
                in 8001L..12000L -> if (isTwi) "💬 [$secStr] Fi Black Star Square kɔ Jamestown, anigyeɛ nko ara!" else if (isFrench) "💬 [$secStr] De Black Star Square à Jamestown, l'ambiance est incroyable!" else "💬 [$secStr] From Black Star Square to Jamestown, the vibes are matching!"
                else -> if (isTwi) "💬 [$secStr] Ghana gyina pintinn ahofadi mu. Freedom and Justice!" else if (isFrench) "💬 [$secStr] Le Ghana se tient fort et libre. Liberté et Justice!" else "💬 [$secStr] Ghana stands strong and free. Freedom and Justice!"
            }
        } else if (videoTitle.contains("Highway Cruise", ignoreCase = true) || videoTitle.contains("Cape Coast Road", ignoreCase = true)) {
            when (adjustedPos) {
                in 0L..4000L -> if (isTwi) "💬 [$secStr] Yɛretwi lɔre wɔ Atlantic kwan kɛseɛ no so kɔ Cape Coast mpoano." else if (isFrench) "💬 [$secStr] Croisière le long de l'autoroute de l'Atlantique vers Cape Coast." else "💬 [$secStr] Cruising down the warm Atlantic highway stretch towards Cape Coast."
                in 4001L..8000L -> if (isTwi) "💬 [$secStr] Kube nnua rehimhim fɛfɛɛfɛ nsuo no hwa mu." else if (isFrench) "💬 [$secStr] Les cocotiers se balancent au gré de la brise marine." else "💬 [$secStr] Coconut palms sway alongside the breeze of the ocean."
                in 8001L..12000L -> if (isTwi) "💬 [$secStr] Kwan kɛseɛ no reto nsa afrɛ wo. Bra bɛhwɛ Ghana Atɔeɛ." else if (isFrench) "💬 [$secStr] La route vous invite. Il est temps d'explorer l'ouest du Ghana." else "💬 [$secStr] The open road invites you. Time to explore Western Ghana."
                else -> if (isTwi) "💬 [$secStr] [Lɔre ahyɛnse rehuruhure pɛpɛɛpɛ anwummere fɛfɛ mu.]" else if (isFrench) "💬 [$secStr] [Vrombissement du moteur sous le crépuscule.]" else "💬 [$secStr] [Engine revving as vehicle cruises smoothly along sunset curves.]"
            }
        } else {
            // General track sync caption rendering for any other dynamic video file
            when (adjustedPos) {
                in 0L..5000L -> if (isTwi) "💬 [$secStr] Akwaaba! Mmere yi yɛrehwehwɛ sene afahyɛ fɛfɛɛfɛ." else if (isFrench) "💬 [$secStr] Bienvenue! Lecture de votre fichier média en cours." else "💬 [$secStr] Welcome! Streaming your selected media file."
                in 5001L..12000L -> if (isTwi) "💬 [$secStr] GHPlayer dedaw di mmerɛ pɔtee amammerɛ nyinaa." else if (isFrench) "💬 [$secStr] GHPlayer est en cours d'optimisation." else "💬 [$secStr] GHPlayer playback rendering engine is online."
                else -> if (isTwi) "💬 [$secStr] Sene No Mmere Sync: ${subtitleOffsetMs}ms (Language: Akan Twi)" else if (isFrench) "💬 [$secStr] Synchronisation Sous-titres: ${subtitleOffsetMs}ms (Langue: Français)" else "💬 [$secStr] Subtitle Track Sync: ${subtitleOffsetMs}ms (Language: English)"
            }
        }
    }

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
    var isInPipMode by mutableStateOf(false)
    var isBackgroundPlayEnabled by mutableStateOf(true)
    var showDemoMedia by mutableStateOf(false)

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
                            val player = mediaPlayer
                            if (player != null && player.isPlaying) {
                                currentPlaybackPosition = player.currentPosition.toLong()
                            } else {
                                // Dynamic backup progression to ensure smooth slider/time UI moving
                                val currentMax = activeAudioItem?.duration ?: 0L
                                if (currentMax > 0L) {
                                    if (currentPlaybackPosition < currentMax) {
                                         currentPlaybackPosition = (currentPlaybackPosition + (1000L * playbackSpeed).toLong()).coerceAtMost(currentMax)
                                    } else {
                                        if (repeatMode == RepeatMode.ONE) {
                                            activeAudioItem?.let { playAudio(it) } ?: playNextAudio()
                                        } else {
                                            isPlaying = false
                                            currentPlaybackPosition = 0L
                                            playNextAudio()
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Error fetching position from MediaPlayer, using backup", e)
                            val currentMax = activeAudioItem?.duration ?: 0L
                            if (currentMax > 0L) {
                                currentPlaybackPosition = (currentPlaybackPosition + (1000L * playbackSpeed).toLong()).coerceAtMost(currentMax)
                            }
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
            _videos.value = scanner.scanVideos(showDemoMedia)
            _audios.value = scanner.scanAudios(showDemoMedia)
        }
    }

    fun toggleDemoMedia(enabled: Boolean) {
        showDemoMedia = enabled
        refreshLibrary()
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
                    this@MainViewModel.updateNotificationService()
                }
                setOnCompletionListener {
                    this@MainViewModel.isPlaying = false
                    this@MainViewModel.currentPlaybackPosition = 0L
                    this@MainViewModel.updateNotificationService()
                    if (closeAppAfterCurrentSong) {
                        stopPlayback()
                        closeAppAfterCurrentSong = false
                    } else {
                        if (repeatMode == RepeatMode.ONE) {
                            activeAudioItem?.let { playAudio(it) } ?: playNextAudio()
                        } else {
                            playNextAudio()
                        }
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("MainViewModel", "MediaPlayer error: $what, extra: $extra")
                    this@MainViewModel.isPlaying = false
                    this@MainViewModel.updateNotificationService()
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

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
    }

    fun toggleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun getActiveAudioList(): List<LocalMediaItem> {
        val currentPlaylist = activePlaylist
        if (currentPlaylist != null) {
            val songs = _activePlaylistSongs.value
            return songs.map { song ->
                LocalMediaItem(
                    id = song.songPath,
                    title = song.songTitle,
                    artist = song.songArtist,
                    path = song.songPath,
                    duration = song.songDuration,
                    size = song.songSize,
                    folder = song.songFolder,
                    mediaType = "audio",
                    isStream = song.isStream
                )
            }
        } else {
            return _audios.value
        }
    }

    fun playNextAudio() {
        val songs = getActiveAudioList()
        if (songs.isEmpty()) {
            isPlaying = false
            currentPlaybackPosition = 0L
            return
        }

        val currentIdx = songs.indexOfFirst { it.path == activeAudioItem?.path }

        if (isShuffleEnabled) {
            val nextIdx = if (songs.size > 1) {
                var r = (0 until songs.size).random()
                while (r == currentIdx) {
                    r = (0 until songs.size).random()
                }
                r
            } else {
                0
            }
            playAudio(songs[nextIdx])
        } else {
            if (currentIdx != -1 && currentIdx < songs.size - 1) {
                playAudio(songs[currentIdx + 1])
            } else {
                if (repeatMode == RepeatMode.ALL) {
                    playAudio(songs[0])
                } else {
                    isPlaying = false
                    currentPlaybackPosition = 0L
                }
            }
        }
    }

    fun playPreviousAudio() {
        val songs = getActiveAudioList()
        if (songs.isEmpty()) {
            isPlaying = false
            currentPlaybackPosition = 0L
            return
        }

        // If current song is played more than 3 seconds, restart it first
        if (currentPlaybackPosition > 3000L) {
            seekTo(0L)
            return
        }

        val currentIdx = songs.indexOfFirst { it.path == activeAudioItem?.path }

        if (isShuffleEnabled) {
            val prevIdx = if (songs.size > 1) {
                var r = (0 until songs.size).random()
                while (r == currentIdx) {
                    r = (0 until songs.size).random()
                }
                r
            } else {
                0
            }
            playAudio(songs[prevIdx])
        } else {
            if (currentIdx > 0) {
                playAudio(songs[currentIdx - 1])
            } else {
                if (repeatMode == RepeatMode.ALL) {
                    playAudio(songs[songs.size - 1])
                } else {
                    seekTo(0L)
                }
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
        updateNotificationService()
    }

    fun resumePlayback() {
        if (activeAudioItem != null) {
            try {
                mediaPlayer?.start()
                isPlaying = true
                updateNotificationService()
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
        stopNotificationService()
    }

    fun seekTo(pos: Long) {
        if (activeAudioItem != null) {
            val currentMax = activeAudioItem?.duration ?: 0L
            val target = pos.coerceIn(0L, currentMax)
            try {
                mediaPlayer?.seekTo(target.toInt())
                currentPlaybackPosition = target
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error seeking to position $pos", e)
            }
        } else if (activeVideoItem != null) {
            val currentMax = activeVideoItem?.duration ?: 0L
            currentPlaybackPosition = pos.coerceIn(0L, currentMax)
        }
    }

    fun manualSeekTo(pos: Long) {
        lastManualSeekTimeMs = System.currentTimeMillis()
        seekTo(pos)
    }

    fun seekForward() {
        lastManualSeekTimeMs = System.currentTimeMillis()
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
        lastManualSeekTimeMs = System.currentTimeMillis()
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
            isDownloadingSubtitle = true
            subtitleDownloadError = null
            activeSubtitleUrl = "Downloading..."
            for (i in 1..5) {
                delay(300)
            }
            val subName = "${item.title.take(12).replace(" ", "_")}_$subtitleLanguage.srt"
            activeSubtitleUrl = subName
            isDownloadingSubtitle = false
            completion("Successfully loaded local matching subtitle '$subName'!")
        }
    }

    fun downloadSubtitleWithSearchAndLanguage(
        item: LocalMediaItem,
        query: String,
        lang: String,
        forceNotFound: Boolean,
        completion: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            isDownloadingSubtitle = true
            subtitleDownloadError = null
            activeSubtitleUrl = "Downloading..."
            
            // Simulating realistic search latency over servers
            delay(1200)
            
            if (forceNotFound || query.trim().lowercase().contains("error") || query.trim().length < 3) {
                isDownloadingSubtitle = false
                activeSubtitleUrl = null
                subtitleDownloadError = "Offline or subtitle file not found on GHSubs/OpenSubtitles database server for query '$query'. Error 404."
                completion(false, "No subtitles matched on our systems.")
            } else {
                val subName = "${query.trim().take(15).replace(" ", "_").uppercase()}_$lang.srt"
                activeSubtitleUrl = subName
                isDownloadingSubtitle = false
                subtitleDownloadError = null
                completion(true, "Successfully grabbed subtitle track ($subName) online!")
            }
        }
    }

    fun loadLocalMockFile(item: LocalMediaItem) {
        activeSubtitleUrl = "${item.title.take(12).replace(" ", "_")}_LOCAL_MUX.srt"
        subtitleDownloadError = null
    }

    fun clearSubtitle() {
        activeSubtitleUrl = null
        subtitleDownloadError = null
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

    fun clearFavorites() {
        viewModelScope.launch {
            mediaRepository.clearAllFavorites()
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

    fun updateNotificationService() {
        val item = activeAudioItem ?: return
        try {
            val intent = Intent(getApplication(), PlaybackService::class.java).apply {
                putExtra("title", item.title)
                putExtra("artist", item.artist)
                putExtra("isPlaying", isPlaying)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error updating notification service", e)
        }
    }

    fun stopNotificationService() {
        try {
            val intent = Intent(getApplication(), PlaybackService::class.java)
            getApplication<Application>().stopService(intent)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error stopping notification service", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        activePlaylistSongsJob?.cancel()
        stopNotificationService()
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
