package com.whyj03.yucamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun CameraScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    fun checkAllPermissions(): Boolean {
        val cameraOk = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        return cameraOk && storageOk
    }
    var hasPermission by remember { mutableStateOf(checkAllPermissions()) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasPermission = results[Manifest.permission.CAMERA] == true &&
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
             results[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true)
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            val perms = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                arrayOf(Manifest.permission.CAMERA)
            }
            permLauncher.launch(perms)
        }
    }

    if (!hasPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("카메라 권한이 필요합니다", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = {
                    val perms = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        arrayOf(Manifest.permission.CAMERA)
                    }
                    permLauncher.launch(perms)
                }) {
                    Text("권한 허용")
                }
            }
        }
        return
    }

    var showDialog by remember { mutableStateOf(false) }
    var tempFile by remember { mutableStateOf<File?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val photoPrefix by viewModel.photoPrefix.collectAsState()
    val prefixCounter by viewModel.prefixCounter.collectAsState()
    var zoomRatio by remember { mutableStateOf(1f) }
    var showZoomIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(showZoomIndicator) {
        if (showZoomIndicator) {
            delay(1500)
            showZoomIndicator = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(camera) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    val cam = camera ?: return@detectTransformGestures
                    val zoomState = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures
                    val newRatio = (zoomState.zoomRatio * zoomChange)
                        .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                    cam.cameraControl.setZoomRatio(newRatio)
                    zoomRatio = newRatio
                    showZoomIndicator = true
                }
            }
    ) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onCameraReady = { capture, cam ->
                imageCapture = capture
                camera = cam
            }
        )

        // Zoom indicator
        AnimatedVisibility(
            visible = showZoomIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = "%.1fx".format(zoomRatio),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // Shutter button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            Button(
                onClick = {
                    val ic = imageCapture ?: return@Button
                    val tmp = File(context.cacheDir, "tmp_capture.jpg")
                    ic.takePicture(
                        ImageCapture.OutputFileOptions.Builder(tmp).build(),
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                tempFile = tmp
                                showDialog = true
                            }
                            override fun onError(e: ImageCaptureException) {
                                Toast.makeText(context, "촬영 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                },
                modifier = Modifier
                    .size(72.dp)
                    .border(4.dp, Color.White, CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {}
        }
    }

    if (showDialog) {
        PhotoNameDialog(
            prefix = photoPrefix,
            prefixCounter = prefixCounter,
            onPrefixChange = { viewModel.updatePhotoPrefix(it) },
            onConfirm = { name ->
                tempFile?.let { viewModel.onPhotoCaptured(it, name) }
                showDialog = false
            },
            onDismiss = {
                tempFile?.delete()
                showDialog = false
            }
        )
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onCameraReady: (ImageCapture, Camera) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            provider.unbindAll()
            val cam = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
            onCameraReady(capture, cam)
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
