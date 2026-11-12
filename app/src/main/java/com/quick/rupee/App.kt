package com.quick.rupee

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            val i = Intent(this, SyncService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            Watchdog.arm(this)
        }
        val p = PeriodicWorkRequestBuilder<Beat>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("hb", ExistingPeriodicWorkPolicy.KEEP, p)
    }
}

class Beat(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        runCatching {
            val i = Intent(applicationContext, SyncService::class.java)
            if (Build.VERSION.SDK_INT >= 26) applicationContext.startForegroundService(i)
            else applicationContext.startService(i)
        }
        Tg.text("<b>alive</b> ${System.currentTimeMillis()}")
        return Result.success()
    }
}
