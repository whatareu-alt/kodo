package com.japaneseminimal.musicplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.japaneseminimal.musicplayer.data.model.Song
import com.japaneseminimal.musicplayer.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = ""
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)

    private val _libraryState = MutableStateFlow(LibraryState())
    val libraryState: StateFlow<LibraryState> = _libraryState.asStateFlow()

    init {
        loadSongs()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(isLoading = true)
            repository.getAllSongs().collect { songs ->
                _libraryState.value = _libraryState.value.copy(
                    songs = songs,
                    isLoading = false
                )
            }
        }
    }

    fun searchSongs(query: String) {
        _libraryState.value = _libraryState.value.copy(searchQuery = query)
        
        if (query.isEmpty()) {
            loadSongs()
            return
        }

        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(isLoading = true)
            repository.searchSongs(query).collect { songs ->
                _libraryState.value = _libraryState.value.copy(
                    songs = songs,
                    isLoading = false
                )
            }
        }
    }
}
