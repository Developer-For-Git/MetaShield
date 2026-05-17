package com.metashield.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.metashield.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BatchForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "metashield_batch_channel"
        const val NOTIFICATION_ID = 1001
        const val DONE_NOTIFICATION_ID = 1002
        const val ACTION_START = "ACTION_START"
        const val ACTION_CANCEL = "ACTION_CANCEL"
        const val ACTION_UPDATE = "ACTION_UPDATE"
        const val ACTION_DONE = "ACTION_DONE"

        fun start(context: Context) {
            val intent = Intent(context, BatchForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BatchForegroundService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val notification = buildProgressNotification("Preparing batch...", 0, 1)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_CANCEL -> stopSelf()
            ACTION_UPDATE -> {
                val current = intent.getIntExtra("CURRENT", 0)
                val total = intent.getIntExtra("TOTAL", 0)
                val fileName = intent.getStringExtra("FILENAME") ?: ""
                updateProgress(current, total, fileName)
            }
            ACTION_DONE -> {
                val processed = intent.getIntExtra("PROCESSED", 0)
                val failed = intent.getIntExtra("FAILED", 0)
                showCompletionNotification(processed, failed)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun updateProgress(current: Int, total: Int, fileName: String) {
        val notification = buildProgressNotification("Processing: $fileName", current, total)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    fun showCompletionNotification(processed: Int, failed: Int) {
        stopForeground(STOP_FOREGROUND_DETACH)
        val text = if (failed == 0) "$processed files processed successfully"
                   else "$processed processed, $failed failed"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setContentTitle("MetaShield — Done!")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent())
            .build()
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(DONE_NOTIFICATION_ID, notification)
        stopSelf()
    }

    private fun buildProgressNotification(text: String, current: Int, total: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setContentTitle("MetaShield")
            .setContentText(text)
            .setProgress(total, current, total == 0)
            .setContentIntent(mainPendingIntent())
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun mainPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Batch Processing", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of batch metadata operations"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
