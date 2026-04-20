package com.suyash.lumen.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud fallback — used when the device can't run AICore.
 *
 * Implementation note: the responsible way to ship this is NOT to call
 * Gemini API directly from the client with a bundled key. Instead, call
 * a thin backend proxy that owns the key and enforces per-user quotas.
 * For a portfolio project, a Cloud Run function is enough.
 *
 * The hard per-session token budget lives HERE, not in feature code.
 */
@Singleton
class CloudFallbackEngine @Inject constructor(
    // TODO(Week 3): inject Ktor HttpClient and a config object with the
    // proxy endpoint URL.
) : SummarizationEngine {

    override val id = EngineId.CLOUD_GEMINI

    override val capabilities = EngineCapabilities(
        runsOnDevice = false,
        supportsStreaming = true, // SSE-based streaming
        maxInputTokens = 32_000,  // cloud has much larger context
        supportedModes = setOf(SummaryMode.BULLETS, SummaryMode.PARAGRAPH, SummaryMode.KEY_QUOTES),
    )

    // Enforced at this layer — feature code never sees it.
    private val sessionTokenBudget = 50_000
    private var tokensUsedThisSession = 0

    override fun summarize(request: SummarizeRequest): Flow<SummaryChunk> = flow {
        if (tokensUsedThisSession >= sessionTokenBudget) {
            emit(SummaryChunk.Error(EngineError.QuotaExceeded))
            return@flow
        }
        // TODO(Week 3): POST to backend proxy, stream SSE response,
        // map to SummaryChunk. Track tokens used.
        emit(SummaryChunk.Text("[TODO: wire cloud proxy]"))
        emit(SummaryChunk.Done(fullText = "[stub]", tokensUsed = 0))
    }

    override fun rewrite(request: RewriteRequest): Flow<SummaryChunk> = flow {
        emit(SummaryChunk.Error(EngineError.Unknown("Not yet implemented")))
    }

    override fun ask(request: AskRequest): Flow<SummaryChunk> = flow {
        emit(SummaryChunk.Error(EngineError.Unknown("Not yet implemented")))
    }
}
