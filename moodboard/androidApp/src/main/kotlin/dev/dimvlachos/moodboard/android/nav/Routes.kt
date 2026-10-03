package dev.dimvlachos.moodboard.android.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Gallery : NavKey

@Serializable data object Boards : NavKey

@Serializable data object Search : NavKey

/** [tab] keeps the same photo opened from two tabs as two distinct entries. */
@Serializable data class PhotoDetail(val photoId: String, val tab: String) : NavKey

@Serializable data class BoardDetail(val boardId: String) : NavKey

@Serializable data class BoardEditor(val boardId: String) : NavKey
