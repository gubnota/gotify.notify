package notify.gotify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestNotificationPermissionIfNeeded()
        createNotificationChannel()
        startForegroundService()
        // val request =
        //         Request.Builder().url(BuildConfig.GOTIFY_WSS_URL).build()
        // val client = OkHttpClient()
        // val ws = client.newWebSocket(request, NotifyListener())

        val label: TextView = findViewById(R.id.run_label)
        val runLabel: TextView = findViewById(R.id.run_label)
        val currentTime = System.currentTimeMillis()
        runLabel.setText((currentTime % 100).toString())
        val mainView: View = findViewById(R.id.main)
        val colors =
            listOf(
                0xFFE3F2FD.toInt(), // Light Blue
                0xFFFFF9C4.toInt(), // Light Yellow
                0xFFC8E6C9.toInt(), // Light Green
                0xFFE9B2FF.toInt(), // Light Purple
                0xFFFFB2E5.toInt() // Light Pink
            )
        val randomColor = colors.random()
        mainView.setBackgroundColor(randomColor)

        val button: Button = findViewById(R.id.test_notification_button)
        button.setOnClickListener {
            Handler(Looper.getMainLooper()).postDelayed({
                showTestNotification()
            }, 5000)
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, NotifyService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun showTestNotification() {
        val notification = NotificationCompat.Builder(this, "default")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Test Notification")
            .setContentText("This is a test notification after 5s")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this)
                .notify((System.currentTimeMillis() % 10000).toInt(), notification)
        } else {
            Log.w("MainActivity", "Notification permission not granted")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default",
                "Gotify Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from Gotify WebSocket"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }
}