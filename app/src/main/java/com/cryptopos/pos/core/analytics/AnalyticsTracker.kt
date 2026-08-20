package com.cryptopos.pos.core.analytics

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface AnalyticsTracker {
    fun screen(name: String)
    fun event(name: String, params: Map<String, String> = emptyMap())
}

/**
 * Timber-backed tracker. Swap for FirebaseAnalyticsTracker without touching call sites.
 */
@Singleton
class TimberAnalyticsTracker @Inject constructor() : AnalyticsTracker {
    override fun screen(name: String) {
        Timber.i("analytics_screen name=%s", name)
    }

    override fun event(name: String, params: Map<String, String>) {
        Timber.i("analytics_event name=%s params=%s", name, params)
    }
}
