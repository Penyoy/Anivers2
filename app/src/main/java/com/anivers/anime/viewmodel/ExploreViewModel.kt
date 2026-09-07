package com.anivers.anime.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anivers.anime.data.model.Anime
import com.anivers.anime.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ExploreViewModel(private val repo: AnimeRepository = AnimeRepository()) : ViewModel() {
    private val _rekomendasi = MutableStateFlow<List<Anime>>(emptyList())
    val rekomendasi: StateFlow<List<Anime>> = _rekomendasi
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            try { _rekomendasi.value = repo.getRekomendasi() } catch (_: Exception) {}
            _loading.value = false
        }
    }
}
