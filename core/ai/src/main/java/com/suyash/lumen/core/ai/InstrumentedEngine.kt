package com.suyash.lumen.core.ai

import com.suyash.lumen.core.observability.Telemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * Wraps any SummarizationEngine and emits uniform telemetry.
 *
 * This is the "one place" the RFC talks about — wire new engines
 * through here and observability is automatic.
 */
class InstrumentedEngine(
    private val delegate: SummarizationEngine,
    private val telemetry: Telemetry,
) : SummarizationEngine by delegate {

    override fun summarize(request: SummarizeRequest): Flow<SummaryChunk> =
        delegate.summarize(request).instrument(feature = "summarize", mode = request.mode.name)

    override fun rewrite(request: RewriteRequest): Flow<SummaryChunk> =
        delegate.rewrite(request).instrument(feature = "rewrite", mode = request.tone.name)

    override fun ask(request: AskRequest): Flow<SummaryChunk> =
        delegate.ask(request).instrument(feature = "ask", mode = "-")

    private fun Flow<SummaryChunk>.instrument(feature: String, mode: String): Flow<SummaryChunk> {
        val startedAt = System.currentTimeMillis()
        var tokens = 0
        var outcome = "unknown"

        return this
            .onStart {
                telemetry.event(
                    "engine.call.start",
                    mapOf("engineId" to delegate.id.name, "feature" to feature, "mode" to mode),
                )
            }
            .onCompletion { cause ->
                if (cause != null) outcome = "error:${cause.javaClass.simpleName}"
                telemetry.event(
                    "engine.call.end",
                    mapOf(
                        "engineId" to delegate.id.name,
                        "feature" to feature,
                        "mode" to mode,
                        "durationMs" to (System.currentTimeMillis() - startedAt),
                        "tokensUsed" to tokens,
                        "outcome" to outcome,
                    ),
                )
            }
    }
}
