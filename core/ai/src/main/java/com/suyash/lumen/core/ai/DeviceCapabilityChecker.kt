package com.suyash.lumen.core.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the AICore availability check.
 *
 * The result is cached for the app session — AICore status doesn't
 * change between launches in practice, and repeated checks are wasteful.
 */
@Singleton
class DeviceCapabilityChecker @Inject constructor() {

    @Volatile
    private var cached: AiCoreStatus? = null

    suspend fun aiCoreStatus(): AiCoreStatus {
        cached?.let { return it }

        // TODO(Week 3): query real status via
        //   GenerativeModelFutures.from(Generation.INSTANCE.getClient())
        //   and map { AVAILABLE, DOWNLOADABLE, UNAVAILABLE } to this enum.
        //
        // The ML Kit Prompt API quickstart shows the exact call:
        // https://developers.google.com/ml-kit/genai/prompt/android/get-started
        //
        // Keep the mapping in this class; don't leak ML Kit types outward.

        val result = AiCoreStatus.UNKNOWN
        cached = result
        return result
    }
}

enum class AiCoreStatus {
    AVAILABLE,       // Gemini Nano is ready; use on-device engines
    DOWNLOADABLE,    // Device supports it; model not yet downloaded
    UNAVAILABLE,     // Device cannot run AICore; use cloud fallback
    UNKNOWN,         // Haven't checked yet, or check failed
}
