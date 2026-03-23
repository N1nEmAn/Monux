package com.minux.notify

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.minux.MainService

class MinuxNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        Log.i(TAG, "notification listener connected")
        MainService.updateNotificationAccess(true)
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "notification listener disconnected")
        MainService.updateNotificationAccess(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.i(TAG, "notification posted package=${sbn.packageName}")
        MainService.forwardNotification(NotificationPayloadMapper.toJson(sbn).toString())
    }

    companion object {
        private const val TAG = "MinuxNotification"
    }
}
