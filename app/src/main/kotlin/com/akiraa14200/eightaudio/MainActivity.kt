package com.akiraa14200.eightaudio

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val bg = Color(0xFF090B0D)
private val card = Color(0xFF171B20)
private val mint = Color(0xFFB8F7D4)
private val muted = Color(0xFF9AA4AE)

class MainActivity : ComponentActivity() {
    private lateinit var store: AudioPreferences
    private var localTrackCallback: ((Track) -> Unit)? = null
    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: "Local audio"
            val track = Track("local:${uri}", name.substringBeforeLast('.', name), "On this device", "", uri.toString())
            localTrackCallback?.invoke(track)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = AudioPreferences(this)
        setContent {
            AudioApp(
                store = store,
                pickLocal = { callback -> localTrackCallback = callback; pickAudio.launch(arrayOf("audio/*")) },
                play = { t -> startTrack(t) },
                download = { t -> downloadTrack(t) },
                applyRefresh = { enabled -> setHighRefresh(enabled) }
            )
        }
    }

    private fun startTrack(t: Track) {
        store.saveRecent(t)
        val i = Intent(this, MusicPlaybackService::class.java).setAction(MusicPlaybackService.ACTION_PLAY)
            .putExtra(MusicPlaybackService.URL, t.streamUrl)
            .putExtra(MusicPlaybackService.TITLE, t.title)
            .putExtra(MusicPlaybackService.ARTIST, t.artist)
            .putExtra(MusicPlaybackService.ART, t.artwork)
        ContextCompat.startForegroundService(this, i)
    }

    private fun downloadTrack(t: Track) {
        if (!t.streamUrl.startsWith("https://")) return
        runCatching {
            val request = DownloadManager.Request(Uri.parse(t.streamUrl))
                .setTitle(t.title).setDescription("8 Audio • ${t.artist}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "8 Audio/${safeName(t.title)}.mp3")
                .setAllowedOverMetered(true).setAllowedOverRoaming(false)
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        }
    }

    private fun safeName(value: String) = value.replace(Regex("[^a-zA-Z0-9 _-]"), "").take(80).ifBlank { "track" }

    private fun setHighRefresh(enabled: Boolean) {
        runCatching {
            val modes = display?.supportedModes.orEmpty()
            val target = if (enabled) modes.maxByOrNull { it.refreshRate } else modes.minByOrNull { it.refreshRate }
            if (target != null) {
                val attrs = window.attributes
                attrs.preferredDisplayModeId = target.modeId
                window.attributes = attrs
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioApp(
    store: AudioPreferences,
    pickLocal: ((Track) -> Unit) -> Unit,
    play: (Track) -> Unit,
    download: (Track) -> Unit,
    applyRefresh: (Boolean) -> Unit
) {
    var tab by remember { mutableStateOf("Home") }
    var query by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf(emptyList<Track>()) }
    var current by remember { mutableStateOf<Track?>(null) }
    var queue by remember { mutableStateOf(emptyList<Track>()) }
    var favorites by remember { mutableStateOf(store.favoriteIds()) }
    var recents by remember { mutableStateOf(store.recentTracks()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }
    var showLyrics by remember { mutableStateOf(false) }
    var pureBlack by remember { mutableStateOf(store.getBoolean("pure_black")) }
    var dynamicTheme by remember { mutableStateOf(store.getBoolean("dynamic_theme", true)) }
    var blurDisabled by remember { mutableStateOf(store.getBoolean("disable_blur")) }
    var animationsDisabled by remember { mutableStateOf(store.getBoolean("disable_animations")) }
    var highRefresh by remember { mutableStateOf(store.getBoolean("high_refresh")) }
    val scope = rememberCoroutineScope()
    val background = if (pureBlack) Color.Black else bg
    val surface = if (pureBlack) Color(0xFF080808) else card

    fun playTrack(t: Track) {
        current = t
        store.saveRecent(t)
        recents = store.recentTracks()
        play(t)
    }
    fun search(q: String) {
        if (q.isBlank()) return
        query = q; busy = true; error = ""
        scope.launch {
            try {
                tracks = withContext(Dispatchers.IO) { SaavnSearch.search(q) }
                if (tracks.isEmpty()) error = "No playable results. Try another search."
            } catch (_: Exception) { error = "Couldn't load music. Check your connection." }
            finally { busy = false }
        }
    }
    fun toggleFavorite(t: Track) {
        store.toggleFavorite(t.id)
        favorites = store.favoriteIds()
    }
    fun loadLyrics(t: Track) {
        showLyrics = true; lyrics = "Looking for lyrics…"
        scope.launch {
            lyrics = withContext(Dispatchers.IO) { SaavnSearch.lyrics(t.id) }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = mint, background = background, surface = surface, onSurface = Color.White)) {
        Scaffold(containerColor = background, bottomBar = {
            Column {
                current?.let { t ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)
                            .clip(RoundedCornerShape(18.dp)).background(if (pureBlack) Color(0xFF101010) else Color(0xFF20302A))
                            .clickable { tab = "Player" }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(t.artwork.takeIf { it.isNotBlank() }, null, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(t.artist, color = muted, fontSize = 12.sp)
                        }
                        IconButton(onClick = { playTrack(t) }) { Icon(Icons.Rounded.PlayArrow, "Play", tint = mint) }
                        IconButton(onClick = { queue = queue + t; tab = "Queue" }) { Icon(Icons.Rounded.QueueMusic, "Queue", tint = mint) }
                    }
                }
                NavigationBar(containerColor = surface) {
                    listOf("Home", "Search", "Library", "Queue", "Settings").forEach { v ->
                        NavigationBarItem(
                            selected = tab == v, onClick = { tab = v },
                            icon = { Icon(when (v) {
                                "Search" -> Icons.Rounded.Search; "Library" -> Icons.Rounded.LibraryMusic
                                "Queue" -> Icons.Rounded.QueueMusic; "Settings" -> Icons.Rounded.Settings
                                else -> Icons.Rounded.Home
                            }, v) },
                            label = { Text(v, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = mint, selectedTextColor = mint,
                                indicatorColor = Color(0xFF26362F), unselectedIconColor = muted, unselectedTextColor = muted)
                        )
                    }
                }
            }
        }) { pad ->
            LazyColumn(
                Modifier.fillMaxSize().then(if (blurDisabled || pureBlack) Modifier.background(background) else Modifier.background(Brush.verticalGradient(listOf(Color(0xFF12201B), background, background)))).padding(pad),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("8 AUDIO", color = mint, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, fontSize = 12.sp)
                            Text(when (tab) {
                                "Search" -> "Find your sound"; "Library" -> "Your music space"; "Queue" -> "Up next"
                                "Settings" -> "Make it yours"; "Player" -> "Now playing"; else -> "Good music. No limits."
                            }, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Rounded.GraphicEq, null, tint = mint, modifier = Modifier.size(30.dp))
                    }
                }
                when (tab) {
                    "Home" -> {
                        item {
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                                .then(if (blurDisabled) Modifier.background(surface) else Modifier.background(Brush.linearGradient(listOf(Color(0xFF27483D), Color(0xFF18211E), Color(0xFF25263A)))))
                                .padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("YOUR SOUND,\nYOUR SPACE", fontSize = 27.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Discover tracks, save favourites and keep your music close.", color = Color(0xFFD2DDD7))
                                Button(onClick = { tab = "Search" }, colors = ButtonDefaults.buttonColors(containerColor = mint, contentColor = Color.Black)) {
                                    Icon(Icons.Rounded.Search, null); Text("  Explore music")
                                }
                            }
                        }
                        item { SectionTitle("Recently played") }
                        if (recents.isEmpty()) item { EmptyPanel("Your listening history will appear here.") }
                        items(recents.take(8), key = { "recent:" + it.id }) { t -> TrackItem(t, favorites.contains(t.id), { playTrack(t) }, { toggleFavorite(t) }, { queue = queue + t }, { download(t) }) }
                        item { SectionTitle("Quick picks") }
                        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Arijit Singh", "Trending Hindi", "Lo-fi").forEach { v ->
                                Surface(Modifier.weight(1f).clickable { tab = "Search"; search(v) }, color = surface, shape = RoundedCornerShape(15.dp)) {
                                    Text(v, Modifier.padding(11.dp), fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        } }
                    }
                    "Search" -> {
                        item {
                            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Songs, artists, albums…", color = muted) }, leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                trailingIcon = { IconButton(onClick = { search(query) }) { Icon(Icons.Rounded.ArrowForward, "Search", tint = mint) } },
                                singleLine = true, shape = RoundedCornerShape(17.dp), keyboardActions = KeyboardActions(onSearch = { search(query) }),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = mint, unfocusedBorderColor = Color(0xFF343B43), focusedContainerColor = surface, unfocusedContainerColor = surface))
                        }
                        item { Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("Hindi hits", "Lo-fi", "Workout").forEach { v ->
                                SuggestionChip(onClick = { search(when (v) { "Lo-fi" -> "lofi"; "Workout" -> "workout songs"; else -> "latest hindi hits" }) }, label = { Text(v) })
                            }
                        } }
                        if (busy) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = mint) } }
                        if (error.isNotBlank()) item { Text(error, color = Color(0xFFFFB4AB)) }
                        if (tracks.isNotEmpty()) item { SectionTitle("Results") }
                        items(tracks, key = { "result:" + it.id }) { t -> TrackItem(t, favorites.contains(t.id), { playTrack(t) }, { toggleFavorite(t) }, { queue = queue + t }, { download(t) }) }
                    }
                    "Library" -> {
                        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { pickLocal { t -> playTrack(t); recents = store.recentTracks() } }, colors = ButtonDefaults.buttonColors(containerColor = mint, contentColor = Color.Black)) {
                                Icon(Icons.Rounded.FolderOpen, null); Text("  Open local audio")
                            }
                            OutlinedButton(onClick = { tab = "Search" }) { Text("Find music") }
                        } }
                        item { SectionTitle("Favourites") }
                        val favTracks = recents.filter { favorites.contains(it.id) }
                        if (favTracks.isEmpty()) item { EmptyPanel("Tap the heart on a track to save it here.") }
                        items(favTracks, key = { "fav:" + it.id }) { t -> TrackItem(t, true, { playTrack(t) }, { toggleFavorite(t) }, { queue = queue + t }, { download(t) }) }
                        item { SectionTitle("Recently played") }
                        items(recents.take(30), key = { "library-recent:" + it.id }) { t -> TrackItem(t, favorites.contains(t.id), { playTrack(t) }, { toggleFavorite(t) }, { queue = queue + t }, { download(t) }) }
                    }
                    "Queue" -> {
                        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${queue.size} tracks queued", color = muted)
                            TextButton(onClick = { queue = emptyList() }) { Text("Clear queue", color = mint) }
                        } }
                        if (queue.isEmpty()) item { EmptyPanel("Add tracks from Search or Library using the queue icon.") }
                        items(queue, key = { "queue:" + it.id + ":" + queue.indexOf(it) }) { t ->
                            TrackItem(t, favorites.contains(t.id), { playTrack(t) }, { toggleFavorite(t) }, { queue = queue + t }, { download(t) })
                        }
                    }
                    "Player" -> {
                        current?.let { t ->
                            item { AsyncImage(t.artwork.takeIf { it.isNotBlank() }, t.title, Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(28.dp))) }
                            item { Text(t.title, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(t.artist, color = muted) }
                            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { toggleFavorite(t) }) { Icon(if (favorites.contains(t.id)) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite", tint = mint) }
                                IconButton(onClick = { queue = queue + t; tab = "Queue" }) { Icon(Icons.Rounded.QueueMusic, "Queue", tint = mint, modifier = Modifier.size(28.dp)) }
                                FilledIconButton(onClick = { playTrack(t) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = mint, contentColor = Color.Black)) { Icon(Icons.Rounded.PlayArrow, "Play", modifier = Modifier.size(34.dp)) }
                                IconButton(onClick = { loadLyrics(t) }) { Icon(Icons.Rounded.Notes, "Lyrics", tint = mint, modifier = Modifier.size(28.dp)) }
                                IconButton(onClick = { download(t) }) { Icon(Icons.Rounded.Download, "Download", tint = mint) }
                            } }
                            item { Button(onClick = { loadLyrics(t) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = surface)) { Icon(Icons.Rounded.Notes, null); Text("  Show lyrics") } }
                            if (showLyrics) item { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(surface).padding(16.dp)) {
                                Text("Lyrics", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(lyrics, color = muted, modifier = Modifier.padding(top = 10.dp))
                            } }
                        } ?: item { EmptyPanel("Choose a track to open the full player.") }
                    }
                    "Settings" -> {
                        item { SectionTitle("Appearance") }
                        item { SettingRow("Dynamic theme", "Use device wallpaper colours when supported", dynamicTheme) { dynamicTheme = it; store.setBoolean("dynamic_theme", it) } }
                        item { SettingRow("Pure black", "AMOLED-friendly true black background", pureBlack) { pureBlack = it; store.setBoolean("pure_black", it) } }
                        item { SettingRow("Disable blur effects", "Use solid surfaces throughout the app", blurDisabled) { blurDisabled = it; store.setBoolean("disable_blur", it) } }
                        item { SettingRow("Disable animations", "Reduce most UI motion for lower-end devices", animationsDisabled) { animationsDisabled = it; store.setBoolean("disable_animations", it) } }
                        item { SettingRow("Force high refresh rate", "Request the highest display mode available; Android may override it", highRefresh) {
                            highRefresh = it; store.setBoolean("high_refresh", it); applyRefresh(it)
                        } }
                        item { SectionTitle("Music & storage") }
                        item { SettingAction("Local music", "Choose an audio file using Android's file picker", "Open") { pickLocal { t -> playTrack(t) } } }
                        item { SettingAction("Playback cache", "Online streaming cache is managed by Android's media player. Downloads appear in Music/8 Audio.", "Info") { } }
                        item { SettingAction("Lyrics", "Lyrics are shown only when the source exposes them", "Test") { current?.let { loadLyrics(it); tab = "Player" } } }
                        item { Text("8 Audio • Native Android • Settings are saved on this device", color = muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp)) }
                    }
                }
            }
        }
    }
}

