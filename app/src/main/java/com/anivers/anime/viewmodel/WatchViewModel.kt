package com.anivers.anime.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anivers.anime.data.model.EpisodeDataResponse
import com.anivers.anime.data.model.SeriesDetail
import com.anivers.anime.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WatchViewModel(private val repo: AnimeRepository = AnimeRepository()) : ViewModel() {
    private val _stream = MutableStateFlow<EpisodeDataResponse?>(null)
    val stream: StateFlow<EpisodeDataResponse?> = _stream
    private val _series = MutableStateFlow<SeriesDetail?>(null)
    val series: StateFlow<SeriesDetail?> = _series
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _quality = MutableStateFlow("720p")
    val quality: StateFlow<String> = _quality

    fun load(seriesUrl: String, postUrl: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val ep = repo.getEpisodeData(postUrl, seriesUrl)
                _stream.value = ep
                val s = repo.getSeries(seriesUrl)
                _series.value = s
                // pick default quality exists
                val resos = ep.data?.firstOrNull()?.reso ?: emptyList()
                if (resos.isNotEmpty() && !_quality.value.let { resos.contains(it) }) {
                    _quality.value = when {
                        resos.contains("720p") -> "720p"
                        else -> resos.first()
                    }
                }
            } catch (e: Exception) { _error.value = e.message }
            _loading.value = false
        }
    }

    fun setQuality(q: String) { _quality.value = q }
}
