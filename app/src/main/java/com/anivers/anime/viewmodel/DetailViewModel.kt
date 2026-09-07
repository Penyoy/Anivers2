package com.anivers.anime.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anivers.anime.data.model.SeriesDetail
import com.anivers.anime.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetailViewModel(private val repo: AnimeRepository = AnimeRepository()) : ViewModel() {
    private val _detail = MutableStateFlow<SeriesDetail?>(null)
    val detail: StateFlow<SeriesDetail?> = _detail
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun load(slug: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            if (slug == "undefined" || slug.isBlank()) {
                _error.value = "Slug tidak valid: $slug"
                _loading.value = false
                return@launch
            }
            try {
                val d = repo.getSeries(slug)
                if (d == null) _error.value = "Data tidak ditemukan untuk $slug"
                _detail.value = d
            } catch (e: Exception) {
                _error.value = e.message
            }
            _loading.value = false
        }
    }
}
