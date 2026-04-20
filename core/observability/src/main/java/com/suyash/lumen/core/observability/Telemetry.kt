package com.suyash.lumen.core.observability

/**
 * The boundary between app code and whatever telemetry backend we pick.
 *
 * Keep this interface boring: emit structured events, let the
 * implementation decide whether they go to Logcat, Firebase, Datadog,
 * or all three.
 */
interface Telemetry {
    fun event(name: String, attributes: Map<String, Any?> = emptyMap())
    fun error(message: String, throwable: Throwable? = null, attributes: Map<String, Any?> = emptyMap())
}

/**
 * Default impl just logs. Swap for a Crashlytics/Datadog-backed impl
 * in a release-only DI module later.
 */
class LogcatTelemetry : Telemetry {
    override fun event(name: String, attributes: Map<String, Any?>) {
        android.util.Log.i("Lumen", "event=$name attrs=$attributes")
    }
    override fun error(message: String, throwable: Throwable?, attributes: Map<String, Any?>) {
        android.util.Log.e("Lumen", "error=$message attrs=$attributes", throwable)
    }
}
