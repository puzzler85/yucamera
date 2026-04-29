package com.whyj03.yucamera

import java.io.File

data class PhotoItem(
    val file: File,
    val displayName: String,
    val uploadStatus: UploadStatus = UploadStatus.PENDING
)

enum class UploadStatus {
    PENDING, UPLOADING, SUCCESS, FAILED
}
