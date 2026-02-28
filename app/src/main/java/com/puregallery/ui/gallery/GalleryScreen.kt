package com.puregallery.ui.gallery

import android.Manifest
import android.app.Activity
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.puregallery.model.PhotoItem
import com.puregallery.model.PhotoStatus
import com.puregallery.media.FavoriteExecutor
import com.puregallery.ui.theme.RedOverlay
import android.content.pm.PackageManager
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape

private enum class SwipeFeedback(val label: String, val color: Color) {
    DELETE_ON("已标记待删除", Color(0xFFD32F2F)),
    DELETE_OFF("已取消待删除", Color(0xFF616161)),
    FAVORITE_ON("已收藏", Color(0xFFE91E63)),
    FAVORITE_OFF("已取消收藏", Color(0xFF616161))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    onOpenReview: () -> Unit,
    viewModel: GalleryViewModel = viewModel()
) {
    data class PendingFavoriteAction(
        val photoId: Long,
        val uri: Uri,
        val shouldFavorite: Boolean
    )

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteExecutor = remember(context) { FavoriteExecutor(context) }

    val readPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                readPermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.loadPhotos()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadPhotos()
        }
    }

    DisposableEffect(context, lifecycleOwner, hasPermission) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasPermission) {
                viewModel.loadPhotos(showLoading = false)
            }
        }

        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (hasPermission) {
                    viewModel.loadPhotos(showLoading = false)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        context.contentResolver.registerContentObserver(collection, true, contentObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            context.contentResolver.unregisterContentObserver(contentObserver)
        }
    }

    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var lastViewedIndex by remember { mutableStateOf(0) }
    var wasViewerOpen by remember { mutableStateOf(false) }
    var shuffleEnabled by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    var pendingFavoriteAction by remember { mutableStateOf<PendingFavoriteAction?>(null) }

    BackHandler(enabled = viewerIndex != null) {
        viewerIndex = null
    }

    LaunchedEffect(viewerIndex, uiState.photos.size) {
        if (viewerIndex != null) {
            wasViewerOpen = true
        } else if (wasViewerOpen && uiState.photos.isNotEmpty()) {
            val target = lastViewedIndex.coerceIn(0, uiState.photos.lastIndex)
            gridState.scrollToItem(target)
            wasViewerOpen = false
        }
    }

    val favoriteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val action = pendingFavoriteAction
        pendingFavoriteAction = null
        if (action != null && result.resultCode == Activity.RESULT_OK) {
            favoriteExecutor.applyFavoriteDirectly(action.uri, action.shouldFavorite)
            viewModel.loadPhotos(showLoading = false)
        }
    }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = onOpenReview) {
                    Text(text = "清理列表 (${uiState.pendingDeleteCount})")
                }
            }
        }
    ) { innerPadding ->
        when {
            !hasPermission -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "需要相册读取权限",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "PureGallery 完全离线运行，仅在本地读取你的照片。",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    OutlinedButton(
                        onClick = { permissionLauncher.launch(readPermission) },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(text = "授予权限")
                    }
                }
            }

            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    OutlinedButton(
                        onClick = { viewModel.loadPhotos() },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(text = "重试")
                    }
                }
            }

            uiState.photos.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "未找到照片")
                }
            }

            else -> {
                if (viewerIndex == null) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(120.dp),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.photos, key = { it.id }) { photo ->
                            PhotoGridItem(
                                photo = photo,
                                onClick = {
                                    val index = uiState.photos.indexOfFirst { it.id == photo.id }
                                        .takeIf { it >= 0 }
                                    if (index != null) {
                                        lastViewedIndex = index
                                        viewerIndex = index
                                    }
                                }
                            )
                        }
                    }
                } else {
                    SwipeViewer(
                        photos = uiState.photos,
                        startIndex = viewerIndex ?: 0,
                        shuffleEnabled = shuffleEnabled,
                        onShuffleToggle = { shuffleEnabled = !shuffleEnabled },
                        onCurrentPhotoChanged = {
                            viewerIndex = it
                            lastViewedIndex = it
                        },
                        onSwipeUp = { photoId -> viewModel.toggleDelete(photoId) },
                        onSwipeDown = { photoId ->
                            val target = uiState.photos.firstOrNull { it.id == photoId }
                            if (target == null) return@SwipeViewer
                            val shouldFavorite = target.status != PhotoStatus.FAVORITE

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val sender = favoriteExecutor.buildFavoriteIntentSender(
                                    uri = target.uri,
                                    favorite = shouldFavorite
                                )
                                if (sender != null) {
                                    pendingFavoriteAction = PendingFavoriteAction(
                                        photoId = photoId,
                                        uri = target.uri,
                                        shouldFavorite = shouldFavorite
                                    )
                                    favoriteLauncher.launch(
                                        IntentSenderRequest.Builder(sender).build()
                                    )
                                } else {
                                    favoriteExecutor.applyFavoriteDirectly(target.uri, shouldFavorite)
                                    viewModel.loadPhotos(showLoading = false)
                                }
                            } else {
                                viewModel.setFavoriteStatus(photoId, shouldFavorite)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: PhotoItem,
    onClick: () -> Unit
) {
    val itemShape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(itemShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, itemShape)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (photo.status == PhotoStatus.PENDING_DELETE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RedOverlay)
            )

            Text(
                text = "待删",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.onError,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }

        if (photo.status == PhotoStatus.FAVORITE) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeViewer(
    photos: List<PhotoItem>,
    startIndex: Int,
    shuffleEnabled: Boolean,
    onShuffleToggle: () -> Unit,
    onCurrentPhotoChanged: (Int) -> Unit,
    onSwipeUp: (Long) -> Unit,
    onSwipeDown: (Long) -> Unit
) {
    val safeStartIndex = startIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
    val photoIds = remember(photos) { photos.map { it.id } }
    var currentPhotoId by remember(photoIds) {
        mutableStateOf(photos.getOrNull(safeStartIndex)?.id)
    }
    var playOrder by remember(photoIds) {
        mutableStateOf((0 until photos.size).toList())
    }

    val pagerState = rememberPagerState(
        initialPage = safeStartIndex,
        pageCount = { playOrder.size }
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(shuffleEnabled, photoIds) {
        if (photos.isEmpty()) return@LaunchedEffect

        val anchorId = currentPhotoId ?: photos[safeStartIndex].id
        val anchorIndex = photos.indexOfFirst { it.id == anchorId }
            .takeIf { it >= 0 }
            ?: safeStartIndex

        val newOrder = buildPlayOrder(
            totalCount = photos.size,
            anchorIndex = anchorIndex,
            shuffleEnabled = shuffleEnabled
        )

        playOrder = newOrder
        val targetPage = newOrder.indexOf(anchorIndex)
            .takeIf { it >= 0 }
            ?: 0
        pagerState.scrollToPage(targetPage)
    }

    LaunchedEffect(pagerState.currentPage, playOrder, photoIds) {
        if (photos.isEmpty()) return@LaunchedEffect
        val currentIndex = playOrder.getOrElse(pagerState.currentPage) { safeStartIndex }
        currentPhotoId = photos.getOrNull(currentIndex)?.id
        onCurrentPhotoChanged(currentIndex)
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp, bottom = 10.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${photos.size}",
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (shuffleEnabled) "随机" else "顺序",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable(onClick = onShuffleToggle)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    ) { innerPadding ->
        val bottomPadding = innerPadding.calculateBottomPadding()

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding)
        ) { page ->
            val photo = photos[playOrder[page]]
            var totalDragX by remember(photo.id) { mutableStateOf(0f) }
            var totalDragY by remember(photo.id) { mutableStateOf(0f) }
            var swipeFeedback by remember(photo.id) { mutableStateOf<SwipeFeedback?>(null) }

            LaunchedEffect(swipeFeedback) {
                if (swipeFeedback != null) {
                    kotlinx.coroutines.delay(500)
                    swipeFeedback = null
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(photo.id) {
                        detectDragGestures(
                            onDrag = { _, dragAmount ->
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y
                            },
                            onDragEnd = {
                                val threshold = 120f
                                val absX = kotlin.math.abs(totalDragX)
                                val absY = kotlin.math.abs(totalDragY)

                                when {
                                    absX > absY && totalDragX < -threshold -> {
                                        scope.launch {
                                            val next = (pagerState.currentPage + 1).coerceAtMost(playOrder.lastIndex)
                                            pagerState.animateScrollToPage(next)
                                        }
                                    }

                                    absX > absY && totalDragX > threshold -> {
                                        scope.launch {
                                            val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                                            pagerState.animateScrollToPage(prev)
                                        }
                                    }

                                    absY >= absX && totalDragY < -threshold -> {
                                        val wasPendingDelete = photo.status == PhotoStatus.PENDING_DELETE
                                        scope.launch {
                                            onSwipeUp(photo.id)
                                            swipeFeedback = if (wasPendingDelete) {
                                                SwipeFeedback.DELETE_OFF
                                            } else {
                                                SwipeFeedback.DELETE_ON
                                            }
                                            val next = (pagerState.currentPage + 1).coerceAtMost(playOrder.lastIndex)
                                            pagerState.animateScrollToPage(next)
                                        }
                                    }

                                    absY >= absX && totalDragY > threshold -> {
                                        val wasFavorite = photo.status == PhotoStatus.FAVORITE
                                        scope.launch {
                                            onSwipeDown(photo.id)
                                            swipeFeedback = if (wasFavorite) {
                                                SwipeFeedback.FAVORITE_OFF
                                            } else {
                                                SwipeFeedback.FAVORITE_ON
                                            }
                                            val next = (pagerState.currentPage + 1).coerceAtMost(playOrder.lastIndex)
                                            pagerState.animateScrollToPage(next)
                                        }
                                    }
                                }

                                totalDragX = 0f
                                totalDragY = 0f
                            }
                        )
                    }
            ) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                if (photo.status == PhotoStatus.PENDING_DELETE) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(RedOverlay)
                    )
                }

                if (photo.status == PhotoStatus.FAVORITE) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                }

                AnimatedVisibility(
                    visible = swipeFeedback != null,
                    modifier = Modifier.align(Alignment.Center),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = (swipeFeedback?.color ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = swipeFeedback?.label ?: "",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

private fun buildPlayOrder(
    totalCount: Int,
    anchorIndex: Int,
    shuffleEnabled: Boolean
): List<Int> {
    val normalizedAnchor = anchorIndex.coerceIn(0, (totalCount - 1).coerceAtLeast(0))
    if (!shuffleEnabled || totalCount <= 1) {
        return (0 until totalCount).toList()
    }

    val remaining = (0 until totalCount)
        .filter { it != normalizedAnchor }
        .shuffled()
    return listOf(normalizedAnchor) + remaining
}
