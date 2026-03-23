package com.monux.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import com.monux.protocol.Protocol
import java.security.MessageDigest

class ClipboardSyncManager(
    private val clipboardManager: ClipboardManager,
    private val sendMessage: (String) -> Unit,
) {
    private var lastContentHash: String = ""

    fun syncToLinux(text: String) {
        val hash = contentHash(text)
        if (hash == lastContentHash) {
            return
        }
        lastContentHash = hash
        sendMessage(Protocol.clipboard(text, hash).toString())
    }

    fun applyFromLinux(text: String, hash: String) {
        if (hash == lastContentHash) {
            return
        }
        lastContentHash = hash
        clipboardManager.setPrimaryClip(ClipData.newPlainText("monux", text))
    }

    fun currentHash(): String = lastContentHash

    private fun contentHash(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
