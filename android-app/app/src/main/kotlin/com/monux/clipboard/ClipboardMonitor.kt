package com.monux.clipboard

import android.content.ClipboardManager

class ClipboardMonitor(
    private val clipboardManager: ClipboardManager,
    private val onTextChanged: (String) -> Unit,
) : ClipboardManager.OnPrimaryClipChangedListener {
    fun start() {
        clipboardManager.addPrimaryClipChangedListener(this)
    }

    fun stop() {
        clipboardManager.removePrimaryClipChangedListener(this)
    }

    override fun onPrimaryClipChanged() {
        val text = clipboardManager.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(null)
            ?.toString()
            .orEmpty()
        if (text.isNotBlank()) {
            onTextChanged(text)
        }
    }
}
