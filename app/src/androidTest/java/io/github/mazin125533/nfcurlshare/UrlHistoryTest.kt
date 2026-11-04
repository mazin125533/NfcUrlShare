package io.github.mazin125533.nfcurlshare

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UrlHistoryTest {

    // アプリ本体の履歴を消さないよう、テスト専用の保存先を使う
    private lateinit var history: UrlHistory

    @Before
    fun setUp() {
        history = UrlHistory(InstrumentationRegistry.getInstrumentation().targetContext, "history_test")
        history.clear()
    }

    @After
    fun tearDown() = history.clear()

    @Test
    fun newestFirst() {
        history.add("https://a.example/", "A", now = 1)
        history.add("https://b.example/", "B", now = 2)
        assertEquals(listOf("https://b.example/", "https://a.example/"), history.load().map { it.url })
    }

    @Test
    fun reusingUrlMovesToTopWithoutDuplicateAndKeepsTitle() {
        history.add("https://a.example/", "ページA", now = 1)
        history.add("https://b.example/", null, now = 2)
        history.add("https://a.example/", null, now = 3)   // 手入力で再利用（タイトルなし）
        val entries = history.load()
        assertEquals(listOf("https://a.example/", "https://b.example/"), entries.map { it.url })
        assertEquals("ページA", entries[0].title)
        assertEquals(3L, entries[0].time)
    }

    @Test
    fun keepsAtMostMaxEntries() {
        repeat(UrlHistory.MAX + 5) { history.add("https://example.com/$it", null, now = it.toLong()) }
        val entries = history.load()
        assertEquals(UrlHistory.MAX, entries.size)
        assertEquals("https://example.com/${UrlHistory.MAX + 4}", entries.first().url)
    }

    @Test
    fun removeAndClear() {
        history.add("https://a.example/", null)
        history.add("https://b.example/", null)
        history.remove("https://a.example/")
        assertEquals(listOf("https://b.example/"), history.load().map { it.url })
        history.clear()
        assertTrue(history.load().isEmpty())
    }
}
