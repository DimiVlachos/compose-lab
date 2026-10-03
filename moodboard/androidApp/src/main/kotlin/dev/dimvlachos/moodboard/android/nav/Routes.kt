package dev.dimvlachos.moodboard.android.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Gallery : NavKey

@Serializable data object Boards : NavKey

@Serializable data object Search : NavKey

@Serializable data class PhotoDetail(val photoId: String) : NavKey

@Serializable data class BoardDetail(val boardId: String) : NavKey

@Serializable data class BoardEditor(val boardId: String) : NavKey

val TopLevelRoutes: List<NavKey> = listOf(Gallery, Boards, Search)
