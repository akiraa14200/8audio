package com.akiraa14200.eightaudio

import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicPlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var session: MediaSession? = null
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        session = MediaSession.Builder(this, player!!).build()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PLAY) {
            val url = intent.getStringExtra(URL).orEmpty()
            if (url.startsWith("https://")) {
                val meta = MediaMetadata.Builder().setTitle(intent.getStringExtra(TITLE)).setArtist(intent.getStringExtra(ARTIST))
                    .setArtworkUri(intent.getStringExtra(ART)?.let(Uri::parse)).build()
                player?.setMediaItem(MediaItem.Builder().setUri(url).setMediaMetadata(meta).build())
                player?.prepare()
                player?.play()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session
    override fun onDestroy() {
        player?.release()
        session?.release()
        player = null; session = null
        super.onDestroy()
    }
    companion object {
        const val ACTION_PLAY = "com.akiraa14200.eightaudio.PLAY"
        const val URL = "url"; const val TITLE = "title"; const val ARTIST = "artist"; const val ART = "art"
    }
}
