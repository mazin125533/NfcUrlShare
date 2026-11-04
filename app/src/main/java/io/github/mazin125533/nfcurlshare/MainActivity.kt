package io.github.mazin125533.nfcurlshare

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var urlInput: EditText
    private lateinit var status: TextView
    private lateinit var nfcSettings: Button
    private lateinit var qr: ImageView
    private lateinit var qrCaption: TextView
    private var nfcAdapter: NfcAdapter? = null
    private var qrUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        urlInput = findViewById(R.id.url_input)
        status = findViewById(R.id.status)
        nfcSettings = findViewById(R.id.nfc_settings)
        qr = findViewById(R.id.qr)
        qrCaption = findViewById(R.id.qr_caption)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        findViewById<Button>(R.id.apply).setOnClickListener { applyUrl(urlInput.text.toString()) }
        nfcSettings.setOnClickListener { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }

        SharedUrl.url?.let {
            urlInput.setText(it)
            showQr(it)
        }
        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        SharedUrl.active = true
        SharedUrl.onSent = {
            runOnUiThread { Toast.makeText(this, R.string.status_sent, Toast.LENGTH_SHORT).show() }
        }
        // 同じ AID を使う他のアプリより、この画面を開いている間はこのアプリを優先する
        cardEmulation()?.setPreferredService(this, ComponentName(this, NdefHostApduService::class.java))
        updateStatus()
    }

    override fun onPause() {
        super.onPause()
        SharedUrl.active = false
        SharedUrl.onSent = null
        cardEmulation()?.unsetPreferredService(this)
    }

    private fun cardEmulation(): CardEmulation? {
        val adapter = nfcAdapter ?: return null
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) return null
        return CardEmulation.getInstance(adapter)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val url = SharedUrl.extractUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
        if (url == null) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_LONG).show()
            return
        }
        urlInput.setText(url)
        applyUrl(url)
    }

    private fun applyUrl(text: String) {
        if (!SharedUrl.set(text)) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_LONG).show()
            return
        }
        val url = SharedUrl.url ?: return
        urlInput.setText(url)
        urlInput.clearFocus()
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(urlInput.windowToken, 0)
        showQr(url)
        updateStatus()
    }

    private fun showQr(url: String) {
        if (url == qrUrl) return
        val sizePx = (360 * resources.displayMetrics.density).toInt()
        qr.setImageBitmap(QrCode.create(url, sizePx))
        qr.visibility = View.VISIBLE
        qrCaption.visibility = View.VISIBLE
        qrUrl = url
    }

    private fun updateStatus() {
        val adapter = nfcAdapter
        val hce = packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
        nfcSettings.visibility = View.GONE
        status.setText(
            when {
                adapter == null || !hce -> R.string.status_no_nfc
                !adapter.isEnabled -> {
                    nfcSettings.visibility = View.VISIBLE
                    R.string.status_nfc_off
                }
                SharedUrl.url == null -> R.string.status_no_url
                else -> R.string.status_ready
            }
        )
    }
}
