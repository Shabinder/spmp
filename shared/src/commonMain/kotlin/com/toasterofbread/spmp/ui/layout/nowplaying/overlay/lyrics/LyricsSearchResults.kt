package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource.SearchResult
import com.toasterofbread.utils.composable.Marquee
import com.toasterofbread.utils.common.getContrasted
import com.toasterofbread.utils.common.setAlpha

@Composable
internal fun ColumnScope.LyricsSearchResults(results_and_source: Pair<List<SearchResult>, Int>, modifier: Modifier = Modifier, onFinished: (Int?) -> Unit) {
    BackHandler {
        onFinished(null)
    }

    val (results, source_idx) = results_and_source

    if (results.isNotEmpty()) {
        LazyColumn(
            modifier.fillMaxSize().weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(results.size + 1) { index ->

                if (index == results.size) {
                    Text(getString("lyrics_no_more_results"), color = Theme.accent)
                }
                else {
                    val result = results[index]
                    Box(
                        Modifier
                            .background(Theme.accent, RoundedCornerShape(16))
                            .clickable {
                                onFinished(index)
                            }
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                            @Composable
                            fun Item(name: String, value: String, colour: Color) {
                                Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(name.uppercase(), style = MaterialTheme.typography.bodySmall, color = colour)
                                    Text(value, color = colour)
                                }
                            }

                            val shape = RoundedCornerShape(16)

                            Marquee(Modifier.fillMaxWidth()) {
                                Text(result.name, color = Theme.on_accent, softWrap = false)
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End)
                            ) {
                                @Composable
                                fun text(text: String, colour: Color) {
                                    Text(
                                        text,
                                        Modifier.padding(5.dp),
                                        color = colour,
                                        fontSize = 10.sp,
                                        softWrap = false
                                    )
                                }

                                val sync_colour = if (result.sync_type == SongLyrics.SyncType.NONE) Color.LightGray else Color.Magenta
                                Box(Modifier.background(sync_colour, CircleShape)) {
                                    text(result.sync_type.getReadable(), sync_colour.getContrasted())
                                }

                                val source = remember(source_idx) { LyricsSource.fromIdx(source_idx) }
                                val source_colour = source.getColour()
                                Box(Modifier.background(source_colour, CircleShape)) {
                                    text(source.getReadable(), source_colour.getContrasted())
                                }
                            }

                            Column(
                                Modifier
                                    .border(Dp.Hairline, Theme.on_accent, shape)
                                    .background(
                                        Theme
                                            .on_accent
                                            .setAlpha(0.1f), shape
                                    )
                                    .padding(2.dp)
                                    .fillMaxWidth()
                            ) {
                                if (result.artist_name != null) {
                                    Item(getString("artist"), result.artist_name!!, Theme.on_accent)
                                }
                                if (result.album_name != null) {
                                    Item(getString("album"), result.album_name!!, Theme.on_accent)
                                }
                            }
                        }
                    }
                }

            }
        }
    }
    else {
        Text(getStringTODO("No results found"), modifier, color = Theme.accent)
    }
}
