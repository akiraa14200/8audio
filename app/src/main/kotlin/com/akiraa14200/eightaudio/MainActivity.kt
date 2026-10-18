package com.akiraa14200.eightaudio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

private val bg=Color(0xFF090B0D); private val card=Color(0xFF171B20); private val mint=Color(0xFFB8F7D4); private val muted=Color(0xFF9AA4AE)

class MainActivity:ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); setContent { AudioApp { t ->
   val i=Intent(this,MusicPlaybackService::class.java).setAction(MusicPlaybackService.ACTION_PLAY)
    .putExtra(MusicPlaybackService.URL,t.streamUrl).putExtra(MusicPlaybackService.TITLE,t.title).putExtra(MusicPlaybackService.ARTIST,t.artist).putExtra(MusicPlaybackService.ART,t.artwork)
   ContextCompat.startForegroundService(this,i)
 }}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AudioApp(play:(Track)->Unit) {
 var tab by remember{mutableStateOf("Home")}; var query by remember{mutableStateOf("")}
 var tracks by remember{mutableStateOf(emptyList<Track>())}; var current by remember{mutableStateOf<Track?>(null)}
 var busy by remember{mutableStateOf(false)}; var error by remember{mutableStateOf("")}
 val scope= rememberCoroutineScope()
 fun search(q:String){if(q.isBlank())return; query=q;busy=true;error="";scope.launch{try{tracks=withContext(Dispatchers.IO){SaavnSearch.search(q)};if(tracks.isEmpty())error="No playable results. Try another search."}catch(e:Exception){error="Couldn't load music. Check your connection."}finally{busy=false}}}
 MaterialTheme(colorScheme=darkColorScheme(primary=mint,background=bg,surface=card,onSurface=Color.White)) {
  Scaffold(containerColor=bg,bottomBar={Column{
   current?.let{t->Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=5.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF20302A)).clickable{play(t)}.padding(9.dp),verticalAlignment=Alignment.CenterVertically){
    AsyncImage(t.artwork,null,Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)));Column(Modifier.weight(1f).padding(start=10.dp)){Text(t.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold);Text(t.artist,color=muted,fontSize=12.sp)}
    Icon(Icons.Rounded.GraphicEq,null,tint=mint);IconButton(onClick={current=null}){Icon(Icons.Rounded.Close,null,tint=muted)}
   }}
   NavigationBar(containerColor=Color(0xFF121519)){listOf("Home","Search","Library").forEach{v->NavigationBarItem(selected=tab==v,onClick={tab=v},icon={Icon(when(v){"Search"->Icons.Rounded.Search;"Library"->Icons.Rounded.LibraryMusic;else->Icons.Rounded.Home},v)},label={Text(v)},colors=NavigationBarItemDefaults.colors(selectedIconColor=mint,selectedTextColor=mint,indicatorColor=Color(0xFF26362F),unselectedIconColor=muted,unselectedTextColor=muted))}}
  }}){pad->
   LazyColumn(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF12201B),bg,bg))).padding(pad),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
    item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text("8 AUDIO",color=mint,fontWeight=FontWeight.Bold,letterSpacing=3.sp,fontSize=12.sp);Text(when(tab){"Search"->"Find your sound";"Library"->"Your music space";else->"Good music. No limits."},fontSize=24.sp,fontWeight=FontWeight.Bold)};Icon(Icons.Rounded.GraphicEq,null,tint=mint,modifier=Modifier.size(32.dp))}}
    if(tab=="Home"){item{Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Color(0xFF27483D),Color(0xFF18211E),Color(0xFF25263A)))).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("YOUR NEXT\nFAVOURITE SONG",fontSize=27.sp,lineHeight=30.sp,fontWeight=FontWeight.ExtraBold);Text("Find tracks, artists and albums.",color=Color(0xFFD2DDD7));Button(onClick={tab="Search"},colors=ButtonDefaults.buttonColors(containerColor=mint,contentColor=bg)){Icon(Icons.Rounded.Search,null);Text("  Explore music")}}}
     item{Text("Quick picks",fontWeight=FontWeight.Bold,fontSize=18.sp)}
     item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Arijit Singh","Trending Hindi","Lo-fi").forEach{v->Surface(Modifier.weight(1f).clickable{tab="Search";search(v)},color=card,shape=RoundedCornerShape(15.dp)){Text(v,Modifier.padding(11.dp),fontSize=12.sp,maxLines=1)}}}}
    }
    if(tab=="Search"){item{OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Songs, artists, albums…",color=muted)},leadingIcon={Icon(Icons.Rounded.Search,null)},trailingIcon={IconButton(onClick={search(query)}){Icon(Icons.Rounded.ArrowForward,"Search",tint=mint)}},singleLine=true,shape=RoundedCornerShape(17.dp),keyboardActions=KeyboardActions(onSearch={search(query)}),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=mint,unfocusedBorderColor=Color(0xFF343B43),focusedContainerColor=card,unfocusedContainerColor=card))}
     item{Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("Hindi hits","Lo-fi","Workout").forEach{v->SuggestionChip(onClick={search(when(v){"Lo-fi"->"lofi";"Workout"->"workout songs";else->"latest hindi hits"})},label={Text(v)})}}}
     if(busy)item{Box(Modifier.fillMaxWidth().padding(24.dp),contentAlignment=Alignment.Center){CircularProgressIndicator(color=mint)}}
     if(error.isNotBlank())item{Text(error,color=Color(0xFFFFB4AB))}
     if(tracks.isNotEmpty())item{Text("Results",fontSize=18.sp,fontWeight=FontWeight.Bold)}
     items(tracks,key={it.id}){t->TrackItem(t){current=t;play(t)}}
    }
    if(tab=="Library")item{Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(card).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Rounded.LibraryMusic,null,tint=mint,modifier=Modifier.size(30.dp));Text("Your library",fontSize=22.sp,fontWeight=FontWeight.Bold);Text("Your saved music and playlists will appear here.",color=muted);Button(onClick={tab="Search"},colors=ButtonDefaults.buttonColors(containerColor=mint,contentColor=bg)){Text("Find music")}}}
   }
  }
 }
}

@Composable private fun TrackItem(t:Track,onPlay:()->Unit){Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(card).clickable{onPlay()}.padding(10.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(t.artwork,t.title,Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)));Column(Modifier.weight(1f).padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(t.title,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(t.artist,color=muted,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)};IconButton(onClick=onPlay){Icon(Icons.Rounded.PlayCircle,"Play",tint=mint,modifier=Modifier.size(30.dp))}}}
