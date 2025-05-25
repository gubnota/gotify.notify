package notify.gotify

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NotifyService : Service() {

    private lateinit var webSocket: WebSocket
    private lateinit var notificationHelper: NotificationHelper
    private var isConnected = false

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS) // TCP-level ping
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val handler = Handler(Looper.getMainLooper())
    private val reconnectDelay = 10_000L
    private val appPingInterval = 30_000L
    private val pongTimeout = 40_000L

    private var lastPongTime: Long = 0
    private var shouldReconnect = true

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        connectWebSocket()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        showForegroundMessage("Connecting to WebSocket…")
        val notification = notificationHelper.buildNotification(
            title = "Gotify WebSocket",
            message = "Listening for messages...",
            vibrate = false,
            sound = false,
            priority = NotificationCompat.PRIORITY_MIN,
            channel = NotificationHelper.CHANNEL_ID_FOREGROUND
        )
        startForeground(1, notification) // 👈 Persistent, but silent
            return START_NOT_STICKY//START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        shouldReconnect = false
        handler.removeCallbacksAndMessages(null)
        webSocket.cancel()
    }

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url(BuildConfig.GOTIFY_WSS_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                if (!isConnected) {
                    isConnected = true
                    lastPongTime = System.currentTimeMillis()
                    Log.d("WS", "Connected")
//                    showForegroundMessage("WebSocket connected")
                    startAppPingLoop()
//                    startPongWatchdog()
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d("WS", "Message: $text")
                lastPongTime = System.currentTimeMillis()

                try {
                    val obj = JSONObject(text)
                    val type = obj.optString("type")

                    if (type == "pong") {
                        // Optional: log or ignore
                        return
                    }

                    handleIncomingMessage(text)

                } catch (e: Exception) {
                    Log.e("NotifyService", "Invalid message JSON: ${e.message}")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS", "WebSocket failure: ${t.message}")
                notificationHelper.showNotification(title="Network failure", message="WebSocket failed", cancelPrevious = true)
//                showForegroundMessage("WebSocket failed")
                handleDisconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.w("WS", "WebSocket closed: $code - $reason")
                handleDisconnect()
            }
        })
    }

    private fun handleDisconnect() {
        isConnected = false
        if (shouldReconnect) {
            handler.postDelayed({
                Log.d("WS", "Reconnecting WebSocket...")
                connectWebSocket()
            }, reconnectDelay)
        }
    }

    private fun startAppPingLoop() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (::webSocket.isInitialized) {
                    val sent = webSocket.send("""{"type":"ping"}""")
                    Log.d("WS", "App ping sent: $sent")
                }
                handler.postDelayed(this, appPingInterval)
            }
        }, appPingInterval)
    }

    private fun startPongWatchdog() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - lastPongTime
                if (elapsed > pongTimeout) {
                    Log.w("WS", "Pong timeout. Reconnecting...")
                    webSocket.cancel()
                    handleDisconnect()
                } else {
                    handler.postDelayed(this, pongTimeout)
                }
            }
        }, pongTimeout)
    }

    private fun handleIncomingMessage(jsonText: String) {
        try {
            val obj = JSONObject(jsonText)
            val title = obj.optString("title", "New Message")
            val message = obj.optString("message", "")
            showInteractiveNotification(title, message)
//            updateForegroundNotification(title=title, message = message)
        } catch (e: Exception) {
            Log.e("NotifyService", "Error parsing message: ${e.message}")
        }
    }

    private fun showInteractiveNotification(title: String, message: String) {
        notificationHelper.showNotification(title, message)
    }

    private fun showForegroundMessage(title:String = "Gotify WebSocket", text: String) {
        val notification = notificationHelper.showNotification(
            title = title,
            message = text,
            foreground = true,
            sound = false,
            vibrate = false,
            priority = NotificationCompat.PRIORITY_LOW
        )
//        if (notification != null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            startForeground(1, notification)
//        } else {
//            Log.w("NotifyService", "Could not show foreground notification")
//        }
    }

    fun updateForegroundNotification(title: String, message: String) {
        val notification = notificationHelper.buildNotification(
            title = title,
            message = message,
            vibrate = false,
            sound = false,
            priority = NotificationCompat.PRIORITY_MIN
        )
        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}