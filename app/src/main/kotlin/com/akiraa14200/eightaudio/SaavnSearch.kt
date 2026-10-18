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
}
