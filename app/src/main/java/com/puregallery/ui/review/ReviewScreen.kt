package com.puregallery.ui.review

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import com.puregallery.ui.theme.RedOverlay

@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    viewModel: ReviewViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onSystemDeleteSucceeded(uiState.photos.map { it.id })
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPending()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        when {
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = uiState.errorMessage.orEmpty())
                    OutlinedButton(
                        onClick = { viewModel.loadPending() },
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(text = "重试")
                    }
                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(text = "返回")
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "待删除复核（${uiState.photos.size}）",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (uiState.photos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "当前没有待删除照片")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(120.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(uiState.photos, key = { it.id }) { photo ->
                                val itemShape = RoundedCornerShape(14.dp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(itemShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, itemShape)
                                ) {
                                    AsyncImage(
                                        model = photo.uri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(RedOverlay)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val uris = uiState.photos.map { it.uri }
                            if (uris.isEmpty()) {
                                return@Button
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val intentSender = viewModel.buildDeleteIntentSender(uris)
                                if (intentSender != null) {
                                    deleteLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                }
                            } else {
                                viewModel.deleteDirectlyForApi29(
                                    contentResolver = context.contentResolver,
                                    uris = uris,
                                    onFinished = { _ -> }
                                )
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 12.dp)
                    ) {
                        Text(text = "确认清空")
                    }

                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = "返回")
                    }
                }
            }
        }
    }
}
