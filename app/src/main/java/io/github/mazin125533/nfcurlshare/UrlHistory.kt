package io.github.mazin125533.nfcurlshare

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 共有した URL の履歴（新しい順、最大 [MAX] 件）。端末内の SharedPreferences に保存する */
class UrlHistory(context: Context, prefsName: String = "history") {

    data class Entry(val url: String, val title: String?, val time: Long)

    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun load(): List<Entry> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Entry(
                    url = o.getString("url"),
                    title = o.optString("title").takeIf { it.isNotEmpty() },
                    time = o.optLong("time"),
                )
            }
        } catch (e: org.json.JSONException) {
            emptyList()   // 壊れていたら読み捨てる
        }
    }

    /** 先頭に追加する。同じ URL があれば先頭へ移動（タイトルが無ければ以前のものを引き継ぐ） */
    fun add(url: String, title: String?, now: Long = System.currentTimeMillis()) {
        val old = load()
        val keptTitle = title?.takeIf { it.isNotBlank() } ?: old.firstOrNull { it.url == url }?.title
        save(listOf(Entry(url, keptTitle, now)) + old.filter { it.url != url })
    }

    fun remove(url: String) = save(load().filter { it.url != url })

    fun clear() = prefs.edit().remove(KEY).apply()

    private fun save(entries: List<Entry>) {
        val array = JSONArray()
        entries.take(MAX).forEach {
            array.put(JSONObject().put("url", it.url).put("title", it.title ?: "").put("time", it.time))
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    companion object {
        const val MAX = 20
        private const val KEY = "entries"
    }
}
