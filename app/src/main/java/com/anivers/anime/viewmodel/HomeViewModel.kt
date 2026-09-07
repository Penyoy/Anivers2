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
    val error: String? = null,
    val debugDetail: String? = null
)

class HomeViewModel(private val repo: AnimeRepository = AnimeRepository()) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, debugDetail = null)
            try {
                val errors = mutableListOf<String>()
                val ongoingDef = async {
                    try { repo.getOngoing() } catch (e: Exception) {
                        val msg = "ongoing: ${e.message?.take(120)}"
                        errors.add(msg); android.util.Log.e("ANIVERS_HOME", msg, e); emptyList<Anime>()
                    }
                }
                val baruDef = async {
                    try { repo.getBaruUpload() } catch (e: Exception) {
                        val msg = "baruUpload: ${e.message?.take(120)}"
                        errors.add(msg); android.util.Log.e("ANIVERS_HOME", msg, e); emptyList<Anime>()
                    }
                }
                val movieDef = async {
                    try { repo.getMovie() } catch (e: Exception) {
                        val msg = "movie: ${e.message?.take(120)}"
                        errors.add(msg); android.util.Log.e("ANIVERS_HOME", msg, e); emptyList<Anime>()
                    }
                }
                val rekomDef = async {
                    try { repo.getRekomendasi() } catch (e: Exception) {
                        val msg = "rekomendasi: ${e.message?.take(120)}"
                        errors.add(msg); android.util.Log.e("ANIVERS_HOME", msg, e); emptyList<Anime>()
                    }
                }
                val jadwalDef = async {
                    try { repo.getJadwal().data ?: emptyList() } catch (e: Exception) {
                        val msg = "jadwal: ${e.message?.take(120)}"
                        errors.add(msg); android.util.Log.e("ANIVERS_HOME", msg, e); emptyList<com.anivers.anime.data.model.JadwalDay>()
                    }
                }

                val ongoing = ongoingDef.await()
                val baru = baruDef.await()
                val movies = movieDef.await()
                val rekom = rekomDef.await()
                val jadwal = jadwalDef.await()

                val top = rekom.sortedByDescending { it.score.toDoubleOrNull() ?: 0.0 }.take(10)
                    .ifEmpty { ongoing.take(10) }

                if (ongoing.isEmpty() && baru.isEmpty() && movies.isEmpty() && rekom.isEmpty()) {
                    val detail = if (errors.isNotEmpty()) errors.joinToString("\n") else "Semua endpoint 200 tapi data kosong"
                    android.util.Log.e("ANIVERS_HOME", "All empty, errors=$detail")
                    _state.value = HomeUiState(
                        loading = false,
                        error = "Gagal memuat data. Periksa koneksi.",
                        debugDetail = detail
                    )
                } else {
                    // jika sebagian gagal, tetap tampilkan yang berhasil tapi beri warning di log
                    if (errors.isNotEmpty()) android.util.Log.w("ANIVERS_HOME", "Partial fail: ${errors.joinToString()}")
                    _state.value = HomeUiState(
                        loading = false,
                        ongoing = ongoing,
                        baruUpload = baru,
                        movies = movies,
                        rekomendasi = rekom,
                        topAnime = top,
                        jadwal = jadwal,
                        debugDetail = if (errors.isNotEmpty()) errors.joinToString("\n") else null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ANIVERS_HOME", "load fatal", e)
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Unknown", debugDetail = e.stackTraceToString().take(600))
            }
        }
    }
}
