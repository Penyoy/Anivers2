package com.anivers.anime.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anivers.anime.data.model.Anime
import com.anivers.anime.data.repository.AnimeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val repo: AnimeRepository = AnimeRepository()) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _results = MutableStateFlow<List<Anime>>(emptyList())
    val results: StateFlow<List<Anime>> = _results
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _preview = MutableStateFlow<List<Anime>>(emptyList())
    val preview: StateFlow<List<Anime>> = _preview
    private var debounce: Job? = null

    fun setQuery(q: String) { _query.value = q }

    fun search(q: String) {
        if (q.isBlank()) return
        viewModelScope.launch {
            _loading.value = true
            try {
                val list = repo.search(q)
                _results.value = list.filter { it.url.isNotEmpty() && it.url != "undefined" }
            } catch (e: Exception) { _results.value = emptyList() }
            _loading.value = false
        }
    }

    fun preview(q: String) {
        debounce?.cancel()
        if (q.trim().length < 2) { _preview.value = emptyList(); return }
        debounce = viewModelScope.launch {
            delay(320)
            try {
                val list = repo.search(q.trim())
                _preview.value = list.take(6)
            } catch (_: Exception) { _preview.value = emptyList() }
        }
    }

    fun clear() { _query.value = ""; _results.value = emptyList(); _preview.value = emptyList() }
}
