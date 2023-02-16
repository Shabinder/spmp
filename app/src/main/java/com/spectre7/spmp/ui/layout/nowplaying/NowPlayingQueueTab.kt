@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.spectre7.spmp.ui.layout.nowplaying

import android.nfc.NfcAdapter.OnTagRemovedListener
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.rememberSwipeableState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.google.android.exoplayer2.C
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.utils.vibrateShort
import org.burnoutcrew.reorderable.*
import kotlin.math.roundToInt

private class QueueTabItem(val song: Song, val key: Int) {

//    val added_time = System.currentTimeMillis()
    val current_element_modifier = Modifier.background(MainActivity.theme.getOnBackground(true), RoundedCornerShape(45))

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun queueElementSwipeState(requestRemove: () -> Unit): SwipeableState<Int> {
        val swipe_state = rememberSwipeableState(1)
        var removed by remember { mutableStateOf(false) }

        LaunchedEffect(remember { derivedStateOf { swipe_state.progress.fraction > 0.8f } }.value) {
            if (!removed && swipe_state.targetValue != 1 && swipe_state.progress.fraction > 0.8f) {
                requestRemove()
                removed = true
            }
        }

        return swipe_state
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun QueueElement(list_state: ReorderableLazyListState, current: Boolean, index: Int, playerProvider: () -> PlayerViewContext, requestRemove: () -> Unit) {
        val swipe_state = queueElementSwipeState(requestRemove)
        val max_offset = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
        val anchors = mapOf(-max_offset to 0, 0f to 1, max_offset to 2)

        Box(
            Modifier.offset { IntOffset(swipe_state.offset.value.roundToInt(), 0) }.then(if (current) current_element_modifier else Modifier)
        ) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 10.dp, end = 20.dp)
            ) {
                val contentColourProvider = if (current) MainActivity.theme.getBackgroundProvider(true) else MainActivity.theme.getOnBackgroundProvider(true)
                song.PreviewLong(
                    contentColourProvider,
                    remember(index) {
                        {
                            playerProvider().copy(onClickedOverride = {
                                PlayerServiceHost.player.seekTo(index, C.TIME_UNSET)
                            })
                        }
                    },
                    true,
                    Modifier
                        .weight(1f)
                        .swipeable(
                            swipe_state,
                            anchors,
                            Orientation.Horizontal,
                            thresholds = { _, _ -> FractionalThreshold(0.2f) }
                        )
                )

                // Drag handle
                Icon(Icons.Filled.Menu, null, Modifier.detectReorder(list_state).requiredSize(25.dp), tint = contentColourProvider())
            }
        }
    }
}

