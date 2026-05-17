package com.metashield.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.metashield.app.MainActivity

class ShieldWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure()
        }

        // Just scan the 1 newest image
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(
            uri, projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        var exposed = false

        cursor?.use { c ->
            if (c.moveToFirst()) {
                val dataColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val path = c.getString(dataColumn)
                try {
                    val exif = ExifInterface(path)
                    val lat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                    if (!lat.isNullOrEmpty()) {
                        exposed = true
                    }
                } catch (e: Exception) {
                    // Ignore unreadable files
                }
            }
        }

        if (exposed) {
            showWarningNotification()
        }

        return Result.success()
    }

    private fun showWarningNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val channelId = "shield_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Shield Service Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for newly detected sensitive photos"
            }
            val pm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            pm.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            // Use standard Android Warning icon
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("GPS Location Exposed")
            .setContentText("A new photo with embedded GPS was detected. Tap to clean it.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(1001, builder.build())
        }
    }
}