@Composable private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
}

@Composable private fun EmptyPanel(message: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(card).padding(18.dp)) {
        Icon(Icons.Rounded.MusicNote, null, tint = mint, modifier = Modifier.size(26.dp))
        Text(message, color = muted, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable private fun SettingRow(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(card).padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = muted, fontSize = 12.sp)
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable private fun SettingAction(title: String, subtitle: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(card).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = muted, fontSize = 12.sp) }
        TextButton(onClick = onClick) { Text(action, color = mint) }
    }
}

@Composable private fun TrackItem(t: Track, favorite: Boolean, onPlay: () -> Unit, onFavorite: () -> Unit, onQueue: () -> Unit, onDownload: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(card).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(t.artwork.takeIf { it.isNotBlank() }, t.title, Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).clickable { onPlay() })
        Column(Modifier.weight(1f).clickable { onPlay() }.padding(horizontal = 11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.artist, color = muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFavorite) { Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favourite", tint = if (favorite) mint else muted) }
        Box {
            var expanded by remember { mutableStateOf(false) }
            IconButton(onClick = { expanded = true }) { Icon(Icons.Rounded.MoreVert, "More options", tint = muted) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Add to queue") }, onClick = { expanded = false; onQueue() }, leadingIcon = { Icon(Icons.Rounded.QueueMusic, null) })
                DropdownMenuItem(text = { Text("Download") }, onClick = { expanded = false; onDownload() }, leadingIcon = { Icon(Icons.Rounded.Download, null) })
                DropdownMenuItem(text = { Text("Play now") }, onClick = { expanded = false; onPlay() }, leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) })
            }
        }
    }
}
