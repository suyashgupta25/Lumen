package com.suyash.lumen.feature.summarize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suyash.lumen.core.ai.EngineResolver
import com.suyash.lumen.core.ai.SummarizeRequest
import com.suyash.lumen.core.ai.SummaryChunk
import com.suyash.lumen.core.ai.SummaryMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SummarizeUiState(
    val inputText: String = "",
    val mode: SummaryMode = SummaryMode.BULLETS,
    val output: String = "",
    val isRunning: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SummarizeViewModel @Inject constructor(
    private val resolver: EngineResolver,
) : ViewModel() {

    private val _state = MutableStateFlow(SummarizeUiState())
    val state: StateFlow<SummarizeUiState> = _state.asStateFlow()

    private var inFlight: Job? = null

    fun onInputChanged(text: String) = _state.update { it.copy(inputText = text) }

    fun onModeChanged(mode: SummaryMode) = _state.update { it.copy(mode = mode) }

    fun onSummarizeClicked() {
        inFlight?.cancel()
        val snapshot = _state.value
        if (snapshot.inputText.isBlank()) return

        inFlight = viewModelScope.launch {
            _state.update { it.copy(isRunning = true, output = "", error = null) }

            val engine = resolver.resolve(EngineResolver.Feature.SUMMARIZE)
            val request = SummarizeRequest(text = snapshot.inputText, mode = snapshot.mode)

            engine.summarize(request).collect { chunk ->
                when (chunk) {
                    is SummaryChunk.Text -> _state.update { it.copy(output = it.output + chunk.delta) }
                    is SummaryChunk.Done -> _state.update { it.copy(isRunning = false) }
                    is SummaryChunk.Error -> _state.update {
                        it.copy(isRunning = false, error = chunk.reason.toString())
                    }
                }
            }
        }
    }

    fun onCancelClicked() {
        inFlight?.cancel()
        _state.update { it.copy(isRunning = false) }
    }
}
