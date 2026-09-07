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
                if (d == null) {
                    _error.value = "Data tidak ditemukan untuk $slug (coba cari \"gotoubun\" di Search)"
                    android.util.Log.w("ANIVERS_DETAIL", "null for slug=$slug")
                }
                _detail.value = d
            } catch (e: Exception) {
                android.util.Log.e("ANIVERS_DETAIL", "load fail slug=$slug", e)
                _error.value = "Gagal memuat detail: ${e.message?.take(140)}\nRaw: ${e.toString().take(120)}"
            }
            _loading.value = false
        }
    }
}
