package com.monux.input

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monux.MainService
import com.monux.ui.theme.MonuxTheme
import java.util.Locale

class RemoteInputActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.startForegroundService(this, Intent(this, MainService::class.java))
        setContent {
            val state by MainService.stateFlow().collectAsStateWithLifecycle()
            MonuxTheme(uiPreferences = state.uiPreferences) {
                RemoteInputScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RemoteInputScreen() {
    val context = LocalContext.current
    val state by MainService.stateFlow().collectAsStateWithLifecycle()
    val remoteEnabled = state.featureFlags.remoteInput
    val connected = state.connectionStatus == "已连接"
    val speechAvailable = remember(context) { SpeechRecognizer.isRecognitionAvailable(context) }
    var text by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable {
        mutableStateOf("输入文字或语音后发送到 Linux 当前焦点窗口")
    }
    var listening by rememberSaveable { mutableStateOf(false) }
    val speechRecognizer = remember(context, speechAvailable) {
        if (speechAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    val requestAudioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            listening = startVoiceInput(speechRecognizer)
            status = if (listening) "正在监听语音输入" else "语音识别暂不可用"
        } else {
            status = "未授予麦克风权限，无法启用语音输入"
        }
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                status = "请开始说话"
            }

            override fun onBeginningOfSpeech() {
                status = "正在录音…"
            }

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                status = "正在识别…"
            }

            override fun onError(error: Int) {
                listening = false
                status = "语音识别失败：${speechErrorLabel(error)}"
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                    .trim()
                if (spokenText.isBlank()) {
                    status = "未识别到有效文本"
                    return
                }
                text = spokenText
                val sent = MainService.sendRemoteInputText(spokenText, fromVoice = true)
                status = if (sent) {
                    "语音文本已发送"
                } else {
                    "语音识别完成，但当前未发送成功"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
        speechRecognizer?.setRecognitionListener(listener)
        onDispose {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
    }

    fun sendText() {
        val payload = text.trim()
        if (payload.isBlank()) {
            status = "请输入要发送的文本"
            return
        }
        val sent = MainService.sendRemoteInputText(payload)
        status = if (sent) {
            text = ""
            "文本已发送到 Linux"
        } else {
            "发送失败：请确认已连接并启用远程输入"
        }
    }

    fun sendKey(key: String) {
        val sent = MainService.sendRemoteInputKey(key)
        status = if (sent) {
            "按键 $key 已发送"
        } else {
            "按键发送失败：请确认已连接并启用远程输入"
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("远程输入") },
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = if (connected) "已连接到 ${state.deviceName}" else "当前未连接 Linux 端",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (remoteEnabled) "远程输入开关已启用" else "请先在主页打开“远程输入”能力",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    minLines = 6,
                    maxLines = 10,
                    label = { Text("输入要注入到 Linux 的文本") },
                    enabled = remoteEnabled,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { sendText() },
                        enabled = remoteEnabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("发送文本")
                    }
                    OutlinedButton(
                        onClick = {
                            if (!speechAvailable) {
                                status = "当前设备不支持 SpeechRecognizer"
                            } else if (!remoteEnabled) {
                                status = "请先启用远程输入功能"
                            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                listening = startVoiceInput(speechRecognizer)
                                status = if (listening) "正在监听语音输入" else "语音识别暂不可用"
                            } else {
                                requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        enabled = remoteEnabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (listening) "监听中" else "语音输入")
                    }
                }
                Text(
                    text = "常用按键",
                    style = MaterialTheme.typography.titleSmall,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Return", "BackSpace", "Tab", "Escape", "space").forEach { key ->
                        FilterChip(
                            selected = false,
                            onClick = { sendKey(key) },
                            enabled = remoteEnabled,
                            label = { Text(key) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun startVoiceInput(speechRecognizer: SpeechRecognizer?): Boolean {
    val recognizer = speechRecognizer ?: return false
    recognizer.cancel()
    recognizer.startListening(
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    )
    return true
}

private fun speechErrorLabel(code: Int): String = when (code) {
    SpeechRecognizer.ERROR_AUDIO -> "音频错误"
    SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "语言不支持"
    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "语言服务不可用"
    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
    SpeechRecognizer.ERROR_NO_MATCH -> "未匹配到结果"
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙碌"
    SpeechRecognizer.ERROR_SERVER -> "服务端错误"
    SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "服务已断开"
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
    else -> "未知错误($code)"
}
