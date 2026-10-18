package com.akiraa14200.eightaudio

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Track(val id:String,val title:String,val artist:String,val artwork:String,val streamUrl:String)

object SaavnSearch {
    fun search(query:String):List<Track> {
        val q=URLEncoder.encode(query,Charsets.UTF_8.name())
        val c=URL("https://saavn.dev/api/search/songs?query=$q&limit=25").openConnection() as HttpURLConnection
        c.connectTimeout=12000; c.readTimeout=12000
        return try {
            val root=JSONObject(c.inputStream.bufferedReader().use{it.readText()})
            val a=root.optJSONObject("data")?.optJSONArray("results") ?: return emptyList()
            buildList {
                for(i in 0 until a.length()) {
                    val o=a.optJSONObject(i) ?: continue
                    val d=o.optJSONArray("downloadUrl") ?: continue
                    val stream=(d.length()-1 downTo 0).mapNotNull{d.optJSONObject(it)?.optString("url")}.firstOrNull{it.startsWith("https://")} ?: continue
                    val imgs=o.optJSONArray("image")
                    val art=if(imgs!=null && imgs.length()>0) imgs.optJSONObject(imgs.length()-1)?.optString("url").orEmpty() else ""
                    val artist=o.optJSONObject("artists")?.optJSONArray("primary")?.optJSONObject(0)?.optString("name").orEmpty().ifBlank{"Unknown artist"}
                    add(Track(o.optString("id"),o.optString("name","Unknown track"),artist,art,stream))
                }
            }
        } finally { c.disconnect() }
    }
    fun lyrics(id: String): String {
        if (id.isBlank() || id.startsWith("local:")) return "Lyrics aren't available for this local file."
        val q = URLEncoder.encode(id, Charsets.UTF_8.name())
        val c = URL("https://saavn.dev/api/songs?id=$q").openConnection() as HttpURLConnection
        c.connectTimeout = 10000
        c.readTimeout = 10000
        return try {
            val root = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val data = root.optJSONObject("data")
            val song = if (data != null) data else root.optJSONArray("data")?.optJSONObject(0)
            val text = song?.optString("lyrics").orEmpty().trim()
            if (text.isNotBlank() && text != "null") text else "Lyrics weren't provided by the current music source for this track."
        } catch (_: Exception) {
            "Couldn't load lyrics right now. Please try again later."
        } finally { c.disconnect() }
    }
}
