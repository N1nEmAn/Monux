package com.minux.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.minux.MainService

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.startForegroundService(this, Intent(this, MainService::class.java).apply {
            action = MainService.ACTION_SEND_FILE
            putExtra(MainService.EXTRA_FILE_URI, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        })
        finish()
    }
}
