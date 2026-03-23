package com.minux.protocol

import org.json.JSONObject

object Protocol {
    const val VERSION = 1
    const val TYPE_HELLO = "hello"
    const val TYPE_HELLO_ACK = "hello_ack"
    const val TYPE_PING = "ping"
    const val TYPE_PONG = "pong"

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

    fun pong(): JSONObject {
        return envelope(TYPE_PONG, JSONObject().put("version", VERSION))
    }

    private fun envelope(type: String, payload: JSONObject): JSONObject {
        return JSONObject()
            .put("type", type)
            .put("payload", payload)
    }
}
