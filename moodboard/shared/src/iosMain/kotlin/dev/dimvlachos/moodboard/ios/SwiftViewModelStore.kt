package dev.dimvlachos.moodboard.ios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.dimvlachos.moodboard.boards.BoardDetailViewModel
import dev.dimvlachos.moodboard.boards.BoardEditorViewModel
import dev.dimvlachos.moodboard.boards.BoardsViewModel
import dev.dimvlachos.moodboard.detail.PhotoDetailViewModel
import dev.dimvlachos.moodboard.gallery.GalleryViewModel
import dev.dimvlachos.moodboard.search.SearchViewModel

/**
 * The ViewModels of one SwiftUI screen. The screen's model owns one store and calls [clear] when it
 * goes away, which cancels every viewModelScope in it.
 */
class SwiftViewModelStore {
    private val store = ViewModelStore()

    fun gallery(): GalleryViewModel = get("gallery") { GalleryViewModel() }

    fun photoDetail(photoId: String): PhotoDetailViewModel =
        get("photo:$photoId") { PhotoDetailViewModel(photoId) }

    fun boards(): BoardsViewModel = get("boards") { BoardsViewModel() }

    fun boardDetail(boardId: String): BoardDetailViewModel =
        get("board:$boardId") { BoardDetailViewModel(boardId) }

    fun boardEditor(boardId: String): BoardEditorViewModel =
        get("editor:$boardId") { BoardEditorViewModel(boardId) }

    fun search(): SearchViewModel = get("search") { SearchViewModel() }

    fun clear() = store.clear()

    private inline fun <reified T : ViewModel> get(key: String, noinline create: () -> T): T =
        ViewModelProvider.create(store, viewModelFactory { initializer { create() } })[
                key, T::class]
}
