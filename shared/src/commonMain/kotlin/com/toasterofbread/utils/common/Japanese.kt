package com.toasterofbread.utils.common

import java.lang.Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
import java.lang.Character.UnicodeBlock.HIRAGANA
import java.lang.Character.UnicodeBlock.KATAKANA
import java.lang.Character.UnicodeBlock.of

fun Char.isJP(): Boolean = isKanji() || isHiragana() || isKatakana()
fun Char.isKanji(): Boolean = of(this) == CJK_UNIFIED_IDEOGRAPHS
fun Char.isHiragana(): Boolean = of(this) == HIRAGANA
fun Char.isKatakana(): Boolean = of(this) == KATAKANA

// http://kevin3sei.blog95.fc2.com/blog-entry-111.html
fun Char.isFullWidth(): Boolean =
    !(this <= '\u007e' || this == '\u00a5' || this == '\u203e' || this in '\uff61'..'\uff9f')

fun Char.isHalfWidthKatakana(): Boolean {
    return ('\uff66' <= this) && (this <= '\uff9d')
}

fun Char.isFullWidthKatakana(): Boolean {
    return ('\u30a1' <= this) && (this <= '\u30fe')
}

fun Char.toHiragana(): Char {
    if (isFullWidthKatakana()) {
        return (this - 0x60)
    }
    else if (isHalfWidthKatakana()) {
        return (this - 0xcf25)
    }
    return this
}

fun String.toHiragana(): String {
    val ret = StringBuilder()
    for (char in this) {
        ret.append(char.toHiragana())
    }
    return ret.toString()
}

fun String.hasKanjiAndHiragana(): Boolean {
    var has_kanji = false
    var has_hiragana = false
    for (char in this) {
        when (of(char)) {
            CJK_UNIFIED_IDEOGRAPHS -> {
                if (has_hiragana)
                    return true
                has_kanji = true
            }
            HIRAGANA -> {
                if (has_kanji)
                    return true
                has_hiragana = true
            }
            else -> {}
        }
    }
    return false
}
