package com.example.ui

import android.widget.MediaController
import android.widget.VideoView
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import androidx.compose.ui.layout.ContentScale
import com.example.data.LocalMediaItem
import com.example.data.database.MediaHistory
import com.example.data.database.Playlist
import com.example.data.database.PlaylistSong
import com.example.ui.theme.GHSkin
import com.example.ui.theme.SkinsList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

fun hasAllPermissions(context: Context): Boolean {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun PermissionPromptCard(
    skin: GHSkin,
    message: String,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_prompt_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = skin.surface.copy(alpha = 0.85f),
            contentColor = skin.onSurface
        ),
        border = BorderStroke(1.dp, skin.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Storage,
                contentDescription = null,
                tint = skin.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ACCESS LOCAL MEDIA",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = skin.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = skin.onSurface.copy(alpha = 0.7f),
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = skin.primary,
                    contentColor = skin.background
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GRANT PERMISSION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GHPlayerMain(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val skin = viewModel.currentSkin
    var showSubtitleDialog by remember { mutableStateOf<LocalMediaItem?>(null) }
    var showTrackDialog by remember { mutableStateOf<LocalMediaItem?>(null) }
    var isAudioPlayerExpanded by remember { mutableStateOf(false) }
    var showSplashScreen by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(2000)
        showSplashScreen = false
    }

    var permissionsGranted by remember { mutableStateOf(hasAllPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
        val allGranted = resultMap.values.all { it }
        permissionsGranted = allGranted
        if (allGranted) {
            viewModel.refreshLibrary()
        }
    }

    LaunchedEffect(Unit) {
        permissionsGranted = hasAllPermissions(context)
        if (!permissionsGranted) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(permissions)
        }
    }

    val onRequestPermission = {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    val videosList by viewModel.videos.collectAsState()
    val audiosList by viewModel.audios.collectAsState()
    val historyList by viewModel.playbackHistory.collectAsState()
    val favoritesList by viewModel.favorites.collectAsState()

    BackHandler(enabled = isAudioPlayerExpanded) {
        isAudioPlayerExpanded = false
    }

    if (viewModel.isInPipMode) {
        viewModel.activeVideoItem?.let { activeVideo ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                RealVideoPlayerView(
                    videoItem = activeVideo,
                    isPlaying = viewModel.isPlaying,
                    position = viewModel.currentPlaybackPosition,
                    playbackSpeed = viewModel.playbackSpeed,
                    skin = skin,
                    onPositionChanged = { viewModel.manualSeekTo(it) },
                    lastManualSeekTimeMs = viewModel.lastManualSeekTimeMs,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("No video playing", color = Color.White)
            }
        }
    } else if (showSplashScreen) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0C0D0C)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_splash_1781317118399),
                contentDescription = "Splash Screen Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("app_splash_screen"),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = skin.background,
        bottomBar = {
            GHBottomNavigation(
                currentTab = viewModel.currentTab,
                onTabSelected = { viewModel.navigateTo(it) },
                skin = skin
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background Ghana Glow Gradient Effects (Beautification)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Subtle gradient at the top matching active skin primary/secondary colors
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(skin.primary.copy(alpha = 0.08f), Color.Transparent),
                                center = Offset(size.width * 0.2f, 0f),
                                radius = size.width * 0.8f
                            ),
                            radius = size.width * 0.8f,
                            center = Offset(size.width * 0.2f, 0f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(skin.secondary.copy(alpha = 0.08f), Color.Transparent),
                                center = Offset(size.width * 0.8f, size.height * 0.5f),
                                radius = size.width * 0.8f
                            ),
                            radius = size.width * 0.8f,
                            center = Offset(size.width * 0.8f, size.height * 0.5f)
                        )
                    }
            )

            // Dynamic screen loading
            AnimatedContent(
                targetState = viewModel.currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.Videos -> VideosScreen(
                        viewModel = viewModel,
                        videos = videosList,
                        onShowSubtitles = { showSubtitleDialog = it },
                        onShowTracks = { showTrackDialog = it },
                        permissionsGranted = permissionsGranted,
                        onRequestPermission = onRequestPermission
                    )
                    Screen.Music -> MusicScreen(
                        viewModel = viewModel,
                        audios = audiosList,
                        permissionsGranted = permissionsGranted,
                        onRequestPermission = onRequestPermission
                    )
                    Screen.Equalizer -> EqualizerScreen(
                        viewModel = viewModel
                    )
                    Screen.Skins -> SkinsScreen(
                        viewModel = viewModel
                    )
                    Screen.History -> HistoryScreen(
                        viewModel = viewModel,
                        historyList = historyList,
                        favoritesList = favoritesList
                    )
                }
            }

            // Real Conversion Progress Dialog Overlay (MP3 Converter)
            if (viewModel.isConverting) {
                GHPConversionDialog(
                    videoItem = viewModel.conversionActiveVideo,
                    progress = viewModel.conversionProgress,
                    skin = skin
                )
            }

            // Subtitle Download Toast / Dialog Binds
            showSubtitleDialog?.let { item ->
                GHPDownloadSubtitleDialog(
                    item = item,
                    viewModel = viewModel,
                    skin = skin,
                    onDismiss = { showSubtitleDialog = null }
                )
            }

            // Audio track chooser Dialog Binds
            showTrackDialog?.let { item ->
                GHPSelectTrackDialog(
                    item = item,
                    activeTrack = viewModel.activeAudioTrack,
                    skin = skin,
                    onDismiss = { showTrackDialog = null },
                    onSelect = {
                        viewModel.selectAudioTrack(it)
                        showTrackDialog = null
                    }
                )
            }

            // Sleep Timer Bottom Sheet Dialog
            if (viewModel.showTimerBottomSheet) {
                GHPTimerBottomSheet(
                    viewModel = viewModel,
                    skin = skin,
                    onDismiss = { viewModel.showTimerBottomSheet = false }
                )
            }

            // Render bottom miniature audio player if audio is active
            viewModel.activeAudioItem?.let { activeAudio ->
                if (!isAudioPlayerExpanded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val isFav = favoritesList.any { it.path == activeAudio.path }
                        MiniAudioPlayerCard(
                            audioItem = activeAudio,
                            isPlaying = viewModel.isPlaying,
                            position = viewModel.currentPlaybackPosition,
                            isFavorite = isFav,
                            onToggleFavorite = { viewModel.toggleFavorite(activeAudio.path) },
                            onPlayPause = {
                                if (viewModel.isPlaying) viewModel.pauseAudio() else viewModel.resumePlayback()
                            },
                            onClose = { viewModel.stopPlayback() },
                            skin = skin,
                            onSeekForward = { viewModel.seekForward() },
                            onSeekBackward = { viewModel.seekBackward() },
                            onSeekTo = { viewModel.seekTo(it) },
                            onExpand = { isAudioPlayerExpanded = true }
                        )
                    }
                } else {
                    val isFav = favoritesList.any { it.path == activeAudio.path }
                    FullscreenAudioPlayer(
                        audioItem = activeAudio,
                        isPlaying = viewModel.isPlaying,
                        position = viewModel.currentPlaybackPosition,
                        isFavorite = isFav,
                        onToggleFavorite = { viewModel.toggleFavorite(activeAudio.path) },
                        onPlayPause = {
                            if (viewModel.isPlaying) viewModel.pauseAudio() else viewModel.resumePlayback()
                        },
                        onClosePlayer = { isAudioPlayerExpanded = false },
                        skin = skin,
                        onSeekForward = { viewModel.seekForward() },
                        onSeekBackward = { viewModel.seekBackward() },
                        onSeekTo = { viewModel.seekTo(it) },
                        viewModel = viewModel
                    )
                }
            }

            // Render detailed fullscreen overlay playing view if active Video
            viewModel.activeVideoItem?.let { activeVideo ->
                val isFav = favoritesList.any { it.path == activeVideo.path }
                FullscreenVideoPlayer(
                    videoItem = activeVideo,
                    isPlaying = viewModel.isPlaying,
                    position = viewModel.currentPlaybackPosition,
                    speed = viewModel.playbackSpeed,
                    autoPause = viewModel.autoPauseAtEnd,
                    isFloating = viewModel.isFloatingActive,
                    isBackgroundPlay = viewModel.isBackgroundPlayEnabled,
                    activeTrack = viewModel.activeAudioTrack,
                    subtitleUrl = viewModel.activeSubtitleUrl,
                    subtitleText = viewModel.getDynamicSubtitleText(viewModel.currentPlaybackPosition),
                    subtitleTextSize = viewModel.subtitleTextSize,
                    subtitleColor = viewModel.subtitleColor,
                    skin = skin,
                    isFavorite = isFav,
                    onToggleFavorite = { viewModel.toggleFavorite(activeVideo.path) },
                    onChangePlayPause = {
                        if (viewModel.isPlaying) viewModel.pauseVideoPlayback() else viewModel.resumePlayback()
                    },
                    onSeekForward = { viewModel.seekForward() },
                    onSeekBackward = { viewModel.seekBackward() },
                    onChangeSpeed = { viewModel.changePlaybackSpeed(it) },
                    onChangeAutoPause = { viewModel.autoPauseAtEnd = it },
                    onChangeFloating = { viewModel.isFloatingActive = it },
                    onChangeBackgroundPlay = { viewModel.isBackgroundPlayEnabled = it },
                    onShowTracks = { showTrackDialog = activeVideo },
                    onShowSubtitles = { showSubtitleDialog = activeVideo },
                    onClosePlayer = { viewModel.stopPlayback() },
                    onSeekTo = { viewModel.manualSeekTo(it) },
                    lastManualSeekTimeMs = viewModel.lastManualSeekTimeMs
                )
            }
        }
    }
}
}

