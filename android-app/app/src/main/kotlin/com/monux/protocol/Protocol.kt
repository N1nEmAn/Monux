package com.monux.protocol

import org.json.JSONObject

object Protocol {
    const val VERSION = 1
    const val TYPE_HELLO = "hello"
    const val TYPE_HELLO_ACK = "hello_ack"
    const val TYPE_PING = "ping"
    const val TYPE_PONG = "pong"
    const val TYPE_NOTIFY = "notify"
    const val TYPE_CLIPBOARD = "clipboard"
    const val TYPE_SMS = "sms"
    const val TYPE_SMS_SEND = "sms.send"
    const val TYPE_SMS_SENT = "sms.sent"
    const val TYPE_FILE_OFFER = "file.offer"
    const val TYPE_FILE_CHUNK = "file.chunk"
    const val TYPE_FILE_COMPLETE = "file.complete"
    const val TYPE_FILE_RECEIVED = "file.received"
    const val TYPE_FILE_ERROR = "file.error"
    const val TYPE_SCREEN_START = "screen.start"
    const val TYPE_SCREEN_STOP = "screen.stop"
    const val TYPE_SCREEN_STARTED = "screen.started"

    fun hello(deviceName: String, platform: String): JSONObject {
        return envelope(
            TYPE_HELLO,
            JSONObject()
                .put("version", VERSION)
                .put("device_name", deviceName)
                .put("platform", platform)
        )
    }

    fun helloAck(deviceName: String, platform: String): JSONObject {
        return envelope(
            TYPE_HELLO_ACK,
            JSONObject()
                .put("version", VERSION)
                .put("device_name", deviceName)
                .put("platform", platform)
        )
    }

    fun ping(): JSONObject {
        return envelope(TYPE_PING, JSONObject().put("version", VERSION))
    }

    fun pong(): JSONObject {
        return envelope(TYPE_PONG, JSONObject().put("version", VERSION))
    }

    fun notify(app: String, title: String, body: String): JSONObject {
        return envelope(
            TYPE_NOTIFY,
            JSONObject()
                .put("app", app)
                .put("title", title)
                .put("body", body)
        )
    }

    fun clipboard(text: String, contentHash: String): JSONObject {
        return envelope(
            TYPE_CLIPBOARD,
            JSONObject()
                .put("text", text)
                .put("content_hash", contentHash)
        )
    }

    fun sms(sender: String, body: String): JSONObject {
        return envelope(
            TYPE_SMS,
            JSONObject()
                .put("from", sender)
                .put("body", body)
        )
    }

    fun smsSent(address: String, success: Boolean, error: String = ""): JSONObject {
        return envelope(
            TYPE_SMS_SENT,
            JSONObject()
                .put("address", address)
                .put("success", success)
                .put("error", error)
        )
    }

    fun fileOffer(transferId: String, name: String, size: Long): JSONObject {
        return envelope(
            TYPE_FILE_OFFER,
            JSONObject()
                .put("transfer_id", transferId)
                .put("name", name)
                .put("size", size)
        )
    }

    fun fileChunk(transferId: String, index: Int, total: Int, dataBase64: String): JSONObject {
        return envelope(
            TYPE_FILE_CHUNK,
            JSONObject()
                .put("transfer_id", transferId)
                .put("index", index)
                .put("total", total)
                .put("data_base64", dataBase64)
        )
    }

    fun fileComplete(transferId: String, name: String): JSONObject {
        return envelope(
            TYPE_FILE_COMPLETE,
            JSONObject()
                .put("transfer_id", transferId)
                .put("name", name)
        )
    }

    fun screenStart(maxSize: Int, bitrate: String): JSONObject {
        return envelope(
            TYPE_SCREEN_START,
            JSONObject()
                .put("max_size", maxSize)
                .put("bitrate", bitrate)
        )
    }

    fun screenStop(): JSONObject {
        return envelope(TYPE_SCREEN_STOP, JSONObject())
    }

    private fun envelope(type: String, payload: JSONObject): JSONObject {
        return JSONObject()
            .put("type", type)
            .put("payload", payload)
    }
}
