package com.toasterofbread.spmp.platform.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import com.toasterofbread.utils.common.thenIf

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun Modifier.platformClickable(onClick: (() -> Unit)?, onAltClick: (() -> Unit)?, indication: Indication?): Modifier =
    this.thenIf(onClick != null) { onClick(onClick = onClick!!) }
        .thenIf(onAltClick != null) {
            onClick(
                matcher = PointerMatcher.mouse(PointerButton.Secondary),
                onClick = onAltClick!!
            )
        }
