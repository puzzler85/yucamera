package com.whyj03.yucamera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ShareListState {
    object Idle : ShareListState()
    object Loading : ShareListState()
    data class Success(val shares: List<String>) : ShareListState()
    data class Error(val message: String) : ShareListState()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos: StateFlow<List<PhotoItem>> = _photos.asStateFlow()

    private val _nasConfig = MutableStateFlow(NasConfigPrefs.load(context))
    val nasConfig: StateFlow<NasConfig> = _nasConfig.asStateFlow()

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    init {
        loadExistingPhotos()
    }

    private fun loadExistingPhotos() {
        val files = getPhotoDir().listFiles { f -> f.extension.lowercase() == "jpg" } ?: return
        _photos.value = files.sortedByDescending { it.lastModified() }
            .map { PhotoItem(it, it.nameWithoutExtension) }
    }

    fun getPhotoDir(): File {
        val dir = File(context.filesDir, "photos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun onPhotoCaptured(tempFile: File, name: String): String {
        val safeName = name.trim().ifBlank {
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        }
        val dest = File(getPhotoDir(), "$safeName.jpg")
        tempFile.copyTo(dest, overwrite = true)
        tempFile.delete()
        _photos.update { listOf(PhotoItem(dest, safeName)) + it }
        return "$safeName.jpg"
    }

    fun deletePhoto(photo: PhotoItem) {
        photo.file.delete()
        _photos.update { it.filter { p -> p.file.absolutePath != photo.file.absolutePath } }
    }

    fun uploadPhoto(photo: PhotoItem) {
        val config = _nasConfig.value
        if (!config.isConfigured) {
            showMessage("NAS 설정을 먼저 입력해주세요 (설정 탭)")
            return
        }
        setStatus(photo, UploadStatus.UPLOADING)
        viewModelScope.launch(Dispatchers.IO) {
            val result = NasUploader.upload(config, photo.file, "${photo.displayName}.jpg")
            val status = if (result.isSuccess) UploadStatus.SUCCESS else UploadStatus.FAILED
            setStatus(photo, status)
            showMessage(
                if (result.isSuccess) "✓ ${photo.displayName} 업로드 완료"
                else "✗ 업로드 실패: ${result.exceptionOrNull()?.message?.take(60)}"
            )
        }
    }

    fun uploadAllPending() {
        _photos.value
            .filter { it.uploadStatus != UploadStatus.SUCCESS && it.uploadStatus != UploadStatus.UPLOADING }
            .forEach { uploadPhoto(it) }
    }

    fun updateNasConfig(config: NasConfig) {
        NasConfigPrefs.save(context, config)
        _nasConfig.value = config
    }

    private val _shareListState = MutableStateFlow<ShareListState>(ShareListState.Idle)
    val shareListState: StateFlow<ShareListState> = _shareListState.asStateFlow()

    fun loadShareList(host: String, port: Int, username: String, password: String) {
        if (_shareListState.value is ShareListState.Loading) return
        viewModelScope.launch(Dispatchers.IO) {
            _shareListState.value = ShareListState.Loading
            val tempConfig = NasConfig(host = host.trim(), port = port, username = username, password = password)
            NasUploader.listShares(tempConfig)
                .onSuccess { _shareListState.value = ShareListState.Success(it) }
                .onFailure { _shareListState.value = ShareListState.Error(it.message ?: "연결 실패") }
        }
    }

    fun resetShareListState() {
        _shareListState.value = ShareListState.Idle
    }

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    private fun setStatus(photo: PhotoItem, status: UploadStatus) {
        _photos.update { list ->
            list.map { if (it.file.absolutePath == photo.file.absolutePath) it.copy(uploadStatus = status) else it }
        }
    }

    private fun showMessage(msg: String) {
        viewModelScope.launch {
            _snackMessage.value = msg
            delay(4000)
            _snackMessage.value = null
        }
    }
}
