package com.suyash.lumen.core.ai

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EngineResolverTest {

    private fun engineOf(engineId: EngineId): SummarizationEngine =
        object : SummarizationEngine {
            override val id = engineId
            override val capabilities = EngineCapabilities(
                runsOnDevice = engineId != EngineId.CLOUD_GEMINI,
                supportsStreaming = true,
                maxInputTokens = 4096,
                supportedModes = setOf(SummaryMode.BULLETS),
            )
            override fun summarize(request: SummarizeRequest) = emptyFlow<SummaryChunk>()
            override fun rewrite(request: RewriteRequest) = emptyFlow<SummaryChunk>()
            override fun ask(request: AskRequest) = emptyFlow<SummaryChunk>()
        }

    private val engines = mapOf(
        EngineId.ML_KIT_GENAI to engineOf(EngineId.ML_KIT_GENAI),
        EngineId.PROMPT_API to engineOf(EngineId.PROMPT_API),
        EngineId.CLOUD_GEMINI to engineOf(EngineId.CLOUD_GEMINI),
    )

    @Test
    fun `summarize on AICore device uses ML Kit GenAI`() = runTest {
        val checker = mockk<DeviceCapabilityChecker>()
        coEvery { checker.aiCoreStatus() } returns AiCoreStatus.AVAILABLE

        val resolver = EngineResolver(checker, engines)
        val engine = resolver.resolve(EngineResolver.Feature.SUMMARIZE)

        assertEquals(EngineId.ML_KIT_GENAI, engine.id)
    }

    @Test
    fun `ask on AICore device uses Prompt API`() = runTest {
        val checker = mockk<DeviceCapabilityChecker>()
        coEvery { checker.aiCoreStatus() } returns AiCoreStatus.AVAILABLE

        val resolver = EngineResolver(checker, engines)
        val engine = resolver.resolve(EngineResolver.Feature.ASK)

        assertEquals(EngineId.PROMPT_API, engine.id)
    }

    @Test
    fun `unsupported device falls back to cloud for all features`() = runTest {
        val checker = mockk<DeviceCapabilityChecker>()
        coEvery { checker.aiCoreStatus() } returns AiCoreStatus.UNAVAILABLE

        val resolver = EngineResolver(checker, engines)

        assertEquals(EngineId.CLOUD_GEMINI, resolver.resolve(EngineResolver.Feature.SUMMARIZE).id)
        assertEquals(EngineId.CLOUD_GEMINI, resolver.resolve(EngineResolver.Feature.REWRITE).id)
        assertEquals(EngineId.CLOUD_GEMINI, resolver.resolve(EngineResolver.Feature.ASK).id)
    }

    @Test
    fun `unknown device state treats as unavailable and falls back to cloud`() = runTest {
        val checker = mockk<DeviceCapabilityChecker>()
        coEvery { checker.aiCoreStatus() } returns AiCoreStatus.UNKNOWN

        val resolver = EngineResolver(checker, engines)
        val engine = resolver.resolve(EngineResolver.Feature.SUMMARIZE)

        assertEquals(EngineId.CLOUD_GEMINI, engine.id)
    }
}
