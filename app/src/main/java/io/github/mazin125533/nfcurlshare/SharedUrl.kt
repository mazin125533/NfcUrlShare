package io.github.mazin125533.nfcurlshare

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord

/**
 * いま共有する URL と、HCE サービスが返す NDEF ファイルの中身を保持する。
 * Activity（UI スレッド）と HostApduService（バインダースレッド）の両方から触るので @Volatile。
 */
object SharedUrl {
    /** NDEF ファイルの最大サイズ（Capability Container に書く値）。URL の長さもこれで制限される */
    const val MAX_NDEF_FILE_SIZE = 0x0800

    /** 画面を開いている間だけ true。false のときはタグとして応答しない */
    @Volatile
    var active = false

    @Volatile
    var url: String? = null
        private set

    /** NLEN(2byte) + NDEF メッセージ */
    @Volatile
    var ndefFile: ByteArray? = null
        private set

    /** 送信完了（相手が NDEF を最後まで読んだ）を UI に知らせる */
    @Volatile
    var onSent: (() -> Unit)? = null

    /** @return 設定できたら true（URL として不正、または長すぎる場合は false） */
    fun set(newUrl: String): Boolean {
        val uri = Uri.parse(newUrl.trim())
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrEmpty()) return false
        val message = NdefMessage(NdefRecord.createUri(uri)).toByteArray()
        if (message.size + 2 > MAX_NDEF_FILE_SIZE) return false
        val file = ByteArray(message.size + 2)
        file[0] = (message.size shr 8).toByte()
        file[1] = message.size.toByte()
        message.copyInto(file, 2)
        url = uri.toString()
        ndefFile = file
        return true
    }

    /** 共有テキスト（"ページタイトル https://…" のような形もある）から最初の URL を取り出す */
    fun extractUrl(text: String?): String? {
        if (text == null) return null
        return Regex("""https?://\S+""").find(text)?.value
    }
}
