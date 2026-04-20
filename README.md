# Lumen

> On-device reading companion for Android. Summarizes articles, emails, and
> PDFs using Gemini Nano through ML Kit GenAI, with a cloud fallback for
> devices that don't support on-device inference.

**Status:** Work in progress. Scaffold + architecture landed; engine
implementations come online in Week 3.
See [`docs/rfc-01-summarization-engine.md`](docs/rfc-01-summarization-engine.md)
for the design rationale.

<!-- TODO: add a 60-second demo GIF here at the end of Week 2. -->
<!-- TODO: add screenshots of summarize, rewrite, and ask flows. -->

## Why this exists

Most Android apps that do AI features today call out to a cloud LLM on
every interaction. That means latency, cost, battery, and a privacy
footprint — often for tasks (summarize this article, rewrite this
message) that a modern on-device model can handle competently.

Lumen is a small, focused bet in the other direction: default to
on-device, fall back to cloud only when necessary, and make the
architectural seam explicit enough that a reviewer can inspect every
decision.

## Architecture at a glance

```
  Feature modules                        Core
  ┌───────────────────────┐        ┌───────────────────────────────┐
  │  feature-summarize    │        │        core-ai                │
  │  feature-rewrite      │◀──────▶│                               │
  │  feature-history      │        │  SummarizationEngine          │
  └───────────────────────┘        │  (interface)                  │
                                   │                               │
                                   │  ┌─────────┐  ┌────────────┐  │
                                   │  │ MlKit   │  │ PromptApi  │  │
                                   │  │ GenAi   │  │ Engine     │  │
                                   │  └─────────┘  └────────────┘  │
                                   │  ┌───────────────────┐        │
                                   │  │ CloudFallback     │        │
                                   │  └───────────────────┘        │
                                   │                               │
                                   │  EngineResolver picks one     │
                                   │  based on DeviceCapability    │
                                   └───────────────────────────────┘
```

Feature code depends on the `SummarizationEngine` interface and nothing
else. The `EngineResolver` is the single place that knows all three
implementations exist. See the RFC for the selection policy.

### Module layout

| Module                 | Purpose                                             |
|------------------------|-----------------------------------------------------|
| `app`                  | Hilt entry point, navigation, share-target intent   |
| `core-ai`              | `SummarizationEngine` + the three implementations   |
| `core-data`            | Room persistence (summary history)                  |
| `core-design`          | Material3 theme wrapper                             |
| `core-observability`   | `Telemetry` abstraction                             |
| `feature-summarize`    | Summarize flow (ViewModel + Compose screen)         |
| `feature-rewrite`      | *(Week 2)* Tone-shift flow                          |
| `feature-history`      | *(Week 2)* Past summaries                           |

## On-device vs cloud: the tradeoff

<!-- TODO: replace with real numbers measured on Galaxy S24 during Week 4. -->

| Dimension            | Gemini Nano (on-device) | Gemini API (cloud)   |
|----------------------|-------------------------|----------------------|
| First-token latency  | *tbd ms*                | *tbd ms*             |
| Tokens/sec           | *tbd*                   | *tbd*                |
| Cost per 1k requests | 0                       | *$tbd*               |
| Works offline        | Yes                     | No                   |
| Max input context    | *tbd (~4k tokens)*      | 32k+                 |
| Battery impact       | *tbd mAh / request*     | Network only         |
| Privacy              | Text never leaves device | Sent to Google Cloud |

## Tech stack

Kotlin · Jetpack Compose · Hilt · Coroutines + Flow · Room · Ktor ·
ML Kit GenAI (Summarization, Rewriting, Prompt APIs) · AI Edge SDK ·
JUnit + Turbine + MockK

## Running locally

1. Clone, open in Android Studio Ladybug (AGP 8.7+, JDK 17).
2. Plug in an **AICore-compatible device** — Galaxy S24+, Pixel 8+,
   or a more recent flagship. See the
   [current device list](https://developer.android.com/ai/gemini-nano).
3. Ensure AICore is enabled: `Settings → Developer options → AICore
   Settings → Enable On-Device GenAI Features`. First run will trigger
   a ~1 GB model download over Wi-Fi.
4. Run `app` from Android Studio.

### Unsupported device?

Lumen detects this at launch via `DeviceCapabilityChecker` and routes
all requests to `CloudFallbackEngine`. For cloud calls you'll need a
`GEMINI_PROXY_URL` pointing at your own thin backend that owns the
API key. Never bundle the key in the client.

## What's done

- [x] Multi-module Gradle setup with a version catalog
- [x] `SummarizationEngine` interface and three implementations (stubs)
- [x] `EngineResolver` + unit tests covering the selection policy
- [x] `InstrumentedEngine` decorator for uniform telemetry
- [x] Summarize screen with live streaming Flow plumbing
- [x] Share-target integration (Chrome → Lumen)
- [x] Room schema for summary history

## What's next

- [ ] **Week 3:** Real ML Kit GenAI Summarizer + Rewriter inside `MlKitGenAiEngine`
- [ ] **Week 3:** Real Prompt API integration inside `PromptApiEngine`, including `ask()` flow
- [ ] **Week 3:** Cloud proxy + streaming SSE client inside `CloudFallbackEngine`
- [ ] **Week 4:** Benchmarks screen with on-device vs cloud latency/battery numbers
- [ ] **Week 4:** Accompanying blog post and 60-second demo video

## What I learned

*(This section is what hiring managers actually read. Fill in honestly
at the end of Week 4 — 3 or 4 bullets, with specifics.)*

- *Placeholder: the actual hard part of wiring ML Kit GenAI was ...*
- *Placeholder: surprising latency finding was ...*
- *Placeholder: would do differently next time ...*

## License

MIT — see `LICENSE`. (Add when publishing.)
