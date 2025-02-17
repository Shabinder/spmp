package com.toasterofbread.spmp.ui.layout.prefspage

import SpMp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.toasterofbread.composesettings.ui.SettingsInterface
import com.toasterofbread.composesettings.ui.SettingsPageWithItems
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.Languages
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.theme.Theme

internal fun getPrefsPageSettingsInterface(
    pill_menu: PillMenu,
    ytm_auth: SettingsValueState<Set<String>>,
    getCategory: () -> PrefsPageCategory?,
    close: () -> Unit
): SettingsInterface {
    lateinit var settings_interface: SettingsInterface

    val pill_menu_action_overrider: @Composable PillMenu.Action.(i: Int) -> Boolean = { i ->
        if (i == 0) {
            var go_back by remember { mutableStateOf(false) }
            LaunchedEffect(go_back) {
                if (go_back) {
                    settings_interface.goBack()
                }
            }

            ActionButton(
                Icons.Filled.ArrowBack
            ) {
                go_back = true
            }
            true
        } else {
            false
        }
    }

    val discord_auth =
        SettingsValueState<String>(Settings.KEY_DISCORD_ACCOUNT_TOKEN.name).init(Settings.prefs, Settings.Companion::provideDefault)

    val categories = mapOf(
        PrefsPageCategory.GENERAL to lazy { getGeneralCategory(SpMp.ui_language, Languages.loadAvailableLanugages(SpMp.context)) },
        PrefsPageCategory.FILTER to lazy { getFilterCategory() },
        PrefsPageCategory.FEED to lazy { getFeedCategory() },
        PrefsPageCategory.PLAYER to lazy { getPlayerCategory() },
        PrefsPageCategory.LIBRARY to lazy { getLibraryCategory() },
        PrefsPageCategory.THEME to lazy { getThemeCategory(Theme) },
        PrefsPageCategory.LYRICS to lazy { getLyricsCategory() },
        PrefsPageCategory.DOWNLOAD to lazy { getDownloadCategory() },
        PrefsPageCategory.DISCORD_STATUS to lazy { getDiscordStatusGroup(discord_auth) },
        PrefsPageCategory.OTHER to lazy { getOtherCategory() },
        PrefsPageCategory.DEVELOPMENT to lazy { getDevelopmentCategory() }
    )

    settings_interface = SettingsInterface(
        { Theme },
        PrefsPageScreen.ROOT.ordinal,
        SpMp.context,
        Settings.prefs,
        Settings.Companion::provideDefault,
        pill_menu,
        { index, param ->
            when (PrefsPageScreen.values()[index]) {
                PrefsPageScreen.ROOT -> SettingsPageWithItems(
                    { getCategory()?.getTitle() },
                    { categories[getCategory()]?.value ?: emptyList() },
                    getIcon = {
                        val icon = getCategory()?.getIcon()
                        var current_icon by remember { mutableStateOf(icon) }

                        LaunchedEffect(icon) {
                            if (icon != null) {
                                current_icon = icon
                            }
                        }

                        return@SettingsPageWithItems current_icon
                    }
                )
                PrefsPageScreen.YOUTUBE_MUSIC_LOGIN -> getYoutubeMusicLoginPage(ytm_auth, param)
                PrefsPageScreen.DISCORD_LOGIN -> getDiscordLoginPage(discord_auth, manual = param == true)
                PrefsPageScreen.UI_DEBUG_INFO -> getUiDebugInfoPage()
            }
        },
        { page: Int? ->
            if (page == PrefsPageScreen.ROOT.ordinal) {
                pill_menu.removeActionOverrider(pill_menu_action_overrider)
            } else {
                pill_menu.addActionOverrider(pill_menu_action_overrider)
            }
        },
        close
    )

    return settings_interface
}
