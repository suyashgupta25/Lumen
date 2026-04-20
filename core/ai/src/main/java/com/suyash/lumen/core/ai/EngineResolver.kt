package com.suyash.lumen.core.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Selection policy (from RFC-01):
 *   1. If user has pinned an engine (debug), honor it.
 *   2. If AICore is AVAILABLE and request is summarize/rewrite,
 *      use MlKitGenAiEngine.
 *   3. If AICore is AVAILABLE and request needs custom prompts (ask),
 *      use PromptApiEngine.
 *   4. Otherwise, use CloudFallbackEngine.
 */
@Singleton
class EngineResolver @Inject constructor(
    private val deviceCapability: DeviceCapabilityChecker,
    private val engines: Map<EngineId, @JvmSuppressWildcards SummarizationEngine>,
) {

    suspend fun resolve(feature: Feature): SummarizationEngine {
        val status = deviceCapability.aiCoreStatus()

        return when {
            status == AiCoreStatus.AVAILABLE && feature.needsCustomPrompt ->
                engines.require(EngineId.PROMPT_API)

            status == AiCoreStatus.AVAILABLE ->
                engines.require(EngineId.ML_KIT_GENAI)

            else ->
                engines.require(EngineId.CLOUD_GEMINI)
        }
    }

    enum class Feature(val needsCustomPrompt: Boolean) {
        SUMMARIZE(needsCustomPrompt = false),
        REWRITE(needsCustomPrompt = false),
        ASK(needsCustomPrompt = true),
    }

    private fun Map<EngineId, SummarizationEngine>.require(id: EngineId): SummarizationEngine =
        get(id) ?: error("Engine $id not registered. Check AiModule.")
}
