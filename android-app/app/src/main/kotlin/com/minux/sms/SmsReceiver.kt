package com.minux.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.minux.MainService

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        for (message in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            MainService.forwardSms(message.originatingAddress.orEmpty(), message.messageBody.orEmpty())
            Log.i(TAG, "sms received from=${message.originatingAddress}")
        }
    }

    companion object {
        private const val TAG = "MinuxSmsReceiver"
    }
}
