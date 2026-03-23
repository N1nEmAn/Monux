package com.monux.sms

import android.telephony.SmsManager

object SmsReplyExecutor {
    fun send(address: String, body: String): Result<Unit> {
        return runCatching {
            SmsManager.getDefault().sendTextMessage(address, null, body, null, null)
        }
    }
}
