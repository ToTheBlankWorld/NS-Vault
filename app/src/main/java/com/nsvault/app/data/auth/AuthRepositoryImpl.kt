package com.nsvault.app.data.auth

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nsvault.app.core.di.IoDispatcher
import com.nsvault.app.data.crypto.KeystoreManager
import com.nsvault.app.domain.model.PinLength
import com.nsvault.app.domain.model.PinVerification
import com.nsvault.app.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PIN record layout: a JSON envelope (salt, digest, iterations, length)
 * sealed with a hardware-backed Keystore key and stored as one opaque
 * Base64 string. Lockout counters are plain preferences — they are not
 * secrets, and keeping them outside the sealed record lets them update
 * without re-encryption.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val keystore: KeystoreManager,
    private val hasher: PinHasher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    @Serializable
    private data class PinRecord(
        val version: Int,
        val lengthDigits: Int,
        val saltB64: String,
        val hashB64: String,
        val iterations: Int,
    )

    override val isPinSet: Flow<Boolean> =
        dataStore.data.map { it[KEY_PIN_RECORD] != null }.distinctUntilChanged()

    override val pinLength: Flow<PinLength?> =
        dataStore.data.map { prefs ->
            prefs[KEY_PIN_LENGTH]?.let(PinLength::fromDigits)
        }.distinctUntilChanged()

    override val isBiometricEnabled: Flow<Boolean> =
        dataStore.data.map { it[KEY_BIOMETRIC] ?: false }.distinctUntilChanged()

    override suspend fun setPin(pin: String, length: PinLength): Unit = withContext(ioDispatcher) {
        val digest = hasher.hash(pin.toCharArray())
        val record = PinRecord(
            version = 1,
            lengthDigits = length.digits,
            saltB64 = Base64.encodeToString(digest.salt, Base64.NO_WRAP),
            hashB64 = Base64.encodeToString(digest.hash, Base64.NO_WRAP),
            iterations = digest.iterations,
        )
        val sealed = keystore.seal(
            KeystoreManager.ALIAS_PREFS,
            Json.encodeToString(record).encodeToByteArray(),
        )
        dataStore.edit { prefs ->
            prefs[KEY_PIN_RECORD] = Base64.encodeToString(sealed, Base64.NO_WRAP)
            prefs[KEY_PIN_LENGTH] = length.digits
            prefs[KEY_FAILED_ATTEMPTS] = 0
            prefs.remove(KEY_LOCKOUT_UNTIL)
        }
    }

    override suspend fun verifyPin(pin: String): PinVerification = withContext(ioDispatcher) {
        val prefs = dataStore.data.first()
        val now = System.currentTimeMillis()

        val lockedUntil = prefs[KEY_LOCKOUT_UNTIL] ?: 0L
        if (now < lockedUntil) {
            return@withContext PinVerification.LockedOut(lockedUntil - now)
        }

        val recordB64 = prefs[KEY_PIN_RECORD]
            ?: error("verifyPin called before a PIN was set")
        val record = Json.decodeFromString<PinRecord>(
            keystore.open(KeystoreManager.ALIAS_PREFS, Base64.decode(recordB64, Base64.NO_WRAP))
                .decodeToString(),
        )
        val matches = hasher.verify(
            pin = pin.toCharArray(),
            expected = PinHasher.Digest(
                salt = Base64.decode(record.saltB64, Base64.NO_WRAP),
                hash = Base64.decode(record.hashB64, Base64.NO_WRAP),
                iterations = record.iterations,
            ),
        )

        if (matches) {
            dataStore.edit {
                it[KEY_FAILED_ATTEMPTS] = 0
                it.remove(KEY_LOCKOUT_UNTIL)
            }
            PinVerification.Success
        } else {
            val attempts = (prefs[KEY_FAILED_ATTEMPTS] ?: 0) + 1
            val lockoutUntil = if (attempts >= FREE_ATTEMPTS) {
                val escalation = (attempts - FREE_ATTEMPTS).coerceAtMost(MAX_ESCALATION)
                now + (BASE_LOCKOUT_MS shl escalation).coerceAtMost(MAX_LOCKOUT_MS)
            } else {
                null
            }
            dataStore.edit { edit ->
                edit[KEY_FAILED_ATTEMPTS] = attempts
                lockoutUntil?.let { edit[KEY_LOCKOUT_UNTIL] = it }
            }
            if (lockoutUntil != null) {
                PinVerification.LockedOut(lockoutUntil - now)
            } else {
                PinVerification.Mismatch(attemptsRemaining = FREE_ATTEMPTS - attempts)
            }
        }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BIOMETRIC] = enabled }
    }

    companion object {
        private val KEY_PIN_RECORD = stringPreferencesKey("pin_record")
        private val KEY_PIN_LENGTH = intPreferencesKey("pin_length")
        private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        private val KEY_FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        private val KEY_LOCKOUT_UNTIL = longPreferencesKey("lockout_until")

        /** Wrong attempts allowed before lockouts begin. */
        private const val FREE_ATTEMPTS = 5
        private const val BASE_LOCKOUT_MS = 30_000L
        private const val MAX_LOCKOUT_MS = 300_000L
        private const val MAX_ESCALATION = 4
    }
}
