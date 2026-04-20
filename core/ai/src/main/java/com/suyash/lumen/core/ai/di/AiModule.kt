package com.suyash.lumen.core.ai.di

import com.suyash.lumen.core.ai.CloudFallbackEngine
import com.suyash.lumen.core.ai.EngineId
import com.suyash.lumen.core.ai.MlKitGenAiEngine
import com.suyash.lumen.core.ai.PromptApiEngine
import com.suyash.lumen.core.ai.SummarizationEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

/**
 * Multibinds the three engines into a Map<EngineId, SummarizationEngine>
 * that EngineResolver consumes.
 *
 * Adding a fourth engine is a one-line change here — no feature module
 * ever needs to know about it.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @IntoMap
    @EngineIdKey(EngineId.ML_KIT_GENAI)
    abstract fun bindMlKit(impl: MlKitGenAiEngine): SummarizationEngine

    @Binds
    @IntoMap
    @EngineIdKey(EngineId.PROMPT_API)
    abstract fun bindPromptApi(impl: PromptApiEngine): SummarizationEngine

    @Binds
    @IntoMap
    @EngineIdKey(EngineId.CLOUD_GEMINI)
    abstract fun bindCloud(impl: CloudFallbackEngine): SummarizationEngine
}
