package com.anivers.anime.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anivers.anime.data.model.Anime
import com.anivers.anime.data.repository.AnimeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val ongoing: List<Anime> = emptyList(),
    val baruUpload: List<Anime> = emptyList(),
    val movies: List<Anime> = emptyList(),
    val rekomendasi: List<Anime> = emptyList(),
    val topAnime: List<Anime> = emptyList(),
    val jadwal: List<com.anivers.anime.data.model.JadwalDay> = emptyList(),
    val error: String? = null
)

class HomeViewModel(private val repo: AnimeRepository = AnimeRepository()) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val ongoingDef = async { runCatching { repo.getOngoing() }.getOrElse { emptyList() } }
                val baruDef = async { runCatching { repo.getBaruUpload() }.getOrElse { emptyList() } }
                val movieDef = async { runCatching { repo.getMovie() }.getOrElse { emptyList() } }
                val rekomDef = async { runCatching { repo.getRekomendasi() }.getOrElse { emptyList() } }
                val jadwalDef = async { runCatching { repo.getJadwal().data ?: emptyList() }.getOrElse { emptyList() } }

                val ongoing = ongoingDef.await()
                val baru = baruDef.await()
                val movies = movieDef.await()
                val rekom = rekomDef.await()
                val jadwal = jadwalDef.await()

                val top = rekom.sortedByDescending { it.score.toDoubleOrNull() ?: 0.0 }.take(10)
                    .ifEmpty { ongoing.take(10) }

                if (ongoing.isEmpty() && baru.isEmpty() && movies.isEmpty() && rekom.isEmpty()) {
                    _state.value = HomeUiState(loading = false, error = "Gagal memuat data. Periksa koneksi.")
                } else {
                    _state.value = HomeUiState(
                        loading = false,
                        ongoing = ongoing,
                        baruUpload = baru,
                        movies = movies,
                        rekomendasi = rekom,
                        topAnime = top,
                        jadwal = jadwal
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }
}
