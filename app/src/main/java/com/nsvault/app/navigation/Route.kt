package com.nsvault.app.navigation

import kotlinx.serialization.Serializable

/**
 * Destinations inside the unlocked vault. PIN setup and the lock
 * screen are not routes — they render as an authentication layer
 * above the NavHost (see NSVaultApp).
 */
sealed interface Route {

    @Serializable
    data object Home : Route

    @Serializable
    data object Recorder : Route

    @Serializable
    data class Player(val recordingId: Long) : Route

    @Serializable
    data object Library : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object ChangePin : Route
}
