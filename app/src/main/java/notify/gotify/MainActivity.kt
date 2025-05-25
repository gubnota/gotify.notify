package notify.gotify

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var permissionHelper: PermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Init helpers
        notificationHelper = NotificationHelper(this)
        permissionHelper = PermissionHelper(this)

        // Request permission via helper
        permissionHelper.checkAndRequestPermission(
                android.Manifest.permission.POST_NOTIFICATIONS,
                requestCode = 1001
        )

        // Start foreground service
        if (!isServiceRunning(NotifyService::class.java)){
        startForegroundService()
        }

        // Random background color
        val runLabel: TextView = findViewById(R.id.run_label)
        val currentTime = System.currentTimeMillis()
        runLabel.text = (currentTime % 100).toString()

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

        // Handle push intent
        val notificationContainer = findViewById<LinearLayout>(R.id.notification_intent_container)
        val notificationTextView = findViewById<TextView>(R.id.notificationTextView)
        val message = intent.getStringExtra("notification_content")

        if (!message.isNullOrEmpty()) {
            notificationTextView.text = message
            notificationContainer.visibility = View.VISIBLE
            // cancel all prev notifications
            NotificationManagerCompat.from(this).cancelAll()
        } else {
            notificationContainer.visibility = View.INVISIBLE
        }
        // add onTap event handler to disappear notification intent container
        notificationContainer.setOnClickListener {
            notificationContainer.visibility = View.INVISIBLE
        }
        // Setup test button
        val button: Button = findViewById(R.id.test_notification_button)
        button.setOnClickListener {
            Handler(Looper.getMainLooper())
                    .postDelayed(
                            {
                                notificationHelper.showNotification(
                                        title = "Test Notification",
                                        message = "This is a test notification after 2s"
                                )
                            },
                            2000
                    )
        }
    }
    private fun startForegroundService() {
        val intent = Intent(this, NotifyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent) // API 26+
        } else {
            startService(intent) // Fallback for API 24–25
        }
    }
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == serviceClass.name
        }
    }
}
