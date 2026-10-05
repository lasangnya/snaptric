package com.snaptric.navigation

import androidx.navigation.NavBackStackEntry
import com.snaptric.feature.capture.viewmodel.CaptureViewModel

/**
 * The camera screen's route. It optionally carries the property and meter the user started from,
 * so the confirmation sheet can pre-select them.
 */
object CaptureRoute {
    const val PATTERN =
        "capture?${CaptureViewModel.ARG_PROPERTY_ID}={${CaptureViewModel.ARG_PROPERTY_ID}}" +
            "&${CaptureViewModel.ARG_UTILITY_ID}={${CaptureViewModel.ARG_UTILITY_ID}}"

    /** Value used when no property or meter is given. */
    const val NONE = -1L

    fun to(propertyId: Long? = null, utilityId: Long? = null): String =
        "capture?${CaptureViewModel.ARG_PROPERTY_ID}=${propertyId ?: NONE}" +
            "&${CaptureViewModel.ARG_UTILITY_ID}=${utilityId ?: NONE}"

    /** The camera route for the screen the user is on: its property or meter, if it has one. */
    fun from(entry: NavBackStackEntry?): String {
        val args = entry?.arguments
        val route = entry?.destination?.route.orEmpty()
        return when {
            route.startsWith("property_detail") -> to(propertyId = args?.getLong("propertyId"))
            route.startsWith("utility_detail") -> to(utilityId = args?.getLong("utilityId"))
            else -> to()
        }
    }

    fun isCapture(route: String?) = route?.startsWith("capture") == true
}
