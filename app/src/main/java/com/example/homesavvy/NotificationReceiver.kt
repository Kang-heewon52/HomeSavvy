package com.example.homesavvy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {
    private val CHANNEL_ID = "home_savvy_channel"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e("NotifReceiver", "Context or Intent is null.")
            return
        }

        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)

        val title = intent.getStringExtra("NOTIFICATION_TITLE") ?: "알림"
        val message = intent.getStringExtra("NOTIFICATION_MESSAGE") ?: "교체 시기가 도래했습니다."

        Log.d("NotifReceiver", "알림 수신! ID: $notificationId, 메시지: $message")

        createNotificationChannel(context)

        showNotification(context, title, message, notificationId)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "소모품 교체 알림"
            val descriptionText = "소모품 교체 주기가 되었을 때 알림을 보냅니다."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tool_search)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}