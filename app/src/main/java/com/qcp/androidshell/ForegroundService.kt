package com.qcp.androidshell

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.qcp.androidshell.R.drawable.logo

class ForegroundService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createForegroundNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }


    @SuppressLint("UnspecifiedImmutableFlag", "ResourceAsColor")
    private fun createForegroundNotification(): Notification {
        //前台通知的id名，任意
        val channelId = "ForegroundService"
        //前台通知的名称，任意
        val channelName = "Service"
        //发送通知的等级，此处为高，根据业务情况而定
        val importance = NotificationManager.IMPORTANCE_MIN
        //判断Android版本，不同的Android版本请求不一样，以下代码为官方写法
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, channelName, importance)
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
//        }

        //点击通知时可进入的Activity
        val notificationIntent = Intent(Intent.ACTION_MAIN)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_MUTABLE
        )

        //最终创建的通知，以下代码为官方写法
        //注释部分是可扩展的参数，根据自己的功能需求添加
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("running")
            .setSmallIcon(logo)//通知显示的图标
//            .setColor(R.color.purple_200)
//            .setContentIntent(pendingIntent)//点击通知进入Activity
            .setPriority(NotificationCompat.PRIORITY_MIN)
//            .setTicker("通知的提示语")
            .build()
        //.setOngoing(true)
        //.setPriority(NotificationCompat.PRIORITY_MAX)
        //.setCategory(Notification.CATEGORY_TRANSPORT)
        //.setLargeIcon(Icon)
        //.setWhen(System.currentTimeMillis())
    }

}

