package com.whyj03.yucamera

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val _photoPrefix = MutableStateFlow(PrefixPrefs.loadPrefix(context))
    val photoPrefix: StateFlow<String> = _photoPrefix.asStateFlow()

    private val _prefixCounter = MutableStateFlow(PrefixPrefs.loadCounter(context))
    val prefixCounter: StateFlow<Int> = _prefixCounter.asStateFlow()

    fun updatePhotoPrefix(prefix: String) {
        val trimmed = prefix.trim()
        if (trimmed != _photoPrefix.value) {
            _prefixCounter.value = 1
            PrefixPrefs.saveCounter(context, 1)
        }
        _photoPrefix.value = trimmed
        PrefixPrefs.savePrefix(context, trimmed)
    }

    init {
        loadExistingPhotos()
    }

    private fun loadExistingPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val projection = arrayOf(
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_MODIFIED
                )
                val selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ?"
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, arrayOf("Pictures/yucamera/"),
                    "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val list = mutableListOf<PhotoItem>()
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(dataCol)
                        val displayName = cursor.getString(nameCol).removeSuffix(".jpg")
                        list.add(PhotoItem(File(path), displayName))
                    }
                    _photos.value = list
                }
            } else {
                val files = getPhotoDir().listFiles { f -> f.extension.lowercase() == "jpg" } ?: return@launch
                _photos.value = files.sortedByDescending { it.lastModified() }
                    .map { PhotoItem(it, it.nameWithoutExtension) }
            }
        }
    }

    fun getPhotoDir(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "yucamera")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun onPhotoCaptured(tempFile: File, name: String) {
        val prefix = _photoPrefix.value
        val safeName = when {
            name.isNotBlank() -> name.trim()
            prefix.isNotBlank() -> {
                val counter = _prefixCounter.value
                val generated = "${prefix}_${counter.toString().padStart(4, '0')}"
                _prefixCounter.value = counter + 1
                PrefixPrefs.saveCounter(context, counter + 1)
                generated
            }
            else -> SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val destFile: File
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "$safeName.jpg")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/yucamera/")
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
                    ) ?: throw IllegalStateException("MediaStore 저장 실패")
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        tempFile.inputStream().use { it.copyTo(os) }
                    }
                    destFile = context.contentResolver.query(
                        uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null
                    )?.use { cursor ->
                        cursor.moveToFirst()
                        File(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)))
                    } ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "yucamera/$safeName.jpg")
                } else {
                    destFile = File(getPhotoDir(), "$safeName.jpg")
                    tempFile.copyTo(destFile, overwrite = true)
                }
                tempFile.delete()
                _photos.update { listOf(PhotoItem(destFile, safeName)) + it }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "$safeName.jpg 저장 완료", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deletePhoto(photo: PhotoItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Images.Media.DATA} = ?",
                arrayOf(photo.file.absolutePath)
            )
        } else {
            photo.file.delete()
        }
        _photos.update { it.filter { p -> p.file.absolutePath != photo.file.absolutePath } }
    }

    fun deleteAllPhotos() {
        val current = _photos.value
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            current.forEach { photo ->
                context.contentResolver.delete(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    "${MediaStore.Images.Media.DATA} = ?",
                    arrayOf(photo.file.absolutePath)
                )
            }
        } else {
            current.forEach { it.file.delete() }
        }
        _photos.value = emptyList()
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
