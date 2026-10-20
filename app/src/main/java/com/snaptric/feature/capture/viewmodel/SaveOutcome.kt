package com.snaptric.feature.capture.viewmodel

import com.snaptric.core.domain.insights.Insight
import com.snaptric.core.domain.insights.UsageComparison

/**
 * What the capture screen shows after the user saves a reading.
 */
sealed interface SaveOutcome {
    /** Nothing has been saved yet. */
    data object NotSaved : SaveOutcome

    /** The reading is saved and its usage is being explained. */
    data object Explaining : SaveOutcome

    /**
     * The reading is saved.
     *
     * @property comparison How usage compares with the meter's average, or null without enough history.
     * @property explanation The line explaining [comparison], or null when there is none.
     */
    data class Saved(
        val comparison: UsageComparison? = null,
        val explanation: Insight? = null
    ) : SaveOutcome
}
