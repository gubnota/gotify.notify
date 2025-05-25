package notify.gotify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


class NotificationHelper(private val context: Context) {
    companion object {
//        const val CHANNEL_ID = "notify_gotify_channel"
        const val CHANNEL_ID_FOREGROUND = "gotify_foreground"
        const val CHANNEL_ID_MESSAGES = "gotify_messages"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val foregroundChannel = NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                "Gotify Foreground Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Used for service status"
                setSound(null, null) // 👈 disables sound for this channel
                enableVibration(false) // disables vibration
                setShowBadge(false) // ✅ This disables badge count
            }

            val messageChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Gotify Message Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "User-facing messages"
                setShowBadge(true) // ✅ This one affects badge count
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(foregroundChannel)
            manager.createNotificationChannel(messageChannel)
        }
    }

    fun showNotification(
        title: String,
        message: String,
        foreground: Boolean = false,
        vibrate: Boolean = false,
        sound: Boolean = true,
        priority: Int = NotificationCompat.PRIORITY_HIGH,
        channel: String = CHANNEL_ID_MESSAGES,
        cancelPrevious: Boolean = false // cancel all previous notifications on this channel
    ): Notification? {

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (channel != CHANNEL_ID_FOREGROUND) {
                putExtra("notification_content", message)
            }
        }

        val requestCode = if (channel == CHANNEL_ID_FOREGROUND) 101 else 102

        val pendingIntent = PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(priority)

        if (!vibrate) builder.setVibrate(null)
        if (!sound) builder.setSound(null)

        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                val notification = builder.build()

                val manager = NotificationManagerCompat.from(context)

                // Clear all existing notifications before showing a new one
                if (cancelPrevious)
                manager.cancelAll()

                // Show the new one
                manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
                notification
            } else {
                Log.w("NotificationHelper", "Notification permission not granted")
                null
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Failed to post notification: ${e.message}")
            null
        }
    }

    fun buildNotification(
        title: String,
        message: String,
        vibrate: Boolean = false,
        sound: Boolean = false,
        priority: Int = NotificationCompat.PRIORITY_LOW,
        channel: String = CHANNEL_ID_MESSAGES
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_content", message)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(priority)

        if (!vibrate) builder.setVibrate(null)
        if (!sound) builder.setSound(null)

        return builder.build()
    }

}