@Composable
fun QueueTab(expansionProvider: () -> Float, playerProvider: () -> PlayerViewContext, scroll: (pages: Int) -> Unit) {

    var key_inc by remember { mutableStateOf(0) }
    val v_removed = remember { mutableStateListOf<Int>() }
    val undo_list = remember { mutableStateListOf<() -> Unit>() }

    val song_items: SnapshotStateList<QueueTabItem> = remember { mutableStateListOf<QueueTabItem>().also { list ->
        for (item in PlayerServiceHost.status.m_queue) {
            list.add(QueueTabItem(item, key_inc++))
        }
    } }

    val queue_listener = remember {
        object : PlayerServiceHost.PlayerQueueListener {
            override fun onSongAdded(song: Song, index: Int) {
                song_items.add(index, QueueTabItem(song, key_inc++))
            }
            override fun onSongRemoved(song: Song, index: Int) {
                val i = v_removed.indexOf(index)
                if (i != -1) {
                    v_removed.removeAt(i)
                }
                else {
                    song_items.removeAt(index)
                }
            }
            override fun onSongMoved(from: Int, to: Int) {
                song_items.add(to, song_items.removeAt(from))
            }
            override fun onCleared() {
                song_items.clear()
            }
        }
    }

    var playing_key by remember { mutableStateOf<Int?>(null) }

    // TODO
    // LaunchedEffect(p_status.index) {
    //     playing_key =
    // }

    DisposableEffect(Unit) {
        PlayerServiceHost.service.addQueueListener(queue_listener)
        onDispose {
            PlayerServiceHost.service.removeQueueListener(queue_listener)
        }
    }

    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            song_items.add(to.index, song_items.removeAt(from.index))
        },
        onDragEnd = { from, to ->
            if (from != to) {
                PlayerServiceHost.player.moveMediaItem(from, to)
                playing_key = null
            }
        }
    )

    fun removeSong(song: Song, index: Int) {
        v_removed.add(index)

        undo_list.add({
            PlayerServiceHost.service.addToQueue(song, index)
        })

        song_items.removeAt(index)
        PlayerServiceHost.service.removeFromQueue(index)
    }

    Box(Modifier.fillMaxSize()) {

        LazyColumn(
            state = state.listState,
            modifier = Modifier
                .reorderable(state)
                .detectReorderAfterLongPress(state)
                .align(Alignment.TopCenter)
        ) {

            items(song_items.size, { song_items[it].key }) { index ->
                val item = song_items[index]
                ReorderableItem(state, key = item.key) { is_dragging ->

                    LaunchedEffect(is_dragging) {
                        if (is_dragging) {
                            vibrateShort()
                            playing_key = song_items[PlayerServiceHost.status.index].key
                        }
                    }

                    Box(Modifier.height(50.dp)) {
//                        var visible by remember { mutableStateOf(false ) }
//                        LaunchedEffect(visible) {
//                            visible = true
//                        }
//
//                        AnimatedVisibility(
//                            visible,
//                            enter = if (System.currentTimeMillis() - item.added_time < 250)
//                                        fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 })
//                                    else EnterTransition.None,
//                            exit = ExitTransition.None
//                        ) {
                            item.QueueElement(
                                state,
                                if (playing_key != null) playing_key == item.key else PlayerServiceHost.status.m_index == index,
                                index,
                                playerProvider,
                                remember(item.song, index) { { removeSong(item.song, index) } }
                            )
//                        }
                    }
                }
            }
        }

        ActionBar(expansionProvider, undo_list, scroll)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxScope.ActionBar(expansionProvider: () -> Float, undo_list: SnapshotStateList<() -> Unit>, scroll: (pages: Int) -> Unit) {
    val slide_offset: (fullHeight: Int) -> Int = remember { { (it * 0.7).toInt() } }

    Box(Modifier.align(Alignment.BottomCenter)) {

        AnimatedVisibility(
            remember { derivedStateOf { expansionProvider() >= 0.975f } }.value,
            enter = slideInVertically(initialOffsetY = slide_offset),
            exit = slideOutVertically(targetOffsetY = slide_offset)
        ) {
            Row(
                Modifier
                    .padding(10.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    { scroll(-1) },
                    Modifier
                        .background(MainActivity.theme.getOnBackground(true), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, null, tint = MainActivity.theme.getBackground(true))
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val removed: List<Pair<Song, Int>> = PlayerServiceHost.service.clearQueue(keep_current = PlayerServiceHost.status.m_queue.size > 1)
                            if (removed.isNotEmpty()) {
                                val index = PlayerServiceHost.player.currentMediaItemIndex
                                undo_list.add {
                                    val before = mutableListOf<Song>()
                                    val after = mutableListOf<Song>()
                                    for (item in removed.withIndex()) {
                                        if (item.value.second >= index) {
                                            for (i in item.index until removed.size) {
                                                after.add(removed[i].first)
                                            }
                                            break
                                        }
                                        before.add(item.value.first)
                                    }

                                    PlayerServiceHost.service.addMultipleToQueue(before, 0)
                                    PlayerServiceHost.service.addMultipleToQueue(after, index + 1)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MainActivity.theme.getOnBackground(true)
                        )
                    ) {
                        Text(
                            text = "Clear",
                            color = MainActivity.theme.getBackground(true)
                        )
                    }

                    Button(
                        onClick = {
                            val swaps = PlayerServiceHost.service.shuffleQueue(return_swaps = true)!!
                            if (swaps.isNotEmpty()) {
                                undo_list.add {
                                    for (swap in swaps.asReversed()) {
                                        PlayerServiceHost.service.swapQueuePositions(swap.first, swap.second)
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MainActivity.theme.getOnBackground(true)
                        )
                    ) {
                        Text(
                            text = "Shuffle",
                            color = MainActivity.theme.getBackground(true)
                        )
                    }
                }

                AnimatedVisibility(undo_list.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .minimumTouchTargetSize()
                            .background(MainActivity.theme.getOnBackground(true), CircleShape)
                            .combinedClickable(
                                onClick = {
                                    if (undo_list.isNotEmpty()) {
                                        undo_list
                                            .removeLast()
                                            .invoke()
                                    }
                                },
                                onLongClick = {
                                    if (undo_list.isNotEmpty()) {
                                        vibrateShort()
                                        for (undo_action in undo_list.asReversed()) {
                                            undo_action.invoke()
                                        }
                                        undo_list.clear()
                                    }
                                }
                            )
                            .size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Undo, null, tint = MainActivity.theme.getBackground(true))
                    }
                }
            }
        }
    }
}