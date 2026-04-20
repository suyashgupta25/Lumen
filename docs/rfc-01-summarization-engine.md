# RFC-01: SummarizationEngine Abstraction

| Field    | Value                                   |
|----------|-----------------------------------------|
| Author   | Suyash Gupta                            |
| Status   | Accepted                                |
| Created  | 2026-04-20                              |
| Updated  | 2026-04-20                              |
| Reviewers| *(self — portfolio project)*            |

## TL;DR

Lumen needs to produce summaries from three different sources — ML Kit
GenAI (high-level, on-device), the AI Edge SDK Prompt API (low-level,
on-device), and the cloud Gemini API (fallback). Calling these directly
from feature code would couple UI flows to transport details and make
future swaps expensive.

This RFC proposes a single `SummarizationEngine` interface with three
implementations and a runtime resolver that picks the right one per
device. The feature layer never sees which engine is active.

**Recommendation:** Adopt the abstraction now, in Week 3, before the
cloud fallback and Prompt API work lands. Doing it later means a painful
refactor across the feature modules.

## Context

Lumen is an on-device-first reading companion. The core flow is: user
provides text → app returns a summary in one of three modes (bullets,
paragraph, key quotes). Secondary flows (rewrite, Q&A over an article)
share the same underlying need: send text to a model, stream a response
back.

Three facts shape the design:

1. **Gemini Nano is not available on every Android device.** AICore is
   gated by hardware — currently Pixel 8+/9+, Galaxy S24+, and select
   flagships. Roughly 140M devices globally, which is meaningful but
   still a minority of the Android install base. Lumen must degrade
   gracefully on unsupported devices, not refuse to launch.

2. **ML Kit GenAI covers the common cases; the Prompt API covers
   everything else.** Summarization, rewrite, and proofreading have
   dedicated ML Kit APIs with opinionated defaults. Q&A over arbitrary
   text, custom instructions, and structured output require dropping
   down to the AI Edge SDK Prompt API directly. Both run against the
   same Gemini Nano underneath, via AICore.

3. **Cloud Gemini is a different beast.** Different latency profile
   (network-bound, not hardware-bound), different cost model (per-token
   billing vs. free on-device), different failure modes (offline,
   rate-limited, quota-exhausted). Pretending it's interchangeable
   with the on-device engines is a mistake; acknowledging the
   differences inside the abstraction is the whole point.

Without an abstraction, each feature (summarize, rewrite, Q&A) would
contain its own branching logic for engine selection, its own error
handling, and its own fallback policy. That's three times the surface
area and three places to break.

## Goals

- Feature modules depend on one interface, not three SDKs.
- Engine selection happens in one place, based on device capability and
  feature requirements — not scattered across ViewModels.
- Adding a fourth engine later (e.g., Gemma 4 preview, a partner model,
  a local LLM runtime) is a one-class change, not a cross-cutting one.
- Streaming is first-class, not retrofitted. All engines expose
  `Flow<SummaryChunk>`, cancellable from the caller.
- Observability is uniform — every call emits the same shape of
  telemetry (engine used, latency, token count, outcome) regardless of
  which engine handled it.

## Non-goals

- **Model training or fine-tuning.** Lumen uses models as shipped.
- **Prompt engineering library.** Prompts live with the features that
  own them, not in the engine layer.
- **A general LLM SDK.** This abstraction is scoped to Lumen's needs.
  I'm deliberately not trying to build "LangChain for Android."
- **Perfect engine parity.** The cloud engine will have capabilities
  the on-device engines don't (larger context, better quality on hard
  inputs). The interface exposes what's common; callers can query for
  capabilities when they need to specialize.

## Proposed design

### The interface

```kotlin
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

sealed interface SummaryChunk {
    data class Text(val delta: String) : SummaryChunk
    data class Done(val fullText: String, val tokensUsed: Int) : SummaryChunk
    data class Error(val reason: EngineError) : SummaryChunk
}
```

Three design choices worth calling out:

- **`Flow<SummaryChunk>` everywhere.** Even engines that don't truly
  stream (early ML Kit GenAI responses arrive in one chunk) adapt to
  this shape. Callers never special-case streaming vs. non-streaming.
- **`SummaryChunk` is a sealed type, not an exception channel.**
  Errors flow through the stream. This composes better with UI state
  and is simpler to test than `try/catch` around a `collect`.
- **`EngineCapabilities` is public.** Features that need to specialize
  (e.g., "only offer PDF import if `maxInputTokens >= 8k`") query
  capabilities rather than sniffing engine IDs.

### The resolver

```kotlin
class EngineResolver @Inject constructor(
    private val deviceCapability: DeviceCapabilityChecker,
    private val engines: Map<EngineId, @JvmSuppressWildcards SummarizationEngine>,
    private val userPreferences: UserPreferences,
) {
    fun resolve(request: EngineRequest): SummarizationEngine { ... }
}
```

Selection policy, in priority order:

1. If the user has explicitly pinned an engine (debug builds / power
   users), honor it.
2. If the device supports AICore and the request is a supported ML Kit
   GenAI task (summarize, rewrite), use `MlKitGenAiEngine`.
3. If the device supports AICore and the request needs custom prompts
   (ask), use `PromptApiEngine`.
4. Otherwise, use `CloudFallbackEngine`.

The resolver is the *only* class that knows all three engines exist.
Feature code gets a `SummarizationEngine` injected and doesn't care
which one arrived.

