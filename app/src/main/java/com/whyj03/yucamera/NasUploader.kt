package com.whyj03.yucamera

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import java.io.File
import java.util.EnumSet

object NasUploader {

    fun listShares(config: NasConfig): Result<List<String>> {
        return runCatching {
            SMBClient().connect(config.host, config.port).use { connection ->
                val auth = if (config.username.isBlank()) {
                    AuthenticationContext.anonymous()
                } else {
                    AuthenticationContext(config.username, config.password.toCharArray(), null)
                }
                val session = connection.authenticate(auth)
                listOf(
                    "homes", "home", "public", "share", "shares", "data", "files",
                    "media", "photo", "photos", "pictures", "picture", "camera",
                    "backup", "backups", "download", "downloads", "upload", "uploads",
                    "video", "videos", "music", "document", "documents", "nas", "storage"
                ).filter { name ->
                    runCatching { session.connectShare(name).also { it.close() } }.isSuccess
                }
            }
        }
    }

    fun upload(config: NasConfig, localFile: File, remoteFileName: String): Result<Unit> {
        return runCatching {
            SMBClient().connect(config.host, config.port).use { connection ->
                val auth = if (config.username.isBlank()) {
                    AuthenticationContext.anonymous()
                } else {
                    AuthenticationContext(config.username, config.password.toCharArray(), null)
                }
                val session = connection.authenticate(auth)
                val share = session.connectShare(config.shareName) as DiskShare

                ensureDirectoryExists(share, config.remotePath)
                val remotePath = joinPath(config.remotePath, remoteFileName)

                share.openFile(
                    remotePath,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    null
                ).use { smbFile ->
                    smbFile.outputStream.use { out ->
                        localFile.inputStream().use { it.copyTo(out) }
                    }
                }
            }
        }
    }

    private fun joinPath(base: String, name: String): String {
        val trimmed = base.trim('/', '\\')
        return if (trimmed.isEmpty()) name else "$trimmed\\$name"
    }

    private fun ensureDirectoryExists(share: DiskShare, remotePath: String) {
        val path = remotePath.trim('/', '\\')
        if (path.isEmpty()) return
        var current = ""
        for (part in path.split("/", "\\").filter { it.isNotBlank() }) {
            current = if (current.isEmpty()) part else "$current\\$part"
            if (!share.folderExists(current)) share.mkdir(current)
        }
    }
}
