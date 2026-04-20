package com.suyash.lumen.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level on-device engine using ML Kit Prompt API (Gemini Nano).
 *
 * Docs: https://developers.google.com/ml-kit/genai/prompt/android/get-started
 *
 * Use this when we need custom prompts — primarily Q&A over arbitrary
 * text. For stock summarization/rewriting, MlKitGenAiEngine is preferred
 * because its LoRA adapters give more consistent quality.
 */
@Singleton
class PromptApiEngine @Inject constructor(
    // TODO(Week 3): inject GenerativeModelFutures / GenerativeModel client.
) : SummarizationEngine {

    override val id = EngineId.PROMPT_API

    override val capabilities = EngineCapabilities(
        runsOnDevice = true,
        supportsStreaming = true,
        maxInputTokens = 4096, // confirm against device
        supportedModes = setOf(SummaryMode.BULLETS, SummaryMode.PARAGRAPH, SummaryMode.KEY_QUOTES),
    )

    override fun summarize(request: SummarizeRequest): Flow<SummaryChunk> = flow {
        // TODO(Week 3): build a prompt from SummaryMode, call generateContentStream,
        // map chunks to SummaryChunk.
        emit(SummaryChunk.Text("[TODO: wire Prompt API]"))
        emit(SummaryChunk.Done(fullText = "[stub]", tokensUsed = 0))
    }

    override fun rewrite(request: RewriteRequest): Flow<SummaryChunk> = flow {
        // TODO(Week 3): prompt-based rewrite with tone instruction.
        emit(SummaryChunk.Error(EngineError.Unknown("Not yet implemented")))
    }

    override fun ask(request: AskRequest): Flow<SummaryChunk> = flow {
        // TODO(Week 3): this is the primary use case for this engine.
        // Prompt shape:
        //   "Based on the following context, answer the question.
        //    Context: {context}
        //    Question: {question}"
        // Stream the response via generateContentStream().
        emit(SummaryChunk.Text("[TODO: wire Prompt API ask()]"))
        emit(SummaryChunk.Done(fullText = "[stub]", tokensUsed = 0))
    }
}
