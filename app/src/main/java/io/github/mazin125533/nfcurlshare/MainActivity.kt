package io.github.mazin125533.nfcurlshare

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var scroll: ScrollView
    private lateinit var urlInput: EditText
    private lateinit var status: TextView
    private lateinit var nfcSettings: Button
    private lateinit var qr: ImageView
    private lateinit var qrCaption: TextView
    private lateinit var historyHeader: View
    private lateinit var historyHint: View
    private lateinit var historyList: LinearLayout
    private lateinit var history: UrlHistory
    private var nfcAdapter: NfcAdapter? = null
    private var qrUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scroll = findViewById(R.id.scroll)
        urlInput = findViewById(R.id.url_input)
        status = findViewById(R.id.status)
        nfcSettings = findViewById(R.id.nfc_settings)
        qr = findViewById(R.id.qr)
        qrCaption = findViewById(R.id.qr_caption)
        historyHeader = findViewById(R.id.history_header)
        historyHint = findViewById(R.id.history_hint)
        historyList = findViewById(R.id.history_list)
        history = UrlHistory(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        findViewById<Button>(R.id.apply).setOnClickListener { applyUrl(urlInput.text.toString(), title = null) }
        nfcSettings.setOnClickListener { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
        findViewById<Button>(R.id.history_clear).setOnClickListener { confirmClearHistory() }

        SharedUrl.url?.let {
            urlInput.setText(it)
            showQr(it)
        }
        refreshHistory()
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
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val url = SharedUrl.extractUrl(text)
        if (url == null) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_LONG).show()
            return
        }
        // Chrome はページタイトルを EXTRA_SUBJECT に入れる。無ければ本文の URL 以外の部分を使う
        val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim()?.takeIf { it.isNotEmpty() && it != url }
            ?: text?.replace(url, "")?.trim()?.takeIf { it.isNotEmpty() }
        urlInput.setText(url)
        applyUrl(url, title)
    }

    private fun applyUrl(text: String, title: String?) {
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
        history.add(url, title)
        refreshHistory()
    }

    private fun showQr(url: String) {
        if (url == qrUrl) return
        val sizePx = (360 * resources.displayMetrics.density).toInt()
        qr.setImageBitmap(QrCode.create(url, sizePx))
        qr.visibility = View.VISIBLE
        qrCaption.visibility = View.VISIBLE
        qrUrl = url
    }

    private fun refreshHistory() {
        val entries = history.load()
        val visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
        historyHeader.visibility = visibility
        historyHint.visibility = visibility
        historyList.removeAllViews()
        entries.forEach { historyList.addView(historyRow(it)) }
    }

    private fun historyRow(entry: UrlHistory.Entry): View {
        val pad = dp(12)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, pad, 0, pad)
            isClickable = true
            isFocusable = true
            val outValue = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
            contentDescription = entry.title ?: entry.url
        }
        if (entry.title != null) {
            row.addView(TextView(this).apply {
                text = entry.title
                textSize = 16f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
        }
        row.addView(TextView(this).apply {
            text = entry.url
            textSize = if (entry.title != null) 13f else 16f
            alpha = if (entry.title != null) 0.7f else 1f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MIDDLE
        })

        // タップ: この URL をもう一度共有する
        row.setOnClickListener {
            applyUrl(entry.url, entry.title)
            scroll.smoothScrollTo(0, 0)
        }
        // 長押し: 履歴から削除
        row.setOnLongClickListener {
            history.remove(entry.url)
            refreshHistory()
            Toast.makeText(this, R.string.history_removed, Toast.LENGTH_SHORT).show()
            true
        }
        return row
    }

    private fun confirmClearHistory() {
        AlertDialog.Builder(this)
            .setMessage(R.string.history_clear_confirm)
            .setPositiveButton(R.string.history_clear) { _, _ ->
                history.clear()
                refreshHistory()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

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
