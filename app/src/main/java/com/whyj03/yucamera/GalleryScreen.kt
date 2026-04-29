package com.whyj03.yucamera

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun GalleryScreen(viewModel: AppViewModel) {
    val photos by viewModel.photos.collectAsState()
    val snackMessage by viewModel.snackMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackMessage) {
        snackMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("전체 삭제") },
            text = { Text("사진 ${photos.size}장을 모두 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAllPhotos(); showDeleteAllDialog = false }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("취소") }
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "갤러리 (${photos.size}장)",
                    style = MaterialTheme.typography.titleMedium
                )
                if (photos.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.uploadAllPending() }) {
                            Text("전체 업로드")
                        }
                        OutlinedButton(
                            onClick = { showDeleteAllDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("전체 삭제")
                        }
                    }
                }
            }

            if (photos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("촬영된 사진이 없습니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(photos, key = { it.file.absolutePath }) { photo ->
                        PhotoCard(
                            photo = photo,
                            onUpload = { viewModel.uploadPhoto(photo) },
                            onDelete = { viewModel.deletePhoto(photo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCard(photo: PhotoItem, onUpload: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = photo.file,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(photo.displayName, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                StatusBadge(photo.uploadStatus)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (photo.uploadStatus != UploadStatus.SUCCESS && photo.uploadStatus != UploadStatus.UPLOADING) {
                    FilledTonalButton(
                        onClick = onUpload,
                        modifier = Modifier.width(76.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("업로드", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (photo.uploadStatus == UploadStatus.UPLOADING) {
                    CircularProgressIndicator(Modifier.size(24.dp).align(Alignment.CenterHorizontally), strokeWidth = 2.dp)
                }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.width(76.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("삭제", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("사진 삭제") },
            text = { Text("\"${photo.displayName}\" 을(를) 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun StatusBadge(status: UploadStatus) {
    val (label, color) = when (status) {
        UploadStatus.PENDING   -> "미업로드" to MaterialTheme.colorScheme.outline
        UploadStatus.UPLOADING -> "업로드 중" to MaterialTheme.colorScheme.primary
        UploadStatus.SUCCESS   -> "완료" to Color(0xFF2E7D32)
        UploadStatus.FAILED    -> "실패" to MaterialTheme.colorScheme.error
    }
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
