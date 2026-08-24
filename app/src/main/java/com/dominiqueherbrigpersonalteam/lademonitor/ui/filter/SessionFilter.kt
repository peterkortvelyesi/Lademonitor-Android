package com.dominiqueherbrigpersonalteam.lademonitor.ui.filter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Global date filter for Dashboard / Sessions / Map — one shared state (port of the iOS
 * `SessionFilter`) so the selection survives tab switches. Deliberately not persisted: it only
 * applies to the running session.
 */
object SessionFilter {
    private val _dateRange = MutableStateFlow<LongRange?>(null)
    val dateRange: StateFlow<LongRange?> = _dateRange

    val isActive: Boolean get() = _dateRange.value != null

    fun setDateRange(range: LongRange?) {
        _dateRange.value = range
    }

    fun clear() {
        _dateRange.value = null
    }
}
