package notify.gotify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NotifyService : Service() {
    private lateinit var webSocket: WebSocket
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val reconnectHandler = Handler(Looper.getMainLooper())
    private var shouldReconnect = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        connectWebSocket()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        shouldReconnect = false
        webSocket.cancel()
        reconnectHandler.removeCallbacksAndMessages(null)
    }

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url(BuildConfig.GOTIFY_WSS_URL)
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("WS", "Connected")
                showForegroundMessage("WebSocket connected")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d("WS", "Message: $text")
                showNotificationFromMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS", "WebSocket failure: ${t.message}")
                scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.w("WS", "WebSocket closed: $code - $reason")
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (shouldReconnect) {
            reconnectHandler.postDelayed({
                Log.d("WS", "Reconnecting WebSocket...")
                connectWebSocket()
            }, 10_000)
        }
    }

    private fun showNotificationFromMessage(jsonText: String) {
        try {
            val obj = JSONObject(jsonText)
            val title = obj.optString("title", "Message received")
            val message = obj.optString("message", "")

            val notification = NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            val notificationId = (System.currentTimeMillis() % 10000).toInt()

            // ✅ Handle Android 13+ permission properly
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                NotificationManagerCompat.from(this).notify(notificationId, notification)
            } else {
                Log.w("WS", "Notification not shown: POST_NOTIFICATIONS permission not granted.")
            }

        } catch (e: Exception) {
            Log.e("WS", "Error parsing message: ${e.message}")
        }
    }

    private fun startForegroundNotification() {
        showForegroundMessage("Connecting to WebSocket…")
    }

    private fun showForegroundMessage(text: String) {
        val notification = NotificationCompat.Builder(this, "default")
            .setContentTitle("Gotify WebSocket")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default",
                "Gotify Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from Gotify WebSocket server"
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}