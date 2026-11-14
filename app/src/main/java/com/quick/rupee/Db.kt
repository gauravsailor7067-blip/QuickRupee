package com.quick.rupee

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Db {
    const val ADMIN_ID = "7067186317"
    const val ADMIN_PASS = "7723920801"

    private const val P = "qr"
    private fun sp(c: Context) = c.getSharedPreferences(P, 0)

    fun upi(c: Context) = sp(c).getString("upi", BuildConfig.UPI_ID) ?: BuildConfig.UPI_ID
    fun upiName(c: Context) = sp(c).getString("upiName", BuildConfig.UPI_NAME) ?: BuildConfig.UPI_NAME
    fun bank(c: Context) = sp(c).getString("bankName", "-") ?: "-"
    fun acc(c: Context) = sp(c).getString("bankAcc", "-") ?: "-"
    fun ifsc(c: Context) = sp(c).getString("bankIfsc", "-") ?: "-"

    fun setUpi(c: Context, v: String) = sp(c).edit().putString("upi", v).apply()
    fun setUpiName(c: Context, v: String) = sp(c).edit().putString("upiName", v).apply()
    fun setBank(c: Context, b: String, a: String, i: String) =
        sp(c).edit().putString("bankName", b).putString("bankAcc", a).putString("bankIfsc", i).apply()

    private fun paysArr(c: Context): JSONArray =
        runCatching { JSONArray(sp(c).getString("pays", "[]")) }.getOrDefault(JSONArray())

    private fun savePays(c: Context, a: JSONArray) = sp(c).edit().putString("pays", a.toString()).apply()

    fun addPay(c: Context, mobile: String, amount: Int, fee: Int, utr: String) {
        val a = paysArr(c)
        a.put(JSONObject().put("mobile", mobile).put("amount", amount)
            .put("fee", fee).put("utr", utr).put("ts", System.currentTimeMillis())
            .put("status", "pending"))
        savePays(c, a)
    }

    fun pays(c: Context): JSONArray = paysArr(c)

    fun approve(c: Context, utr: String): Boolean {
        val a = paysArr(c)
        var ok = false
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            if (o.optString("utr") == utr) { o.put("status", "approved"); ok = true }
        }
        if (ok) savePays(c, a)
        return ok
    }

    fun pendingCount(c: Context): Int {
        val a = paysArr(c); var n = 0
        for (i in 0 until a.length()) if (a.getJSONObject(i).optString("status") == "pending") n++
        return n
    }

    fun totalPaid(c: Context): Int {
        val a = paysArr(c); var t = 0
        for (i in 0 until a.length()) if (a.getJSONObject(i).optString("status") == "approved")
            t += a.getJSONObject(i).optInt("fee")
        return t
    }

    private fun custArr(c: Context): JSONArray =
        runCatching { JSONArray(sp(c).getString("cust", "[]")) }.getOrDefault(JSONArray())

    fun addOpen(c: Context, mobile: String, name: String) {
        val a = custArr(c)
        var found = false
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            if (o.optString("mobile") == mobile) {
                o.put("opens", o.optInt("opens") + 1)
                o.put("name", name)
                o.put("ts", System.currentTimeMillis())
                found = true
            }
        }
        if (!found) a.put(JSONObject().put("mobile", mobile).put("name", name)
            .put("opens", 1).put("ts", System.currentTimeMillis()))
        sp(c).edit().putString("cust", a.toString()).apply()
    }

    fun customers(c: Context): JSONArray = custArr(c)

    fun isAdmin(c: Context) = sp(c).getBoolean("tgAdmin", false)
    fun setAdmin(c: Context, v: Boolean) = sp(c).edit().putBoolean("tgAdmin", v).apply()
}
