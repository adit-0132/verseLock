package com.verselock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verselock.data.model.LyricsResult
import com.verselock.data.model.TrackInfo
import com.verselock.service.LyricsService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LockScreenViewModel : ViewModel() {

    val currentTrack: StateFlow<TrackInfo?> = LyricsService.currentTrack
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentLyricIndex: StateFlow<Int> = LyricsService.currentLyricIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    val lyricsResult: StateFlow<LyricsResult> = LyricsService.lyricsResult
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LyricsResult.Loading)

    val isPlaying: StateFlow<Boolean> = LyricsService.isPlaying
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}
