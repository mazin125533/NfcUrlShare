package io.github.mazin125533.nfcurlshare

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** 生成した QR コードを読み取り直して、元の URL に戻ることを確かめる */
@RunWith(AndroidJUnit4::class)
class QrCodeTest {

    @Test
    fun qrDecodesToSameUrl() {
        for (url in listOf(
            "https://profile.mazinlab.jp/",
            "https://example.com/日本語/path?q=テスト&x=1",
            "https://example.com/" + "a".repeat(600),
        )) {
            assertEquals(url, decode(QrCode.create(url, 800)))
        }
    }

    private fun decode(bitmap: android.graphics.Bitmap): String {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }
}
