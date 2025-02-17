package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class AccountPlaylistsEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract suspend fun getAccountPlaylists(): Result<List<RemotePlaylistData>>
}

abstract class CreateAccountPlaylistEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract suspend fun createAccountPlaylist(title: String, description: String): Result<String>
}

abstract class DeleteAccountPlaylistEndpoint: YoutubeApi.UserAuthState.UserAuthEndpoint() {
    abstract suspend fun deleteAccountPlaylist(playlist_id: String): Result<Unit>
}
