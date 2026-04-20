package com.suyash.lumen.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level on-device engine using ML Kit GenAI APIs.
 *
 * Docs:
 *  - Summarization: https://developers.google.com/ml-kit/genai/summarization/android
 *  - Rewriting:     https://developers.google.com/ml-kit/genai/rewriting/android
 *
 * Capabilities: summarize (bullets), rewrite (tone). Does NOT support
 * custom prompts — the PromptApiEngine handles ask() flows instead.
 */
@Singleton
class MlKitGenAiEngine @Inject constructor(
    // TODO(Week 3): inject Summarizer + Rewriter clients here.
    // Prefer constructing them in a Hilt provider so they can be mocked.
) : SummarizationEngine {

    override val id = EngineId.ML_KIT_GENAI

    override val capabilities = EngineCapabilities(
        runsOnDevice = true,
        supportsStreaming = true,
        maxInputTokens = 4096, // approximate; confirm with device tests
        supportedModes = setOf(SummaryMode.BULLETS, SummaryMode.PARAGRAPH),
    )

    override fun summarize(request: SummarizeRequest): Flow<SummaryChunk> = flow {
        // TODO(Week 3): real implementation follows this shape:
        //
        //   1. val options = SummarizerOptions.builder(context)
        //                      .setOutputType(OutputType.THREE_BULLETS)
        //                      .build()
        //   2. val summarizer = Summarization.getClient(options)
        //   3. Call summarizer.checkFeatureStatus() — if DOWNLOADABLE,
        //      trigger prepareInferenceEngine() and collect the download
        //      Flow before proceeding.
        //   4. summarizer.runInference(SummarizationRequest.builder(text).build())
        //      returns a Flow<StreamingResponse>. Map each delta to
        //      SummaryChunk.Text, then emit SummaryChunk.Done.
        //   5. Remember to close(summarizer) when done — leaking is a real
        //      memory hazard because the model is huge.
        //
        // Emit a stub so the UI flow compiles end-to-end in Week 1.
        emit(SummaryChunk.Text("[TODO: wire ML Kit Summarizer here]"))
        emit(SummaryChunk.Done(fullText = "[stub]", tokensUsed = 0))
    }

    override fun rewrite(request: RewriteRequest): Flow<SummaryChunk> = flow {
        // TODO(Week 3): implement via Rewriting API, same shape as summarize.
        emit(SummaryChunk.Error(EngineError.Unknown("Rewrite not yet implemented")))
    }

    override fun ask(request: AskRequest): Flow<SummaryChunk> = flow {
        // ML Kit GenAI doesn't expose arbitrary Q&A — this engine
        // deliberately declines. The resolver routes ask() to PromptApiEngine.
        emit(SummaryChunk.Error(EngineError.Unknown("ask() is handled by PromptApiEngine")))
    }
}
