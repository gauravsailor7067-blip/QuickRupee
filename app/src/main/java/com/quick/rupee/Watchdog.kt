package com.quick.rupee

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class Watchdog : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        runCatching {
            val s = Intent(ctx, SyncService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(s) else ctx.startService(s)
        }
        arm(ctx)
    }

    companion object {
        fun arm(ctx: Context) {
            runCatching {
                val am = ctx.getSystemService(AlarmManager::class.java)
                val pi = PendingIntent.getBroadcast(ctx, 7,
                    Intent(ctx, Watchdog::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    System.currentTimeMillis() + 900_000L, 900_000L, pi)
            }
        }
    }
}
