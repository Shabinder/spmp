package com.toasterofbread.spmp.model.mediaitem

import SpMp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.rememberHiddenItems
import com.toasterofbread.spmp.platform.PlatformContext

interface MediaItemHolder {
    // If item is null, consider it deleted
    val item: MediaItem?
}

fun isMediaItemHidden(item: MediaItem, db: Database, hidden_items: List<MediaItem>? = null): Boolean {
    if (hidden_items?.any { it.id == item.id } ?: item.Hidden.get(db)) {
        return true
    }

    if (!Settings.KEY_FILTER_ENABLE.get<Boolean>()) {
        return false
    }

    val title = item.getActiveTitle(db) ?: return false

    if (item is Artist && !Settings.KEY_FILTER_APPLY_TO_ARTISTS.get<Boolean>()) {
        return false
    }

    val keywords: Set<String> = Settings.KEY_FILTER_TITLE_KEYWORDS.get()
    for (keyword in keywords) {
        if (title.contains(keyword)) {
            return true
        }
    }

    return false
}

@Composable
fun List<MediaItemHolder>.rememberFilteredItems(apply_filter: Boolean, context: PlatformContext = SpMp.context): List<MediaItemHolder> {
    val hidden_items: List<MediaItem> = rememberHiddenItems(context)
    return remember(this, apply_filter, hidden_items) {
        if (apply_filter) mapNotNull {
            val item = it.item
            if (item != null && isMediaItemHidden(item, context.database, hidden_items)) {
                return@mapNotNull null
            }
            return@mapNotNull it
        }
        else this
    }
}
