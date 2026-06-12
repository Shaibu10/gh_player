package com.example.ui

import android.widget.MediaController
import android.widget.VideoView
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.example.data.LocalMediaItem
import com.example.data.database.MediaHistory
import com.example.data.database.Playlist
import com.example.data.database.PlaylistSong
import com.example.ui.theme.GHSkin
import com.example.ui.theme.SkinsList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

fun hasAllPermissions(context: Context): Boolean {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO
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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                    Manifest.permission.READ_MEDIA_VIDEO
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
                Manifest.permission.READ_MEDIA_VIDEO
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
                    skin = skin,
                    onDismiss = { showSubtitleDialog = null },
                    onConfirmDownload = {
                        viewModel.downloadSubtitle(item) { msg ->
                            scope.launch {
                                showSubtitleDialog = null
                                // Auto show alert
                                delay(100)
                            }
                        }
                    }
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
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    MiniAudioPlayerCard(
                        audioItem = activeAudio,
                        isPlaying = viewModel.isPlaying,
                        position = viewModel.currentPlaybackPosition,
                        onPlayPause = {
                            if (viewModel.isPlaying) viewModel.pauseAudio() else viewModel.resumePlayback()
                        },
                        onClose = { viewModel.stopPlayback() },
                        skin = skin,
                        onSeekForward = { viewModel.seekForward() },
                        onSeekBackward = { viewModel.seekBackward() }
                    )
                }
            }

            // Render detailed fullscreen overlay playing view if active Video
            viewModel.activeVideoItem?.let { activeVideo ->
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
                    skin = skin,
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
                    onClosePlayer = { viewModel.stopPlayback() }
                )
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
                    VideoItemCard(
                        video = video,
                        skin = skin,
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
                            Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = skin.accentRed,
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

    var activeMusicSubTab by remember { mutableStateOf("all_tracks") } // "all_tracks" or "playlists"
    var selectedPlaylistView by remember { mutableStateOf<Playlist?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songToAddToPlaylist by remember { mutableStateOf<LocalMediaItem?>(null) }

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

    // Dialog for Adding song to selected Playlist
    if (songToAddToPlaylist != null) {
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            containerColor = skin.surface,
            titleContentColor = skin.primary,
            textContentColor = skin.onSurface,
            title = { Text("Add Track to Playlist", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text(
                        text = "Add \"${songToAddToPlaylist?.title}\" to:",
                        fontSize = 13.sp,
                        color = skin.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (playlists.isEmpty()) {
                        Text(
                            "No playlists created yet.",
                            fontSize = 12.sp,
                            color = skin.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                        ) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(playlists) { playlist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(skin.surface.copy(alpha = 0.3f))
                                            .clickable {
                                                viewModel.addSongToPlaylist(playlist.id, songToAddToPlaylist!!)
                                                songToAddToPlaylist = null
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.PlaylistPlay,
                                            contentDescription = null,
                                            tint = skin.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = playlist.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = skin.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Or create a new playlist:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = skin.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var quickPlaylistName by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quickPlaylistName,
                            onValueChange = { quickPlaylistName = it },
                            placeholder = { Text("New Playlist Name", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = skin.onSurface.copy(alpha = 0.2f),
                                focusedBorderColor = skin.primary,
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent
                            )
                        )
                        Button(
                            onClick = {
                                if (quickPlaylistName.isNotBlank() && songToAddToPlaylist != null) {
                                    viewModel.createPlaylistWithSong(quickPlaylistName, songToAddToPlaylist!!)
                                    songToAddToPlaylist = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = skin.primary, contentColor = skin.background),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { songToAddToPlaylist = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = skin.primary)
                ) {
                    Text("Close")
                }
            }
        )
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
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(SortOption.TITLE, SortOption.DURATION, SortOption.SIZE, SortOption.DATE).forEach { option ->
                        val isSelected = viewModel.audioSortOption == option
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) skin.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                .background(skin.surface.copy(alpha = 0.5f))
                                .clickable { viewModel.setAudioSort(option) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = option.name,
                                fontSize = 8.sp,
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
                        AudioFileRow(
                            audio = audio,
                            isActive = isActive,
                            skin = skin,
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
                    Icons.Filled.Favorite,
                    contentDescription = "Favorite",
                    tint = skin.accentRed.copy(alpha = 0.8f),
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

        Spacer(modifier = Modifier.height(16.dp))

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
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val displayedList = if (selectedFilterTab == "History") historyList else favoritesList

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
                        text = "Your ${selectedFilterTab.lowercase()} is currently empty",
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
                        onDeleteClick = { viewModel.removeFromHistory(record.id) }
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
    onDeleteClick: () -> Unit
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
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    skin: GHSkin,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mini_audio_player"),
        colors = CardDefaults.cardColors(containerColor = skin.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Disk Animation
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(skin.accentGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = skin.background)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = audioItem.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = skin.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = audioItem.artist,
                        fontSize = 11.sp,
                        color = skin.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Controls Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSeekBackward) {
                        Icon(Icons.Filled.FastRewind, contentDescription = "-10s", tint = skin.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play Pause",
                            tint = skin.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onSeekForward) {
                        Icon(Icons.Filled.FastForward, contentDescription = "+10s", tint = skin.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = skin.accentRed)
                    }
                }
            }
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

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
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
            }
        },
        update = { videoView ->
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
            if (Math.abs(position - currentViewPos) > 2000L) {
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
    skin: GHSkin,
    onChangePlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onChangeAutoPause: (Boolean) -> Unit,
    onChangeFloating: (Boolean) -> Unit,
    onChangeBackgroundPlay: (Boolean) -> Unit,
    onShowTracks: () -> Unit,
    onShowSubtitles: () -> Unit,
    onClosePlayer: () -> Unit
) {
    if (isFloating) {
        // Floating Mini Picture-in-Picture simulator View
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {},
            contentAlignment = Alignment.TopEnd
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .width(220.dp)
                    .height(140.dp)
                    .testTag("floating_video_player"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = skin.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Video View Frame
                    RealVideoPlayerView(
                        videoItem = videoItem,
                        isPlaying = isPlaying,
                        position = position,
                        playbackSpeed = speed,
                        skin = skin,
                        onPositionChanged = {}
                    )

                    // Control buttons
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onChangePlayPause() }, modifier = Modifier.size(24.dp)) {
                            Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, tint = skin.primary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onChangeFloating(false) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Fullscreen, contentDescription = "Restore", tint = skin.primary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onClosePlayer() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = skin.accentRed, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Watermark
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(skin.accentRed, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("PIP MODE", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = skin.onBackground)
                    }
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
                    onPositionChanged = {}
                )
            }

            // Custom Gestures Overlay layer for Double-Tap seek (+10s and -10s)
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    onSeekBackward()
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
                                onDoubleTap = {
                                    onSeekForward()
                                }
                            )
                        }
                )
            }

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
                    // Floating button
                    IconButton(onClick = { onChangeFloating(true) }) {
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

            // Subtitle Display Render Area (If active/downloaded)
            subtitleUrl?.let { sub ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (sub == "Downloading...") "📥 Downloading subtitle language mapping..." 
                               else "💬 Subtitle loaded: Ghanaian English Track Active",
                        fontSize = 12.sp,
                        color = skin.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Footer player Controls Panel
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(16.dp)
            ) {
                // Seeker Timeline bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("0:00", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = 0.35f,
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = skin.accentGold,
                            activeTrackColor = skin.accentGreen,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Text(formatDuration(videoItem.duration), color = Color.White, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Control actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceSpaceBetween,
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
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
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

                    // Main execution buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onSeekBackward) {
                            Icon(Icons.Filled.FastRewind, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        IconButton(
                            onClick = onChangePlayPause,
                            modifier = Modifier
                                .background(skin.primary, CircleShape)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/pause",
                                tint = skin.background,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = onSeekForward) {
                            Icon(Icons.Filled.FastForward, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(24.dp))
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
    skin: GHSkin,
    onDismiss: () -> Unit,
    onConfirmDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirmDownload,
                colors = ButtonDefaults.buttonColors(containerColor = skin.primary, contentColor = skin.background)
            ) {
                Text("DOWNLOAD ONLINE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = skin.onSurface)) {
                Text("CANCEL")
            }
        },
        title = {
            Text("SUBTITLE DOWNLOADS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Download subtitles for: ${item.title}",
                    fontSize = 13.sp,
                    color = skin.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    text = "System searches and downloads official Ghanaian and global English linguistic subtitles directly to your local file path.",
                    fontSize = 11.sp,
                    color = skin.onSurface.copy(alpha = 0.6f)
                )
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
