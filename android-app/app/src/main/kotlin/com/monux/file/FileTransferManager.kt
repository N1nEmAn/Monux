package com.monux.file

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.monux.protocol.Protocol
import java.util.UUID
import java.util.Base64

class FileTransferManager(
    private val contentResolver: ContentResolver,
    private val sendMessage: (String) -> Unit,
    private val onProgress: (String, Float) -> Unit,
) {
    fun send(uri: Uri) {
        val transferId = UUID.randomUUID().toString()
        val name = resolveName(uri)
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        sendMessage(Protocol.fileOffer(transferId, name, bytes.size.toLong()).toString())
        val chunkSize = 64 * 1024
        val total = ((bytes.size + chunkSize - 1) / chunkSize).coerceAtLeast(1)
        bytes.asList().chunked(chunkSize).forEachIndexed { index, chunk ->
            val chunkBytes = chunk.toByteArray()
            val encoded = Base64.getEncoder().encodeToString(chunkBytes)
            sendMessage(Protocol.fileChunk(transferId, index, total, encoded).toString())
            onProgress(name, (index + 1).toFloat() / total.toFloat())
        }
        sendMessage(Protocol.fileComplete(transferId, name).toString())
    }

    private fun resolveName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return uri.lastPathSegment ?: "shared-file"
    }
}
