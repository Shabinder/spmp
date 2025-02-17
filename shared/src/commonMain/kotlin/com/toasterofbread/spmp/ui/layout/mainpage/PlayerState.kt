package com.toasterofbread.spmp.ui.layout.mainpage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.layout.BrowseParamsData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.platform.MediaPlayerRepeatMode
import com.toasterofbread.spmp.platform.MediaPlayerService
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.service.playerservice.PlayerService
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import com.toasterofbread.utils.common.indexOfOrNull
import java.net.URI
import java.net.URISyntaxException

class PlayerStatus internal constructor(private val player: PlayerService) {
    fun getProgress(): Float = player.duration_ms.let { duration ->
        if (duration <= 0f) 0f
        else player.current_position_ms.toFloat() / duration
    }

    fun getPositionMillis(): Long = player.current_position_ms

    var m_playing: Boolean by mutableStateOf(player.is_playing)
        private set
    var m_duration_ms: Long by mutableStateOf(player.duration_ms)
        private set
    var m_song: Song? by mutableStateOf(player.getSong())
        private set
    var m_index: Int by mutableStateOf(player.current_song_index)
        private set
    var m_repeat_mode: MediaPlayerRepeatMode by mutableStateOf(player.repeat_mode)
        private set
    // TODO
    var m_has_next: Boolean by mutableStateOf(true)
        private set
    var m_has_previous: Boolean by mutableStateOf(true)
        private set
    var m_volume: Float by mutableStateOf(player.volume)
        private set
    var m_song_count: Int by mutableStateOf(player.song_count)
        private set
    var m_undo_count: Int by mutableStateOf(player.undo_count)
        private set
    var m_redo_count: Int by mutableStateOf(player.redo_count)
        private set

    init {
        player.addListener(object : MediaPlayerService.Listener() {
            init {
                onEvents()
            }

            override fun onSongTransition(song: Song?) {
                m_song = song
            }
            override fun onPlayingChanged(is_playing: Boolean) {
                m_playing = is_playing
            }
            override fun onRepeatModeChanged(repeat_mode: MediaPlayerRepeatMode) {
                m_repeat_mode = repeat_mode
            }
            override fun onUndoStateChanged() {
                m_undo_count = player.undo_count
                m_redo_count = player.redo_count
            }

            override fun onEvents() {
                m_duration_ms = player.duration_ms
                m_index = player.current_song_index
                m_volume = player.volume
                m_song_count = player.song_count

                if (m_index > player.active_queue_index) {
                    player.active_queue_index = m_index
                }
            }
        })
    }
}