// ------------------------------------------------------------------
// Bottom Nav Implementation
// ------------------------------------------------------------------
@Composable
fun GHBottomNavigation(
    currentTab: Screen,
    onTabSelected: (Screen) -> Unit,
    skin: GHSkin
) {
    NavigationBar(
        containerColor = skin.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("gh_bottom_nav")
    ) {
        NavigationBarItem(
            selected = currentTab == Screen.Videos,
            onClick = { onTabSelected(Screen.Videos) },
            icon = { Icon(Icons.Filled.VideoLibrary, contentDescription = "Videos") },
            label = { Text("Videos", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = skin.primary,
                selectedTextColor = skin.primary,
                indicatorColor = skin.primary.copy(alpha = 0.15f),
                unselectedIconColor = skin.onSurface.copy(alpha = 0.6f),
                unselectedTextColor = skin.onSurface.copy(alpha = 0.6f)
            )
        )
        NavigationBarItem(
            selected = currentTab == Screen.Music,
            onClick = { onTabSelected(Screen.Music) },
            icon = { Icon(Icons.Filled.MusicNote, contentDescription = "Music") },
            label = { Text("Music", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = skin.primary,
                selectedTextColor = skin.primary,
                indicatorColor = skin.primary.copy(alpha = 0.15f),
                unselectedIconColor = skin.onSurface.copy(alpha = 0.6f),
                unselectedTextColor = skin.onSurface.copy(alpha = 0.6f)
            )
        )
        NavigationBarItem(
            selected = currentTab == Screen.Equalizer,
            onClick = { onTabSelected(Screen.Equalizer) },
            icon = { Icon(Icons.Filled.Tune, contentDescription = "Equalizer") },
            label = { Text("Equalizer", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = skin.primary,
                selectedTextColor = skin.primary,
                indicatorColor = skin.primary.copy(alpha = 0.15f),
                unselectedIconColor = skin.onSurface.copy(alpha = 0.6f),
                unselectedTextColor = skin.onSurface.copy(alpha = 0.6f)
            )
        )
        NavigationBarItem(
            selected = currentTab == Screen.Skins,
            onClick = { onTabSelected(Screen.Skins) },
            icon = { Icon(Icons.Filled.Palette, contentDescription = "Skins") },
            label = { Text("Skins", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = skin.primary,
                selectedTextColor = skin.primary,
                indicatorColor = skin.primary.copy(alpha = 0.15f),
                unselectedIconColor = skin.onSurface.copy(alpha = 0.6f),
                unselectedTextColor = skin.onSurface.copy(alpha = 0.6f)
            )
        )
        NavigationBarItem(
            selected = currentTab == Screen.History,
            onClick = { onTabSelected(Screen.History) },
            icon = { Icon(Icons.Filled.History, contentDescription = "History") },
            label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = skin.primary,
                selectedTextColor = skin.primary,
                indicatorColor = skin.primary.copy(alpha = 0.15f),
                unselectedIconColor = skin.onSurface.copy(alpha = 0.6f),
                unselectedTextColor = skin.onSurface.copy(alpha = 0.6f)
            )
        )
    }
}

// ------------------------------------------------------------------
// Screen 1: Videos Layout Page
// ------------------------------------------------------------------
@Composable
fun VideosScreen(
    viewModel: MainViewModel,
    videos: List<LocalMediaItem>,
    onShowSubtitles: (LocalMediaItem) -> Unit,
    onShowTracks: (LocalMediaItem) -> Unit,
    permissionsGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    val skin = viewModel.currentSkin
    val favoritesList by viewModel.favorites.collectAsState()

    // Create folder options from videos list
    val folders = remember(videos) {
        listOf("All") + videos.map { it.folder }.distinct()
    }

    // Filtered videos
    val filteredVideos = remember(videos, viewModel.selectedVideoFolder, viewModel.videoSearchQuery) {
        videos.filter { video ->
            val matchFolder = viewModel.selectedVideoFolder == "All" || video.folder == viewModel.selectedVideoFolder
            val matchQuery = viewModel.videoSearchQuery.isEmpty() || video.title.contains(viewModel.videoSearchQuery, ignoreCase = true)
            matchFolder && matchQuery
        }
    }

    var currentView by remember { mutableStateOf("All") } // "All", "List", "Folder", "Video", "Network"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Header with Flag Colors Strip & rescan tool
        HeaderGhanaBranding(
            title = "GH VIDEO PLAYER",
            skin = skin,
            onRefreshClick = { viewModel.refreshLibrary() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Bar (All, List, Folder, Video, Network)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val navItems = listOf("All", "List", "Folder", "Video", "Network")
            navItems.forEach { item ->
                val isSelected = currentView == item
                FilterChip(
                    selected = isSelected,
                    onClick = { currentView = item },
                    label = { 
                        Text(
                            item.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) skin.background else skin.onSurface
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = skin.primary,
                        containerColor = skin.surface
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        // Conditional content rendering based on currentView... (rest of code)

        if (!permissionsGranted) {
            PermissionPromptCard(
                skin = skin,
                message = "The app needs storage permissions to scan and show your local videos from your device storage.",
                onRequestPermission = onRequestPermission
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Large Search Field
        OutlinedTextField(
            value = viewModel.videoSearchQuery,
            onValueChange = { viewModel.searchVideos(it) },
            placeholder = { Text("Search local video file...", color = skin.onSurface.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("video_search_input"),
            shape = RoundedCornerShape(24.dp),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = skin.primary) },
            trailingIcon = {
                if (viewModel.videoSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchVideos("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = skin.primary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = skin.surface,
                focusedBorderColor = skin.primary,
                unfocusedLabelColor = skin.onSurface.copy(alpha = 0.5f),
                focusedLabelColor = skin.primary,
                unfocusedContainerColor = skin.surface.copy(alpha = 0.5f),
                focusedContainerColor = skin.surface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Download Sources / Folders Categorization Horizontal layout
        Text("DOWNLOAD SOURCES & FOLDERS", fontSize = 12.sp, color = skin.primary, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(folders) { folder ->
                val isSelected = viewModel.selectedVideoFolder == folder
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) skin.primary else skin.surface)
                        .clickable { viewModel.setVideoFolder(folder) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (folder == "All") Icons.Filled.FolderOpen else Icons.Filled.Folder,
                            contentDescription = null,
                            tint = if (isSelected) skin.background else skin.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = folder.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) skin.background else skin.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Videos list
        if (filteredVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = skin.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No videos match your search/folder selection",
                        color = skin.onSurface.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredVideos) { video ->
                    val isFav = favoritesList.any { it.path == video.path }
                    VideoItemCard(
                        video = video,
                        skin = skin,
                        isFavorite = isFav,
                        onPlayClick = { viewModel.playVideo(video) },
                        onConvertToMp3Click = { viewModel.convertVideoToMp3(video) },
                        onShowSubtitles = { onShowSubtitles(video) },
                        onShowTracks = { onShowTracks(video) },
                        onToggleFavorite = { viewModel.toggleFavorite(video.path) }
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Visual Theme Header Strip (Ghana National Colors Banner)
// ------------------------------------------------------------------
@Composable
fun HeaderGhanaBranding(
    title: String,
    skin: GHSkin,
    onRefreshClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = skin.onBackground
                )
                if (onRefreshClick != null) {
                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.size(32.dp).testTag("header_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh library",
                            tint = skin.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Dynamic mini flag representation
            Row(modifier = Modifier.height(12.dp)) {
                Box(modifier = Modifier.width(16.dp).fillMaxHeight().background(skin.accentRed))
                Box(modifier = Modifier.width(16.dp).fillMaxHeight().background(skin.accentGold))
                Box(modifier = Modifier.width(16.dp).fillMaxHeight().background(skin.accentGreen))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Small thin divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(skin.primary, skin.secondary, Color.Transparent)
                    )
                )
        )
    }
}

// ------------------------------------------------------------------
// Video Item Card Component
// ------------------------------------------------------------------
@Composable
fun VideoItemCard(
    video: LocalMediaItem,
    skin: GHSkin,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onConvertToMp3Click: () -> Unit,
    onShowSubtitles: () -> Unit,
    onShowTracks: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_item_card_${video.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = skin.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video thumbnail placeholder with a vibrant flag colored background
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(skin.accentRed.copy(alpha = 0.8f), skin.accentGold.copy(alpha = 0.8f))
                            )
                        )
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Play icon",
                        tint = skin.background,
                        modifier = Modifier.size(36.dp)
                    )
                    
                    // Duration badge in bottom corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(video.duration),
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Metadata Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(skin.secondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.folder.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = skin.secondary
                            )
                        }
                        if (video.isStream) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(skin.accentRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "STREAM",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = skin.accentRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = video.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = skin.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Size: ${formatSize(video.size)} • Playback Speed Control",
                        fontSize = 11.sp,
                        color = skin.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Quick Actions segment
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(skin.onSurface.copy(alpha = 0.1f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(skin.surface.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Convert to MP3 Button
                TextButton(
                    onClick = onConvertToMp3Click,
                    colors = ButtonDefaults.textButtonColors(contentColor = skin.primary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("EXTRACT MP3", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Subtitle Button
                    IconButton(onClick = onShowSubtitles) {
                        Icon(
                            Icons.Filled.Subtitles,
                            contentDescription = "Subtitle Download",
                            tint = skin.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    // Audio Tracks Button
                    IconButton(onClick = onShowTracks) {
                        Icon(
                            Icons.Filled.AudioFile,
                            contentDescription = "Audio Tracks",
                            tint = skin.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    // Favorite button
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) skin.accentRed else skin.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Screen 2: Music Screen Binds
// ------------------------------------------------------------------
@Composable
fun MusicScreen(
    viewModel: MainViewModel,
    audios: List<LocalMediaItem>,
    permissionsGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    val skin = viewModel.currentSkin
    val playlists by viewModel.playlists.collectAsState()
    val activePlaylistSongs by viewModel.activePlaylistSongs.collectAsState()
    val favoritesList by viewModel.favorites.collectAsState()

    var activeMusicSubTab by remember { mutableStateOf("all_tracks") } // "all_tracks" or "playlists"
    var selectedPlaylistView by remember { mutableStateOf<Playlist?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songToAddToPlaylist by remember { mutableStateOf<LocalMediaItem?>(null) }
    // Music view
    var currentView by remember { mutableStateOf("All") } // "All", "List", "Folder", "Video", "Network"

    // Dialog for Creating Playlist
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCreatePlaylistDialog = false
                newPlaylistName = ""
            },
            containerColor = skin.surface,
            titleContentColor = skin.primary,
            textContentColor = skin.onSurface,
            title = { Text("Create New Playlist", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Enter playlist name...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = skin.onSurface.copy(alpha = 0.2f),
                            focusedBorderColor = skin.primary,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = skin.primary, contentColor = skin.background)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showCreatePlaylistDialog = false
                        newPlaylistName = ""
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = skin.primary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Header
        HeaderGhanaBranding(
            title = "GH MUSIC PLAYER",
            skin = skin,
            onRefreshClick = { viewModel.refreshLibrary() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Bar (All, List, Folder, Video, Network)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val navItems = listOf("All", "List", "Folder", "Video", "Network")
            navItems.forEach { item ->
                val isSelected = currentView == item
                FilterChip(
                    selected = isSelected,
                    onClick = { currentView = item },
                    label = { 
                        Text(
                            item.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) skin.background else skin.onSurface
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = skin.primary,
                        containerColor = skin.surface
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        if (!permissionsGranted) {
            PermissionPromptCard(
                skin = skin,
                message = "The app needs storage permissions to scan and show your local songs and audio clips from your device storage.",
                onRequestPermission = onRequestPermission
            )
        } else {
            when (currentView) {
                "All" -> {
                    // Logic for "All" view
                }
                "Folder" -> {
                    Text("Folder browsing coming soon!", color = skin.onSurface)
                }
                else -> {
                    Text("View $currentView not yet implemented.", color = skin.onSurface)
                }
            }
        }
    
    // (Removed broken AlertDialog for now to fix build)
}

    // Folders derived
    val folders = remember(audios) {
        listOf("All") + audios.map { it.folder }.distinct()
    }

    // Filtered and Sorted
    val filteredAudios = remember(audios, viewModel.selectedAudioFolder, viewModel.audioSortOption, viewModel.audioSearchQuery) {
        var result = audios.filter { audio ->
            val matchFolder = viewModel.selectedAudioFolder == "All" || audio.folder == viewModel.selectedAudioFolder
            val matchQuery = viewModel.audioSearchQuery.isEmpty() || audio.title.contains(viewModel.audioSearchQuery, ignoreCase = true)
            matchFolder && matchQuery
        }
        
        result = when (viewModel.audioSortOption) {
            SortOption.TITLE -> result.sortedBy { it.title }
            SortOption.DURATION -> result.sortedByDescending { it.duration }
            SortOption.SIZE -> result.sortedByDescending { it.size }
            SortOption.DATE -> result.sortedByDescending { it.dateAdded }
        }
        result
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        HeaderGhanaBranding(
            title = "GH MUSIC PLAYER",
            skin = skin,
            onRefreshClick = { viewModel.refreshLibrary() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!permissionsGranted) {
            PermissionPromptCard(
                skin = skin,
                message = "The app needs storage permissions to scan and show your local songs and audio clips from your device storage.",
                onRequestPermission = onRequestPermission
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Premium Capsule music subtab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(skin.surface.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isAllMusicTab = (activeMusicSubTab == "all_tracks")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isAllMusicTab) skin.primary else Color.Transparent)
                    .clickable { 
                        activeMusicSubTab = "all_tracks"
                        selectedPlaylistView = null
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ALL MUSIC & CLIPS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAllMusicTab) skin.background else skin.onSurface.copy(alpha = 0.7f)
                )
            }

            val isPlaylistsTab = (activeMusicSubTab == "playlists")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isPlaylistsTab) skin.primary else Color.Transparent)
                    .clickable { 
                        activeMusicSubTab = "playlists"
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MY PLAYLISTS (${playlists.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaylistsTab) skin.background else skin.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeMusicSubTab == "all_tracks") {
            // Search Audio File
            OutlinedTextField(
                value = viewModel.audioSearchQuery,
                onValueChange = { viewModel.searchAudios(it) },
                placeholder = { Text("Search local audios, music & SD card...", color = skin.onSurface.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_search_input"),
                shape = RoundedCornerShape(24.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = skin.primary) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = skin.surface,
                    focusedBorderColor = skin.primary,
                    unfocusedContainerColor = skin.surface.copy(alpha = 0.5f),
                    focusedContainerColor = skin.surface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Audio folders horizontal slider
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(folders) { folder ->
                    val isSelected = viewModel.selectedAudioFolder == folder
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) skin.primary else skin.surface)
                            .clickable { viewModel.setAudioFolder(folder) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = folder.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) skin.background else skin.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sorting & Timer controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleep Timer section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(skin.surface)
                        .clickable {
                            viewModel.showTimerBottomSheet = true
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Filled.Snooze,
                        contentDescription = "Sleep timer",
                        tint = if (viewModel.sleepTimerMinutes > 0) skin.primary else skin.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (viewModel.sleepTimerMinutes > 0) "TIMER: ${viewModel.timerRemainingSeconds / 60}m" else "SLEEP TIMER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.sleepTimerMinutes > 0) skin.primary else skin.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Filtering/Sorting Options (Title, Duration, Size, Date)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf(SortOption.TITLE, SortOption.DURATION, SortOption.SIZE, SortOption.DATE).forEach { option ->
                        val isSelected = viewModel.audioSortOption == option
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) skin.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                .background(skin.surface.copy(alpha = 0.5f))
                                .clickable { viewModel.setAudioSort(option) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = option.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) skin.primary else skin.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audios Grid/List
            if (filteredAudios.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No audio records / downloads found",
                        color = skin.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredAudios) { audio ->
                        val isActive = viewModel.activeAudioItem?.id == audio.id
                        val isFav = favoritesList.any { it.path == audio.path }
                        AudioFileRow(
                            audio = audio,
                            isActive = isActive,
                            skin = skin,
                            isFavorite = isFav,
                            onClick = { viewModel.playAudio(audio) },
                            onToggleFavorite = { viewModel.toggleFavorite(audio.path) },
                            onAddToPlaylist = { songToAddToPlaylist = audio }
                        )
                    }
                }
            }
        } else {
            // Playlists view mode
            val currentPlaylist = selectedPlaylistView
            if (currentPlaylist == null) {
                // List of Playlists
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(skin.primary.copy(alpha = 0.15f))
                            .clickable { showCreatePlaylistDialog = true }
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = skin.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CREATE NEW PLAYLIST", fontWeight = FontWeight.Bold, color = skin.primary, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (playlists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create playlists to group Ghana hits, clips & tracks!",
                                color = skin.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(playlists) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(skin.surface.copy(alpha = 0.35f))
                                        .clickable {
                                            selectedPlaylistView = playlist
                                            viewModel.selectPlaylist(playlist)
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(skin.primary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.QueueMusic,
                                                contentDescription = null,
                                                tint = skin.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = playlist.name,
                                                fontWeight = FontWeight.Bold,
                                                color = skin.onSurface,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Playlist • Click to view tracks",
                                                fontSize = 11.sp,
                                                color = skin.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.deletePlaylist(playlist.id) }
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete Playlist",
                                                tint = skin.accentRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Icon(
                                            Icons.Filled.ChevronRight,
                                            contentDescription = null,
                                            tint = skin.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Inside a playlist view
                val playlist = currentPlaylist
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                selectedPlaylistView = null
                                viewModel.selectPlaylist(null)
                            }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back to list", tint = skin.primary)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = skin.onSurface
                                )
                                Text(
                                    text = "${activePlaylistSongs.size} tracks total",
                                    fontSize = 11.sp,
                                    color = skin.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.deletePlaylist(playlist.id); selectedPlaylistView = null },
                            colors = ButtonDefaults.buttonColors(containerColor = skin.accentRed.copy(alpha = 0.15f), contentColor = skin.accentRed),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DELETE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (activePlaylistSongs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.PlaylistPlay,
                                    contentDescription = null,
                                    tint = skin.onSurface.copy(alpha = 0.2f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "This playlist is empty.",
                                    fontWeight = FontWeight.SemiBold,
                                    color = skin.onSurface.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Go to \"All Music & Clips\" and click the \"+\" icon on any song to add it here!",
                                    color = skin.onSurface.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(activePlaylistSongs) { song ->
                                val mediaItem = LocalMediaItem(
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
                                val isActive = viewModel.activeAudioItem?.path == song.songPath
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isActive) skin.surface else skin.surface.copy(alpha = 0.40f))
                                        .border(
                                            1.dp,
                                            if (isActive) skin.primary.copy(alpha = 0.4f) else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.playAudio(mediaItem) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(skin.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isActive) Icons.Filled.VolumeUp else Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                tint = skin.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = song.songTitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isActive) skin.primary else skin.onBackground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${song.songArtist} • ${formatDuration(song.songDuration)}",
                                                fontSize = 11.sp,
                                                color = skin.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeSongFromPlaylist(playlist.id, song.songPath) }
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Remove from Playlist",
                                            tint = skin.accentRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Audio Row Layout
// ------------------------------------------------------------------
@Composable
fun AudioFileRow(
    audio: LocalMediaItem,
    isActive: Boolean,
    skin: GHSkin,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) skin.surface else skin.surface.copy(alpha = 0.40f))
            .border(
                1.dp,
                if (isActive) skin.primary.copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Disk graphic
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(skin.accentRed, skin.background)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Filled.VolumeUp else Icons.Filled.MusicNote,
                contentDescription = null,
                tint = if (isActive) skin.primary else skin.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Titles
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audio.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isActive) skin.primary else skin.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${audio.artist} • ${formatDuration(audio.duration)}",
                    fontSize = 11.sp,
                    color = skin.onSurface.copy(alpha = 0.6f)
                )
                if (audio.folder == "Converted MP3s") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(skin.accentGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("MP3 CONVERT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = skin.accentGreen)
                    }
                }
            }
        }

        // Actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    Icons.Filled.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = skin.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) skin.accentRed else skin.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = skin.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ------------------------------------------------------------------
// Screen 3: Audio Equalizer Screen Binds
// ------------------------------------------------------------------
@Composable
fun EqualizerScreen(viewModel: MainViewModel) {
    val skin = viewModel.currentSkin
    var eqEnabled by remember { mutableStateOf(true) }

    val presets = listOf(
        "Ghana Beats (Bass Boost)",
        "Accra Club (Jazz)",
        "Highlife Pop",
        "Stadium Rock",
        "Vocal Acoustic",
        "Flat Standard"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        HeaderGhanaBranding(title = "GH HD EQUALIZER", skin = skin)

        Spacer(modifier = Modifier.height(16.dp))

        // Status Enable Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = skin.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "9-Band Pro Masterizer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = skin.onBackground
                    )
                    Text(
                        if (eqEnabled) "Equalizer engine is Active" else "Bypassed standard layout",
                        fontSize = 11.sp,
                        color = skin.onSurface.copy(alpha = 0.6f)
                    )
                }

                Switch(
                    checked = eqEnabled,
                    onCheckedChange = { eqEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = skin.background,
                        checkedTrackColor = skin.primary,
                        uncheckedThumbColor = skin.onSurface.copy(alpha = 0.6f),
                        uncheckedTrackColor = skin.surface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset drop down row
        Text("SELECT SOUND PROFILE", fontSize = 11.sp, color = skin.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets) { preset ->
                val isSelected = viewModel.eqPreset == preset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) skin.primary else skin.surface)
                        .clickable(enabled = eqEnabled) { viewModel.applyPreset(preset) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        preset.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) skin.background else skin.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Custom EQ Spectrogram representation (Simulated visually)
        Text("HD AUDIO FREQUENCIES", fontSize = 11.sp, color = skin.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(skin.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            val bandFrequencies = listOf("60Hz", "230Hz", "910Hz", "4kHz", "14kHz")
            val currentBands = viewModel.eqBands.value

            currentBands.forEachIndexed { idx, value ->
                // Render custom interactive slider row
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    // slider itself
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(skin.background)
                            .pointerInput(eqEnabled) {
                                if (!eqEnabled) return@pointerInput
                                detectTapGestures { offset ->
                                    // Calculate relative progress from offset
                                    val percent = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                                    viewModel.updateBand(idx, (percent * 100).toInt())
                                }
                            }
                    ) {
                        // Color filler matching active slider percentage.
                        // Utilizes Ghana Colors beautifully (Red top, yellow gold middle, green bottom)
                        val sliderColor = when {
                            value > 70 -> skin.accentRed
                            value > 45 -> skin.accentGold
                            else -> skin.accentGreen
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(value / 100f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (eqEnabled) sliderColor else skin.onSurface.copy(alpha = 0.2f))
                                .align(Alignment.BottomCenter)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = bandFrequencies[idx],
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = skin.onBackground
                    )
                    Text(
                        text = "${value - 50}dB",
                        fontSize = 9.sp,
                        color = skin.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pro Tip
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = skin.surface.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = skin.primary)
                Text(
                    "GH Player custom audio equalizer coordinates with hardware audio sessions to amplify Ghana Afrobeats, Highlife drums, and cinema speech clarity.",
                    fontSize = 11.sp,
                    color = skin.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// Screen 4: Skins Customizer Screen
// ------------------------------------------------------------------
@Composable
fun SkinsScreen(viewModel: MainViewModel) {
    val skin = viewModel.currentSkin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        HeaderGhanaBranding(title = "GH INTERFACE SKINS", skin = skin)

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "TAP TO INSTANTLY CHANGE APPS DESIGN SKIN & EMBED GHANA NATIONAL COLORS",
            fontSize = 10.sp,
            color = skin.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("demo_media_toggle_card"),
                    colors = CardDefaults.cardColors(containerColor = skin.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleDemoMedia(!viewModel.showDemoMedia) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "DEMO / STREAMING MEDIA",
                                fontWeight = FontWeight.Bold,
                                color = skin.onSurface,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Enable to load preloaded Ghana-themed streaming videos and audios for testing without local device files.",
                                fontSize = 11.sp,
                                color = skin.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = viewModel.showDemoMedia,
                            onCheckedChange = { viewModel.toggleDemoMedia(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = skin.primary,
                                checkedTrackColor = skin.primary.copy(alpha = 0.3f),
                                uncheckedThumbColor = skin.onSurface.copy(alpha = 0.4f),
                                uncheckedTrackColor = skin.onSurface.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }

            items(SkinsList) { targetSkin ->
                val isSelected = targetSkin.name == skin.name

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            2.dp,
                            if (isSelected) skin.primary else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.setSkin(targetSkin) },
                    colors = CardDefaults.cardColors(containerColor = targetSkin.background)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = targetSkin.name.uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = targetSkin.onBackground
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .background(targetSkin.primary, CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "ACTIVE SKIN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = targetSkin.background
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Render horizontal strip colors payload
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                targetSkin.primary,
                                targetSkin.secondary,
                                targetSkin.accentRed,
                                targetSkin.accentGold,
                                targetSkin.accentGreen,
                                targetSkin.surface
                            ).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Skin layout simulator preview drawing
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(targetSkin.surface, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(targetSkin.primary))
                                Text("Highlife Track.mp3", fontSize = 10.sp, color = targetSkin.onSurface, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = targetSkin.primary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Screen 5: History & Favorites Screen
// ------------------------------------------------------------------
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    historyList: List<MediaHistory>,
    favoritesList: List<MediaHistory>
) {
    val skin = viewModel.currentSkin
    var selectedFilterTab by remember { mutableStateOf("History") } // "History" or "Favorites"
    var searchHistoryQuery by remember { mutableStateOf("") }
    var selectedMediaTypeFilter by remember { mutableStateOf("All") } // "All", "Videos", "Audios"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        HeaderGhanaBranding(title = "GH MEDIA LIBRARY", skin = skin)

        Spacer(modifier = Modifier.height(14.dp))

        // History / Favorites chooser switch tabs
        Row(
            modifier = Modifier
                .fillOuterBorderWidth(1.dp, skin.surface)
                .fillMaxWidth()
                .background(skin.surface.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            listOf("History", "Favorites").forEach { tab ->
                val isSelected = selectedFilterTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) skin.primary else Color.Transparent)
                        .clickable { selectedFilterTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = if (isSelected) skin.background else skin.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search bar
        OutlinedTextField(
            value = searchHistoryQuery,
            onValueChange = { searchHistoryQuery = it },
            placeholder = { Text("Search title, keywords...", color = skin.onSurface.copy(alpha = 0.5f), fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = skin.primary) },
            trailingIcon = {
                if (searchHistoryQuery.isNotEmpty()) {
                    IconButton(onClick = { searchHistoryQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = skin.primary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = skin.surface,
                focusedBorderColor = skin.primary,
                unfocusedContainerColor = skin.surface.copy(alpha = 0.3f),
                focusedContainerColor = skin.surface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Category Filter Chips Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("All", "Videos", "Audios").forEach { type ->
                val isSelected = selectedMediaTypeFilter == type
                AssistChip(
                    onClick = { selectedMediaTypeFilter = type },
                    label = { Text(type.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) skin.primary else skin.surface.copy(alpha = 0.5f),
                        labelColor = if (isSelected) skin.background else skin.onSurface
                    ),
                    border = null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedFilterTab == "History") "RECENTLY PLAYED ITEMS" else "FAVORITES PLAYLIST",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = skin.primary,
                letterSpacing = 1.sp
            )

            if (selectedFilterTab == "History" && historyList.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = skin.accentRed)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("CLEAR HISTORY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (selectedFilterTab == "Favorites" && favoritesList.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearFavorites() },
                    colors = ButtonDefaults.textButtonColors(contentColor = skin.accentRed)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("CLEAR FAVORITES", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val baseList = if (selectedFilterTab == "History") historyList else favoritesList
        val displayedList = baseList.filter { record ->
            val matchQuery = searchHistoryQuery.isEmpty() || record.title.contains(searchHistoryQuery, ignoreCase = true)
            val matchType = when (selectedMediaTypeFilter) {
                "Videos" -> record.mediaType == "video"
                "Audios" -> record.mediaType == "audio"
                else -> true
            }
            matchQuery && matchType
        }

        if (displayedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (selectedFilterTab == "History") Icons.Outlined.History else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = skin.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No matching items found",
                        color = skin.onSurface.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayedList) { record ->
                    HistoryItemCard(
                        record = record,
                        skin = skin,
                        onPlayClick = {
                            // Recreate a LocalMediaItem and launch
                            val mediaItem = LocalMediaItem(
                                id = "hist_${record.id}",
                                title = record.title,
                                path = record.path,
                                duration = record.duration,
                                size = 0L,
                                folder = record.folder,
                                mediaType = record.mediaType,
                                isStream = record.path.startsWith("http")
                            )
                            if (record.mediaType == "video") {
                                viewModel.playVideo(mediaItem)
                            } else {
                                viewModel.playAudio(mediaItem)
                            }
                        },
                        onDeleteClick = { viewModel.removeFromHistory(record.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(record.path) }
                    )
                }
            }
        }
    }
}

// Border wrapper extensions
@Composable
fun Modifier.fillOuterBorderWidth(width: Dp = 1.dp, color: Color) = this.border(width, color, RoundedCornerShape(24.dp))

// ------------------------------------------------------------------
// History Item Card Component Binds
// ------------------------------------------------------------------
@Composable
fun HistoryItemCard(
    record: MediaHistory,
    skin: GHSkin,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = skin.surface.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (record.mediaType == "video") Icons.Filled.VideoLibrary else Icons.Filled.AudioFile,
                contentDescription = null,
                tint = skin.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = skin.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(skin.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(record.mediaType.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = skin.primary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Source: ${record.folder} • ${formatDuration(record.duration)}",
                        fontSize = 11.sp,
                        color = skin.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (record.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (record.isFavorite) skin.accentRed else skin.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onPlayClick) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = skin.primary)
            }

            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = skin.accentRed)
            }
        }
    }
}

// ------------------------------------------------------------------
// Miniature Audio Floating/Bottom Card Player
// ------------------------------------------------------------------
@Composable
fun MiniAudioPlayerCard(
    audioItem: LocalMediaItem,
    isPlaying: Boolean,
    position: Long,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    skin: GHSkin,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mini_audio_player")
            .clickable { onExpand() }
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(skin.primary, skin.primary.copy(alpha = 0.4f))
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = skin.surface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Disk Animation / Music Note Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(skin.accentGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "Music Note",
                        tint = skin.background,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Artist Column - explicitly limited to single lines to prevent any wrapping columns!
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = audioItem.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = skin.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = audioItem.artist.ifEmpty { "Unknown Artist" },
                        fontSize = 11.sp,
                        color = skin.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Controls Row - optimized horizontal footprint to prevent text column squishing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Skip Backward button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { onSeekBackward() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FastRewind,
                            contentDescription = "-10s",
                            tint = skin.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play Pause Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(skin.primary.copy(alpha = 0.15f))
                            .clickable { onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = skin.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Skip Forward button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { onSeekForward() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FastForward,
                            contentDescription = "+10s",
                            tint = skin.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Favorited Heart
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { onToggleFavorite() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (isFavorite) skin.accentRed else skin.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = skin.accentRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Audio Slider and Timestamps (Elapsed to Left / Duration to Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatDuration(position),
                    color = skin.onSurface.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
                Slider(
                    value = if (audioItem.duration > 0) (position.toFloat() / audioItem.duration).coerceIn(0f, 1f) else 0f,
                    onValueChange = { fraction ->
                        val targetPos = (fraction * audioItem.duration).toLong()
                        onSeekTo(targetPos)
                    },
                    modifier = Modifier.weight(1f).height(12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = skin.accentGold,
                        activeTrackColor = skin.primary,
                        inactiveTrackColor = skin.onSurface.copy(alpha = 0.15f)
                    )
                )
                Text(
                    text = formatDuration(audioItem.duration),
                    color = skin.onSurface.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun FullscreenAudioPlayer(
    audioItem: LocalMediaItem,
    isPlaying: Boolean,
    position: Long,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPlayPause: () -> Unit,
    onClosePlayer: () -> Unit,
    skin: GHSkin,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    viewModel: MainViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showMoreMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(skin.surface, skin.background)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("fullscreen_audio_player")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClosePlayer) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = skin.onBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Share Button
                    IconButton(onClick = {
                        try {
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "Now playing: ${audioItem.title} by ${audioItem.artist} on GHPlayer!")
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Track")
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            android.util.Log.e("GHPlayer", "Failed to share track", e)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = skin.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Favorite/Star Button
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) skin.accentGold else skin.onBackground,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // More actions Button
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = skin.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 2. Large Album Art Cover Art card (Centered with rounded corners)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.05f)
                        .clip(RoundedCornerShape(32.dp)),
                    colors = CardDefaults.cardColors(containerColor = skin.surface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        skin.primary.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.5f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Centered Large Music Icon
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .background(skin.onBackground.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = skin.primary,
                                    modifier = Modifier.size(54.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Nice audio visualizer animation block inside the cover when playing
                            Row(
                                modifier = Modifier.height(36.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val barCount = 10
                                for (i in 0 until barCount) {
                                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
                                    val duration = remember(i) { (600..1200).random() }
                                    val heightProportion by if (isPlaying) {
                                        infiniteTransition.animateFloat(
                                            initialValue = 0.2f,
                                            targetValue = 1.0f,
                                            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                                animation = androidx.compose.animation.core.tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                            )
                                        )
                                    } else {
                                        remember { mutableStateOf(0.15f) }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(5.dp)
                                            .fillMaxHeight(heightProportion)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(skin.primary, skin.accentGold)
                                                ),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Metadata Header (Left Aligned under the Cover art, matching screenshot)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = audioItem.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = skin.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = audioItem.artist.ifEmpty { "Unknown Artist" },
                    fontSize = 16.sp,
                    color = skin.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 4. Progress Seeker and Timestamps (Elapsed left, total right)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Slider(
                    value = if (audioItem.duration > 0) (position.toFloat() / audioItem.duration).coerceIn(0f, 1f) else 0f,
                    onValueChange = { fraction ->
                        val targetPos = (fraction * audioItem.duration).toLong()
                        onSeekTo(targetPos)
                    },
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = skin.primary,
                        activeTrackColor = skin.primary,
                        inactiveTrackColor = skin.onSurface.copy(alpha = 0.2f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(position),
                        color = skin.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatDuration(audioItem.duration),
                        color = skin.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 5. Control Button Action Row (Shuffle, Play/Pause, Prev, Next, Repeat)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button (Leftmost)
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (viewModel.isShuffleEnabled) skin.primary else skin.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Skip Previous Button (Left-center)
                IconButton(onClick = { viewModel.playPreviousAudio() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = skin.onBackground,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Centered large, theme-colored Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(skin.primary)
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = skin.background,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Skip Next Button (Right-center)
                IconButton(onClick = { viewModel.playNextAudio() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = skin.onBackground,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Repeat Button (Rightmost)
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = "Repeat",
                            tint = if (viewModel.repeatMode != RepeatMode.OFF) skin.primary else skin.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    if (viewModel.repeatMode == RepeatMode.ONE) {
                        Text(
                            text = "1",
                            color = skin.background,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(x = 6.dp, y = (-6).dp)
                                .background(skin.primary, CircleShape)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // More Menu Info Dialog
        if (showMoreMenu) {
            AlertDialog(
                onDismissRequest = { showMoreMenu = false },
                title = { Text("Track Specifications", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Title: ${audioItem.title}", fontWeight = FontWeight.SemiBold)
                        Text("Artist: ${audioItem.artist.ifEmpty { "Unknown" }}")
                        Text("Duration: ${formatDuration(audioItem.duration)}")
                        Text("Source: ${audioItem.folder}")
                        Text("Storage Path:\n${audioItem.path}", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                confirmButton = {
                    Button(onClick = { showMoreMenu = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }
    }
}

// ------------------------------------------------------------------
// Real Native Video Player using android.widget.VideoView
// ------------------------------------------------------------------
@Composable
fun RealVideoPlayerView(
    videoItem: LocalMediaItem,
    isPlaying: Boolean,
    position: Long,
    playbackSpeed: Float,
    skin: GHSkin,
    onPositionChanged: (Long) -> Unit,
    lastManualSeekTimeMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoUri = remember(videoItem.path) {
        if (videoItem.isStream) {
            Uri.parse(videoItem.path)
        } else {
            Uri.fromFile(java.io.File(videoItem.path))
        }
    }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    val currentLastSeekTimeMs by androidx.compose.runtime.rememberUpdatedState(lastManualSeekTimeMs)

    LaunchedEffect(isPlaying, videoItem, videoViewRef) {
        if (isPlaying) {
            while (true) {
                try {
                    videoViewRef?.let { view ->
                        if (view.isPlaying) {
                            val timeSinceSeek = System.currentTimeMillis() - currentLastSeekTimeMs
                            if (timeSinceSeek > 1500L) {
                                onPositionChanged(view.currentPosition.toLong())
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RealVideoPlayerView", "Error in position polling look", e)
                }
                delay(500)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                videoViewRef = this
                setVideoURI(videoUri)
                setOnPreparedListener { mp ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                        } catch (e: Exception) {
                            Log.e("RealVideoPlayerView", "Error adjusting speed", e)
                        }
                    }
                    seekTo(position.toInt())
                    if (isPlaying) {
                        start()
                    }
                }
                setOnCompletionListener {
                    onPositionChanged(0L)
                }
            }
        },
        update = { videoView ->
            videoViewRef = videoView
            if (isPlaying) {
                if (!videoView.isPlaying) {
                    videoView.start()
                }
            } else {
                if (videoView.isPlaying) {
                    videoView.pause()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    videoView.setOnPreparedListener { mp ->
                        mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                    }
                } catch (e: Exception) {
                    Log.e("RealVideoPlayerView", "Error setting playback speed on update", e)
                }
            }

            val currentViewPos = videoView.currentPosition.toLong()
            if (Math.abs(position - currentViewPos) > 2500L) {
                videoView.seekTo(position.toInt())
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

// ------------------------------------------------------------------
// Full Interactive Video Controls Overlay and VideoView Container
// ------------------------------------------------------------------
@Composable
fun FullscreenVideoPlayer(
    videoItem: LocalMediaItem,
    isPlaying: Boolean,
    position: Long,
    speed: Float,
    autoPause: Boolean,
    isFloating: Boolean,
    isBackgroundPlay: Boolean,
    activeTrack: String,
    subtitleUrl: String?,
    subtitleText: String?,
    subtitleTextSize: Float,
    subtitleColor: String,
    skin: GHSkin,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onChangePlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onChangeAutoPause: (Boolean) -> Unit,
    onChangeFloating: (Boolean) -> Unit,
    onChangeBackgroundPlay: (Boolean) -> Unit,
    onShowTracks: () -> Unit,
    onShowSubtitles: () -> Unit,
    onClosePlayer: () -> Unit,
    onSeekTo: (Long) -> Unit,
    lastManualSeekTimeMs: Long = 0L
) {
    val context = LocalContext.current
    var controlsVisible by remember { mutableStateOf(true) }
    var showLeftDoubleTapIndicator by remember { mutableStateOf(false) }
    var showRightDoubleTapIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    LaunchedEffect(showLeftDoubleTapIndicator) {
        if (showLeftDoubleTapIndicator) {
            delay(600)
            showLeftDoubleTapIndicator = false
        }
    }
    LaunchedEffect(showRightDoubleTapIndicator) {
        if (showRightDoubleTapIndicator) {
            delay(600)
            showRightDoubleTapIndicator = false
        }
    }

    if (isFloating) {
        // Floating Mini Picture-in-Picture simulator View
        var offsetX by remember { mutableStateOf(-20f) }
        var offsetY by remember { mutableStateOf(100f) }
        var isMiniSize by remember { mutableStateOf(true) }
        var isPiPMuted by remember { mutableStateOf(false) }

        val pipWidth = if (isMiniSize) 210.dp else 290.dp
        val pipHeight = if (isMiniSize) 130.dp else 180.dp

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .width(pipWidth + 32.dp)
                .height(pipHeight + 32.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Clamp values to keep the PiP window floating realistically inside visible viewport bounds
                            offsetX = (offsetX + dragAmount.x).coerceIn(-1000f, 100f)
                            offsetY = (offsetY + dragAmount.y).coerceIn(-100f, 2000f)
                        }
                    }
                    .border(1.5.dp, skin.primary.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .testTag("floating_video_player"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = skin.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Video View Frame
                    RealVideoPlayerView(
                        videoItem = videoItem,
                        isPlaying = isPlaying,
                        position = position,
                        playbackSpeed = speed,
                        skin = skin,
                        onPositionChanged = onSeekTo,
                        lastManualSeekTimeMs = lastManualSeekTimeMs
                    )

                    // Scrim overlay for easier button visibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )

                    // Top Action Overlay Bar (Drag helper tag, size ratio option, Close action)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OpenWith,
                                contentDescription = "Drag Handle",
                                tint = skin.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "PIP (${if (isMiniSize) "S" else "L"})",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Size toggle button
                            IconButton(
                                onClick = { isMiniSize = !isMiniSize },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMiniSize) Icons.Filled.AspectRatio else Icons.Filled.FitScreen,
                                    contentDescription = "Resize PiP",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }

                            // Mute toggle simulation button
                            IconButton(
                                onClick = { isPiPMuted = !isPiPMuted },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPiPMuted) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
                                    contentDescription = "Mute Toggle",
                                    tint = if (isPiPMuted) skin.accentRed else skin.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                            }

                            // Close button
                            IconButton(
                                onClick = { onClosePlayer() },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close",
                                    tint = skin.accentRed,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    // Bottom Control Deck (Backward, Play/Pause, Forward, Fullscreen Restore)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                            .padding(bottom = 6.dp, top = 4.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip Backward
                        IconButton(
                            onClick = { onSeekBackward() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FastRewind,
                                contentDescription = "-10s",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Play / Pause toggle
                        IconButton(
                            onClick = { onChangePlayPause() },
                            modifier = Modifier
                                .size(24.dp)
                                .background(skin.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/pause",
                                tint = skin.background,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Skip Forward
                        IconButton(
                            onClick = { onSeekForward() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FastForward,
                                contentDescription = "+10s",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Restore to Full Screen
                        IconButton(
                            onClick = { onChangeFloating(false) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Restore Fullscreen",
                                tint = skin.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Small overlay subtitle inside PIP
                    subtitleText?.let { text ->
                        val displayColor = when (subtitleColor) {
                            "White" -> Color.White
                            "Green" -> Color(0xFF2ECC71)
                            else -> Color(0xFFF1C40F) // Yellow
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 34.dp)
                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = text.replace("💬 ", ""),
                                fontSize = if (isMiniSize) 7.sp else 9.sp,
                                color = displayColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Sleek horizontal video tracking progress bar indicator at the very bottom
                    val progressRatio = if (videoItem.duration > 0) (position.toFloat() / videoItem.duration).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp),
                        color = skin.primary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }
    } else {
        // Fullscreen Overlay Interface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(enabled = false) {} // block clickthrough
        ) {
            // Underlay Video Stream/Decoder Panel
            Box(modifier = Modifier.fillMaxSize()) {
                RealVideoPlayerView(
                    videoItem = videoItem,
                    isPlaying = isPlaying,
                    position = position,
                    playbackSpeed = speed,
                    skin = skin,
                    onPositionChanged = onSeekTo,
                    lastManualSeekTimeMs = lastManualSeekTimeMs
                )
            }

            // Custom Gestures Overlay layer for Double-Tap seek (+10s and -10s) and Single-Tap toggle
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    controlsVisible = !controlsVisible
                                },
                                onDoubleTap = {
                                    onSeekBackward()
                                    showLeftDoubleTapIndicator = true
                                    showRightDoubleTapIndicator = false
                                }
                            )
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    controlsVisible = !controlsVisible
                                },
                                onDoubleTap = {
                                    onSeekForward()
                                    showRightDoubleTapIndicator = true
                                    showLeftDoubleTapIndicator = false
                                }
                            )
                        }
                )
            }

            // Left Double-Tap visual feedback indicator
            if (showLeftDoubleTapIndicator) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .fillMaxWidth(0.40f)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FastRewind,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "-10s",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Right Double-Tap visual feedback indicator
            if (showRightDoubleTapIndicator) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(0.40f)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.20f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FastForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "+10s",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            if (controlsVisible) {
                // UI Header Layer Binds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClosePlayer) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(videoItem.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Auto-Pause Mode: ${if (autoPause) "ON" else "OFF"} • Track: $activeTrack", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }

                    Row {
                        // Favorite toggle button
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (isFavorite) skin.accentRed else Color.White
                            )
                        }

                        // Floating button
                        IconButton(onClick = {
                            val activity = context.findActivity()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                try {
                                    val params = PictureInPictureParams.Builder().build()
                                    activity.enterPictureInPictureMode(params)
                                } catch (e: java.lang.Exception) {
                                    android.util.Log.e("FullscreenVideoPlayer", "Could not enter Picture-in-Picture mode", e)
                                    onChangeFloating(true) // Fallback to simulated Pip if exception
                                }
                            } else {
                                onChangeFloating(true) // Fallback to simulated Pip on older APIs or if activity is null
                            }
                        }) {
                            Icon(Icons.Filled.PictureInPicture, contentDescription = "PIP Mode", tint = skin.primary)
                        }

                        // Background audio play toggle
                        IconButton(onClick = { onChangeBackgroundPlay(!isBackgroundPlay) }) {
                            Icon(
                                imageVector = if (isBackgroundPlay) Icons.Filled.Headset else Icons.Filled.HeadsetOff,
                                contentDescription = "Background play",
                                tint = if (isBackgroundPlay) skin.primary else Color.White
                            )
                        }
                    }
                }
            }
            // Subtitle Display Render Area (If active/downloaded)
            subtitleText?.let { text ->
                val displayColor = when (subtitleColor) {
                    "White" -> Color.White
                    "Green" -> Color(0xFF2ECC71)
                    else -> Color(0xFFF1C40F) // "Yellow" gold custom branding
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (controlsVisible) 220.dp else 40.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = text,
                        fontSize = subtitleTextSize.sp,
                        color = displayColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            if (controlsVisible) {
                // Footer player Controls Panel
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    // Seeker Timeline bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(formatDuration(position), color = Color.White, fontSize = 11.sp)
                        Slider(
                            value = if (videoItem.duration > 0) (position.toFloat() / videoItem.duration).coerceIn(0f, 1f) else 0f,
                            onValueChange = { fraction ->
                                val targetPos = (fraction * videoItem.duration).toLong()
                                onSeekTo(targetPos)
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = skin.accentGold,
                                activeTrackColor = skin.accentGreen,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        Text(formatDuration(videoItem.duration), color = Color.White, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Control actions split for best ergonomics & responsiveness on any device screen size:
                    // Row 1: Quick Utilities (Tracks, Subtitles, Speed option dial, Auto pause)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Audio Tracks select quick icon
                        IconButton(onClick = onShowTracks) {
                            Icon(Icons.Filled.AudioFile, contentDescription = "Audio track", tint = Color.White)
                        }

                        // Speeds dial layout (0.5x to 2x)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { itemSpeed ->
                                val isSelected = speed == itemSpeed
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) skin.primary else Color.White.copy(alpha = 0.1f))
                                        .clickable { onChangeSpeed(itemSpeed) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "${itemSpeed}x",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) skin.background else Color.White
                                    )
                                }
                            }
                        }

                        // Subtitles download quick icon
                        IconButton(onClick = onShowSubtitles) {
                            Icon(Icons.Filled.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                        }

                        // Auto Pause Toggle
                        IconButton(onClick = { onChangeAutoPause(!autoPause) }) {
                            Icon(
                                imageVector = if (autoPause) Icons.Filled.Timer else Icons.Filled.TimerOff,
                                contentDescription = "Auto Pause",
                                tint = if (autoPause) skin.primary else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Large main centered playback triggers (Fast Rewind - Play/Pause - Fast Forward)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onSeekBackward,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FastRewind,
                                contentDescription = "-10s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(28.dp))

                        IconButton(
                            onClick = onChangePlayPause,
                            modifier = Modifier
                                .size(64.dp)
                                .background(skin.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/pause",
                                tint = skin.background,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(28.dp))

                        IconButton(
                            onClick = onSeekForward,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FastForward,
                                contentDescription = "+10s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Simulated Video Canvas Player view (Using full elegant visuals)
@Composable
fun SimulatedVideoCanvas(videoItem: LocalMediaItem, isPlaying: Boolean, skin: GHSkin) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Visual equalizer animation layout representing active playing state!
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0.8f, 0.4f, 0.9f, 0.5f, 0.7f, 0.3f, 0.6f).forEach { heightPercent ->
                    // Animated waves
                    val waveHeight = if (isPlaying) heightPercent * 60 else 10f
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(waveHeight.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(skin.accentRed, skin.accentGold, skin.accentGreen)
                                )
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${videoItem.title.take(35)}...",
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                if (isPlaying) "🎬 STREAM DECODING • GH QUALITY RENDER" else "⏸️ PLAYER PAUSED",
                color = skin.primary,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )
        }
    }
}

// ------------------------------------------------------------------
// Quick Help Dialogs / Overlays
// ------------------------------------------------------------------
@Composable
fun GHPConversionDialog(videoItem: LocalMediaItem?, progress: Float, skin: GHSkin) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = {
            Text("CONVERTING VIDEO TO MP3", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = skin.onBackground)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = videoItem?.title ?: "Converting file...",
                    fontSize = 12.sp,
                    color = skin.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress slider
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = skin.primary,
                    trackColor = skin.surface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${(progress * 100).toInt()}% COMPLETED",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = skin.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Saving output to custom folder \"GHplayer_Extracts\"",
                    fontSize = 10.sp,
                    color = skin.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        containerColor = skin.background
    )
}

@Composable
fun GHPDownloadSubtitleDialog(
    item: LocalMediaItem,
    viewModel: MainViewModel,
    skin: GHSkin,
    onDismiss: () -> Unit
) {
    var queryText by remember { mutableStateOf(item.title) }
    var forceNotFound by remember { mutableStateOf(false) }
    var actionStatusString by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Subtitles,
                    contentDescription = null,
                    tint = skin.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text("SUBTITLE CONTROL CENTER", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Active Status info card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(skin.background)
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "STATUS:",
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = skin.primary
                        )
                        Text(
                            text = if (viewModel.activeSubtitleUrl != null) {
                                "🟢 Active: ${viewModel.activeSubtitleUrl}"
                            } else {
                                "🔴 No active subtitle track"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = skin.onSurface
                        )
                        viewModel.activeSubtitleUrl?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { viewModel.clearSubtitle() },
                                colors = ButtonDefaults.textButtonColors(contentColor = skin.accentRed),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CLEAR SUBTITLE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Query text field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "SUBTITLE SOURCE SEARCH:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = skin.onSurface.copy(alpha = 0.6f)
                    )
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = skin.primary,
                            unfocusedBorderColor = skin.onSurface.copy(alpha = 0.2f),
                            focusedContainerColor = skin.background,
                            unfocusedContainerColor = skin.background
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Language select chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "LANGUAGE:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = skin.onSurface.copy(alpha = 0.6f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("English", "Akan (Twi)", "French").forEach { lang ->
                            val isSelected = viewModel.subtitleLanguage == lang
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) skin.primary else skin.onSurface.copy(alpha = 0.05f))
                                    .clickable { viewModel.subtitleLanguage = lang }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = lang,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) skin.background else skin.onSurface
                                )
                            }
                        }
                    }
                }

                // Toggle simulate error
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { forceNotFound = !forceNotFound }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simulate Subtitle Not Found", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = skin.onSurface)
                        Text("Simulate network or server 404 failure.", fontSize = 9.sp, color = skin.onSurface.copy(alpha = 0.5f))
                    }
                    Switch(
                        checked = forceNotFound,
                        onCheckedChange = { forceNotFound = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = skin.primary,
                            checkedTrackColor = skin.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = skin.onSurface.copy(alpha = 0.3f),
                            uncheckedTrackColor = skin.onSurface.copy(alpha = 0.1f)
                        )
                    )
                }

                // Status banner
                actionStatusString?.let { status ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(skin.primary.copy(alpha = 0.1f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = status,
                            color = skin.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Error Warning Card
                viewModel.subtitleDownloadError?.let { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, skin.accentRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .background(skin.accentRed.copy(alpha = 0.05f))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = skin.accentRed, modifier = Modifier.size(16.dp))
                                Text("SUBTITLE TRACK NOT FOUND", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = skin.accentRed)
                            }
                            Text(err, fontSize = 10.sp, color = skin.onSurface.copy(alpha = 0.8f))
                            Text("💡 Try loading offline backup using 'LOAD OFFLINE MOCK' below.", fontSize = 9.sp, color = skin.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }

                // Actions Button Row
                if (viewModel.isDownloadingSubtitle) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CircularProgressIndicator(color = skin.primary, modifier = Modifier.size(24.dp))
                        Text("Querying OpenSubtitles db...", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = skin.primary)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.downloadSubtitleWithSearchAndLanguage(
                                    item = item,
                                    query = queryText,
                                    lang = viewModel.subtitleLanguage,
                                    forceNotFound = forceNotFound
                                ) { success, msg ->
                                    actionStatusString = msg
                                    if (success) {
                                        scope.launch {
                                            delay(1500)
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = skin.primary, contentColor = skin.background),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SEARCH & DOWNLOAD ONLINE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.loadLocalMockFile(item)
                                    actionStatusString = "Loaded offline local simulated subtitle!"
                                    scope.launch {
                                        delay(1200)
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.weight(1.3f),
                                border = BorderStroke(1.dp, skin.primary.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = skin.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LOAD OFFLINE MOCK", fontSize = 10.sp)
                            }

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(0.7f),
                                colors = ButtonDefaults.buttonColors(containerColor = skin.onSurface.copy(alpha = 0.08f), contentColor = skin.onSurface),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("CLOSE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = skin.onSurface.copy(alpha = 0.1f))

                // Typography & Offset sync preferences
                Text("STYLE & SYNCHRONIZER OFFSETS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = skin.primary)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Offset Delay Adjust:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = skin.onSurface)
                        Text("${viewModel.subtitleOffsetMs}ms ${if (viewModel.subtitleOffsetMs > 0) "delayed" else if (viewModel.subtitleOffsetMs < 0) "ahead" else "(synced)"}", fontSize = 10.sp, color = skin.primary, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(-500L, -100L, 0L, 100L, 500L).forEach { delta ->
                            val txt = if (delta == 0L) "RESET" else if (delta > 0) "+${delta}" else "$delta"
                            Button(
                                onClick = { if (delta == 0L) viewModel.subtitleOffsetMs = 0L else viewModel.subtitleOffsetMs += delta },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (delta == 0L) skin.primary.copy(alpha = 0.15f) else skin.background,
                                    contentColor = if (delta == 0L) skin.primary else skin.onSurface
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp)
                            ) {
                                Text(txt, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Font Size Settings slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Disp Font Sizing:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = skin.onSurface)
                        Text("${viewModel.subtitleTextSize.toInt()} sp", fontSize = 10.sp, color = skin.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = viewModel.subtitleTextSize,
                        onValueChange = { viewModel.subtitleTextSize = it },
                        valueRange = 10f..24f,
                        colors = SliderDefaults.colors(
                            thumbColor = skin.accentGold,
                            activeTrackColor = skin.primary,
                            inactiveTrackColor = skin.onSurface.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.height(14.dp)
                    )
                }

                // Typography selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Typography Tone Coloring:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = skin.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Yellow" to Color(0xFFF1C40F),
                            "White" to Color.White,
                            "Green" to Color(0xFF2ECC71)
                        ).forEach { (name, color) ->
                            val isSelected = viewModel.subtitleColor == name
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) skin.primary else skin.onSurface.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .background(skin.background)
                                    .clickable { viewModel.subtitleColor = name }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, CircleShape)
                                        .clip(CircleShape)
                                )
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) skin.primary else skin.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = skin.surface
    )
}

@Composable
fun GHPSelectTrackDialog(
    item: LocalMediaItem,
    activeTrack: String,
    skin: GHSkin,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("AUDIO TRACKS & TRANSLATIONS", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.audioTracks.forEach { track ->
                    val isSelected = track == activeTrack
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) skin.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { onSelect(track) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = track,
                            color = if (isSelected) skin.primary else skin.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = skin.primary)
                        }
                    }
                }
            }
        },
        containerColor = skin.surface
    )
}

// ------------------------------------------------------------------
// Internal helper layout bounds / spaces
// ------------------------------------------------------------------
fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 60000) % 60
    val hr = (ms / 3600000)
    return if (hr > 0) {
        String.format("%02d:%02d:%02d", hr, min, sec)
    } else {
        String.format("%02d:%02d", min, sec)
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val kb = bytes / 1024f
    val mb = kb / 1024f
    return if (mb >= 1.0) {
        String.format("%.1f MB", mb)
    } else {
        String.format("%.1f KB", kb)
    }
}

val Arrangement.SpaceSpaceBetween: Arrangement.Horizontal
    get() = Arrangement.SpaceBetween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GHPTimerBottomSheet(
    viewModel: MainViewModel,
    skin: GHSkin,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = skin.surface,
        contentColor = skin.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = skin.onSurface.copy(alpha = 0.2f))
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                text = "Timer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = skin.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timer list of options
            val options = listOf(
                "Off" to 0,
                "15min" to 15,
                "30min" to 30,
                "45min" to 45,
                "60min" to 60,
                "Custom" to -1
            )

            // Detect currently active predefined selected timer integer
            val currentTimer = viewModel.sleepTimerMinutes
            val currentSelectedValue = when {
                currentTimer == 0 -> 0
                currentTimer == 15 -> 15
                currentTimer == 30 -> 30
                currentTimer == 45 -> 45
                currentTimer == 60 -> 60
                else -> -1 // Custom or custom active
            }

            options.forEach { (label, value) ->
                val isSelected = (value == currentSelectedValue)

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (value == -1) {
                                    viewModel.setSleepTimer(viewModel.customTimerMinutes)
                                } else {
                                    viewModel.setSleepTimer(value)
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = skin.onSurface
                        )

                        // Beautiful circular radio button indicators exact style as attachment
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) skin.primary else skin.onSurface.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(skin.primary, CircleShape)
                                )
                            }
                        }
                    }

                    // If custom is ticked, expand premium customizable slider under option Row!
                    if (value == -1 && isSelected) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .background(skin.background.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Custom Duration",
                                    fontSize = 12.sp,
                                    color = skin.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${viewModel.customTimerMinutes} minutes",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = skin.primary
                                )
                            }
                            Slider(
                                value = viewModel.customTimerMinutes.toFloat(),
                                onValueChange = {
                                    val roundedValue = it.toInt()
                                    viewModel.customTimerMinutes = roundedValue
                                    viewModel.setSleepTimer(roundedValue)
                                },
                                valueRange = 1f..120f,
                                steps = 119,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = skin.primary,
                                    inactiveTrackColor = skin.onSurface.copy(alpha = 0.1f),
                                    thumbColor = skin.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(skin.onSurface.copy(alpha = 0.08f)))
            Spacer(modifier = Modifier.height(20.dp))

            // Lower controls switch for end-of-track sleep triggers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.closeAppAfterCurrentSong = !viewModel.closeAppAfterCurrentSong },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "Close the App after This Song",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = skin.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When it's on, it will wait until the current song...",
                        style = MaterialTheme.typography.bodySmall,
                        color = skin.onSurface.copy(alpha = 0.5f)
                    )
                }

                Switch(
                    checked = viewModel.closeAppAfterCurrentSong,
                    onCheckedChange = { viewModel.closeAppAfterCurrentSong = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = skin.primary,
                        uncheckedThumbColor = skin.onSurface.copy(alpha = 0.4f),
                        uncheckedTrackColor = skin.onSurface.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