### Capability detection

`DeviceCapabilityChecker` wraps the AICore availability check from the
AI Edge SDK. It caches the result for the app session — AICore status
doesn't change between launches in practice, and repeated checks are
wasteful.

On first launch with an unsupported device, we show a one-time
explainer: "Your device can't run on-device AI; Lumen will use secure
cloud processing instead." This is a deliberate UX choice, not a
silent degradation.

### Observability

Every engine call is wrapped by an `InstrumentedEngine` decorator that
emits a structured event with:

- `engineId`, `feature` (summarize / rewrite / ask), `mode`
- `inputTokens` (estimated), `outputTokens`, `durationMs`
- `outcome` (success / user_cancelled / error_<type>)
- `deviceModel`, `aicoreVersion` (when applicable)

This is the single biggest lesson I'm carrying forward from Deliveroo:
if you don't instrument the abstraction at the boundary, you'll spend
the next six months reverse-engineering production issues from crash
reports.

## Alternatives considered

### 1. No abstraction; call SDKs directly from feature code

**Pros:** Zero upfront design cost. Easier to understand for someone
reading a single feature in isolation.

**Cons:** Every feature reimplements engine selection, error handling,
and fallback. Adding cloud fallback in Week 3 becomes a three-file
change instead of one. Testing requires mocking three SDKs instead of
one interface. Rejected — the cost of not abstracting compounds with
every feature added.

### 2. One engine per feature (SummarizeEngine, RewriteEngine, AskEngine)

**Pros:** Each interface is narrower and easier to reason about.

**Cons:** Engine selection logic gets duplicated three times. Device
capability checks get duplicated three times. The three features are
doing variations of the same thing (text in, streaming text out) —
splitting the interface splits what should be shared.
Rejected — premature separation along the wrong axis.

### 3. Use a third-party abstraction (LangChain4j, Koog, etc.)

**Pros:** Don't build what already exists. Possibly richer feature set.

**Cons:** Additional dependency, additional abstractions to learn,
most don't yet support ML Kit GenAI or the AI Edge SDK as first-class
targets. The surface area of Lumen's needs is small enough that a
20-line interface beats a 200-class dependency.
Rejected for this project, but worth revisiting if Lumen grows
beyond three engines.

### 4. Make the cloud engine the default, with on-device as an optimization

**Pros:** Simpler engine selection (cloud works everywhere). Better
quality on hard inputs.

**Cons:** Contradicts the entire product thesis. Lumen's pitch is
"on-device first, cloud only when necessary." Defaulting to cloud
means battery, latency, cost, and privacy costs for every user on a
capable device. Rejected — this is a product decision, not an
engineering one, and the product decision is already made.

## Risks and mitigations

**Risk: AICore API surface is still evolving.** Gemma 4 / Gemini Nano 4
preview landed three weeks before this RFC was written. Breaking
changes are plausible.
*Mitigation:* Isolate AICore-specific code to the two on-device engine
implementations. Keep the public `SummarizationEngine` interface
stable. If AICore breaks, we update two classes, not the whole app.

**Risk: Streaming semantics differ between engines.** ML Kit GenAI,
the Prompt API, and cloud Gemini all stream differently.
*Mitigation:* The `SummaryChunk` type normalizes this. Engines that
don't truly stream emit a single `Text` chunk followed by `Done`.
Engines that stream token-by-token emit many `Text` chunks. Callers
see uniform shape.

**Risk: Cloud fallback becomes a cost surprise.** Even in a portfolio
project, an accidental infinite loop through the cloud engine could
cost real money.
*Mitigation:* Per-session token budget on the cloud engine, enforced
at the engine layer (not trusted to feature code). Hard cap of 50k
tokens per session in debug; configurable in production.

**Risk: Over-abstraction.** A 20-line interface today becomes a
200-line nightmare if every feature adds its own optional parameter.
*Mitigation:* Features extend via `capabilities`, not interface bloat.
New methods require RFC amendment. Resist adding `summarizeV2`.

## Rollout

Since this is a greenfield project, "rollout" is really migration
within the codebase:

1. Week 3 Day 1: Land the interface and `MlKitGenAiEngine` (extracted
   from the direct-call code in Week 2).
2. Week 3 Day 2-3: Add `PromptApiEngine`, migrate the ask flow to use
   the resolver.
3. Week 3 Day 4-5: Add `CloudFallbackEngine` and device capability
   gating. End of week: no feature module imports an AI SDK directly.
4. Week 4: Instrumentation decorator, benchmarks, documentation.

## Open questions

- Should cached summaries (Room) be engine-tagged, so we can
  invalidate them when the on-device model version changes? *Leaning
  yes, but will decide when the cache layer lands.*
- Do we expose engine choice to end users as a setting, or keep it
  debug-only? *Debug-only for v1. Exposing it to end users without a
  clear reason creates a support surface.*
- How do we benchmark fairly when on-device latency depends on
  thermal state? *Run benchmarks cold and warm; report both.*

## References

- AI Edge SDK / AICore docs: developer.android.com/ai/gemini-nano
- ML Kit GenAI APIs: developer.android.com/ai/gemini-nano/ml-kit-genai
- Android Developers Blog on Gemma 4 / Gemini Nano 4, April 2026
- Lumen product brief (this repo, `/docs/product-brief.md`)
