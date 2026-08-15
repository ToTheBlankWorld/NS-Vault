package com.nsvault.app.domain.repository

import com.nsvault.app.domain.model.PinLength
import com.nsvault.app.domain.model.PinVerification
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** Whether a vault PIN exists — decides setup vs. lock on launch. */
    val isPinSet: Flow<Boolean>

    /** Configured PIN length, for rendering the right number of dots. */
    val pinLength: Flow<PinLength?>

    val isBiometricEnabled: Flow<Boolean>

    /** Hash, seal, and persist a new PIN, resetting any lockout state. */
    suspend fun setPin(pin: String, length: PinLength)

    /**
     * Verify a PIN attempt, enforcing progressive lockout on repeated
     * failures. Never throws on a wrong PIN — only on storage/crypto
     * corruption.
     */
    suspend fun verifyPin(pin: String): PinVerification

    suspend fun setBiometricEnabled(enabled: Boolean)
}
