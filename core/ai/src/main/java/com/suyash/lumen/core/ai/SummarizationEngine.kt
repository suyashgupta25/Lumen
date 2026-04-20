package com.suyash.lumen.core.ai

import kotlinx.coroutines.flow.Flow

/**
 * The one abstraction the whole app depends on.
 *
 * Three implementations exist:
 *  - [MlKitGenAiEngine]   — high-level on-device, via ML Kit GenAI
 *  - [PromptApiEngine]    — low-level on-device, via ML Kit Prompt API
 *  - [CloudFallbackEngine] — network, for devices without AICore
 *
 * Feature code only ever sees this interface. Which implementation
 * arrives is decided by [EngineResolver] at call time.
 *
 * See: docs/rfc-01-summarization-engine.md
 */
interface SummarizationEngine {

    val id: EngineId
    val capabilities: EngineCapabilities

    fun summarize(request: SummarizeRequest): Flow<SummaryChunk>
    fun rewrite(request: RewriteRequest): Flow<SummaryChunk>
    fun ask(request: AskRequest): Flow<SummaryChunk>
}

enum class EngineId { ML_KIT_GENAI, PROMPT_API, CLOUD_GEMINI }

data class EngineCapabilities(
    val runsOnDevice: Boolean,
    val supportsStreaming: Boolean,
    val maxInputTokens: Int,
    val supportedModes: Set<SummaryMode>,
)

enum class SummaryMode { BULLETS, PARAGRAPH, KEY_QUOTES }

data class SummarizeRequest(val text: String, val mode: SummaryMode)
data class RewriteRequest(val text: String, val tone: RewriteTone)
data class AskRequest(val context: String, val question: String)

enum class RewriteTone { FORMAL, CASUAL, CONCISE, FRIENDLY }

sealed interface SummaryChunk {
    data class Text(val delta: String) : SummaryChunk
    data class Done(val fullText: String, val tokensUsed: Int) : SummaryChunk
    data class Error(val reason: EngineError) : SummaryChunk
}

sealed interface EngineError {
    data object DeviceNotSupported : EngineError
    data object ModelNotDownloaded : EngineError
    data object InputTooLong : EngineError
    data object Offline : EngineError
    data object QuotaExceeded : EngineError
    data class Unknown(val message: String?) : EngineError
}