open class PlayerState protected constructor(
    private val onClickedOverride: ((item: MediaItem, multiselect_key: Int?) -> Unit)? = null,
    private val onLongClickedOverride: ((item: MediaItem, long_press_data: LongPressMenuData?) -> Unit)? = null,
    private val upstream: PlayerState? = null
) {
    val database: Database get() = context.database
    open val context: PlatformContext get() = upstream!!.context

    open val main_page_state: MainPageState get() = upstream!!.main_page_state
    val main_page: MainPage get() = main_page_state.current_page

    open val overlay_page: Pair<PlayerOverlayPage, MediaItem?>? get() = upstream!!.overlay_page
    open val bottom_padding: Float get() = upstream!!.bottom_padding
    open val main_multiselect_context: MediaItemMultiSelectContext get() = upstream!!.main_multiselect_context
    open val np_theme_mode: ThemeMode get() = upstream!!.np_theme_mode
    open val np_overlay_menu: MutableState<OverlayMenu?> get() = upstream!!.np_overlay_menu

    open val player: PlayerService? get() = upstream!!.player
    open fun withPlayer(action: PlayerService.() -> Unit) {
        upstream!!.withPlayer(action)
    }

    open val status: PlayerStatus get() = upstream!!.status
    open val session_started: Boolean get() = upstream!!.session_started

    open val screen_size: DpSize get() = upstream!!.screen_size

    open fun interactService(action: (player: PlayerService) -> Unit) { upstream!!.interactService(action) }
    open fun isRunningAndFocused(): Boolean = upstream!!.isRunningAndFocused()

    val bottom_padding_dp: Dp
        @Composable get() = with(LocalDensity.current) {
            bottom_padding.toDp() + getDefaultVerticalPadding()
        }

    fun copy(
        onClickedOverride: ((item: MediaItem, multiselect_key: Int?) -> Unit)? = null,
        onLongClickedOverride: ((item: MediaItem, long_press_data: LongPressMenuData?) -> Unit)? = null
    ): PlayerState {
        return PlayerState(onClickedOverride, onLongClickedOverride, this)
    }

    fun openUri(uri_string: String): Result<Unit> {
        fun failure(reason: String): Result<Unit> = Result.failure(URISyntaxException(uri_string, reason))

        val uri = URI(uri_string)
        if (uri.host != "music.youtube.com" && uri.host != "www.youtube.com") {
            return failure("Unsupported host '${uri.host}'")
        }

        val path_parts = uri.path.split('/').filter { it.isNotBlank() }
        when (path_parts.firstOrNull()) {
            "channel" -> {
                val channel_id = path_parts.elementAtOrNull(1) ?: return failure("No channel ID")

                interactService {
                    val artist = ArtistRef(channel_id)
                    artist.createDbEntry(context.database)
                    openMediaItem(artist)
                }
            }
            "watch" -> {
                val v_start = (uri.query.indexOfOrNull("v=") ?: return failure("'v' query parameter not found")) + 2
                val v_end = uri.query.indexOfOrNull("&", v_start) ?: uri.query.length

                interactService {
                    val song = SongRef(uri.query.substring(v_start, v_end))
                    song.createDbEntry(context.database)
                    playMediaItem(song)
                }
            }
            "playlist" -> {
                val list_start = (uri.query.indexOfOrNull("list=") ?: return failure("'list' query parameter not found")) + 5
                val list_end = uri.query.indexOfOrNull("&", list_start) ?: uri.query.length

                interactService {
                    val playlist = RemotePlaylistRef(uri.query.substring(list_start, list_end))
                    playlist.createDbEntry(context.database)
                    openMediaItem(playlist)
                }
            }
            else -> return failure("Uri path not implemented")
        }

        return Result.success(Unit)
    }

    @Composable
    open fun nowPlayingTopOffset(base: Modifier = Modifier): Modifier = upstream!!.nowPlayingTopOffset(base)
    @Composable
    open fun nowPlayingBottomPadding(include_np: Boolean = false): Dp = upstream!!.nowPlayingBottomPadding(include_np)

    open fun onNavigationBarTargetColourChanged(colour: Color?, from_lpm: Boolean): Unit = upstream!!.onNavigationBarTargetColourChanged(colour, from_lpm)

    open fun setMainPage(page: MainPage?) { upstream!!.setMainPage(page) }
    open fun setOverlayPage(page: PlayerOverlayPage?, from_current: Boolean = false, replace_current: Boolean = false) { upstream!!.setOverlayPage(
        page,
        from_current,
        replace_current
    ) }

    open fun navigateBack() { upstream!!.navigateBack() }

    open fun onMediaItemClicked(item: MediaItem, multiselect_key: Int? = null) {
        if (onClickedOverride != null) {
            onClickedOverride.invoke(item, multiselect_key)
        }
        else {
            upstream!!.onMediaItemClicked(item, multiselect_key)
        }
    }
    open fun onMediaItemLongClicked(item: MediaItem, long_press_data: LongPressMenuData? = null) {
        if (onLongClickedOverride != null) {
            onLongClickedOverride.invoke(item, long_press_data)
        }
        else {
            upstream!!.onMediaItemLongClicked(item, long_press_data)
        }
    }

    fun onMediaItemLongClicked(item: MediaItem, queue_index: Int) {
        onMediaItemLongClicked(item, LongPressMenuData(item, multiselect_key = queue_index))
    }

    open fun openPage(
        page: PlayerOverlayPage,
        from_current: Boolean = false,
        replace_current: Boolean = false,
    ) { upstream!!.openPage(
        page,
        from_current,
        replace_current
    ) }

    open fun openMediaItem(
        item: MediaItem,
        from_current: Boolean = false,
        replace_current: Boolean = false,
        browse_params: BrowseParamsData? = null,
    ) { upstream!!.openMediaItem(
        item,
        from_current,
        replace_current,
        browse_params
    ) }

    open fun playMediaItem(item: MediaItem, shuffle: Boolean = false) { upstream!!.playMediaItem(item, shuffle) }
    open fun playPlaylist(playlist: Playlist, from_index: Int = 0) { upstream!!.playPlaylist(playlist, from_index) }
    open fun openViewMorePage(browse_id: String, title: String?) { upstream!!.openViewMorePage(browse_id, title) }
    open fun openNowPlayingOverlayMenu(menu: OverlayMenu? = null) { upstream!!.openNowPlayingOverlayMenu(menu) }

    open fun showLongPressMenu(data: LongPressMenuData) { upstream!!.showLongPressMenu(data) }
    fun showLongPressMenu(item: MediaItem) { showLongPressMenu(LongPressMenuData(item)) }

    open fun hideLongPressMenu() { upstream!!.hideLongPressMenu() }
}
