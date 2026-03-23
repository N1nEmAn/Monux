package com.monux.notify

import android.service.notification.StatusBarNotification
import org.json.JSONObject

object NotificationPayloadMapper {
    fun toJson(sbn: StatusBarNotification): JSONObject {
        val extras = sbn.notification.extras
        val title = extras?.getCharSequence("android.title")?.toString().orEmpty()
        val body = extras?.getCharSequence("android.text")?.toString().orEmpty()
        return JSONObject()
            .put("type", "notify")
            .put("payload", JSONObject()
                .put("app", sbn.packageName)
                .put("title", title)
                .put("body", body)
            )
    }
}
