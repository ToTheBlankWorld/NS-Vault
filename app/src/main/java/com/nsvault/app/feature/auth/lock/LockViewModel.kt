package com.nsvault.app.feature.auth.lock

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsvault.app.data.auth.SessionManager
import com.nsvault.app.domain.model.PinLength
import com.nsvault.app.domain.model.PinVerification
import com.nsvault.app.domain.repository.AuthRepository
import com.nsvault.app.domain.usecase.VerifyPinUseCase
import com.nsvault.app.feature.auth.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class LockViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val verifyPin: VerifyPinUseCase,
    private val sessionManager: SessionManager,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

    data class UiState(
        val pinLength: PinLength = PinLength.FOUR,
        val enteredCount: Int = 0,
        val errorPulse: Int = 0,
        val message: String? = null,
        val messageIsError: Boolean = false,
        val lockoutRemainingSeconds: Long = 0,
        val biometricEnabled: Boolean = false,
        val verifying: Boolean = false,
        val success: Boolean = false,
    ) {
        val inputEnabled: Boolean
            get() = !verifying && !success && lockoutRemainingSeconds == 0L
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    val biometricHardwareAvailable: Boolean
        get() = biometricAuthenticator.isAvailable

    private val buffer = StringBuilder()
    private var lockoutJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.pinLength.collect { length ->
                if (length != null) {
                    _uiState.update { it.copy(pinLength = length) }
                }
            }
        }
        viewModelScope.launch {
            authRepository.isBiometricEnabled.collect { enabled ->
                _uiState.update { it.copy(biometricEnabled = enabled) }
            }
        }
    }

    fun onDigit(digit: Int) {
        val state = _uiState.value
        if (!state.inputEnabled) return
        if (buffer.length >= state.pinLength.digits) return
        buffer.append(digit)
        _uiState.update { it.copy(enteredCount = buffer.length) }
        if (buffer.length == state.pinLength.digits) {
            submit()
        }
    }

    fun onBackspace() {
        if (!_uiState.value.inputEnabled || buffer.isEmpty()) return
        buffer.deleteCharAt(buffer.length - 1)
        _uiState.update { it.copy(enteredCount = buffer.length) }
    }

    fun promptBiometric(activity: FragmentActivity) {
        if (!_uiState.value.biometricEnabled || !biometricAuthenticator.isAvailable) return
        biometricAuthenticator.authenticate(
            activity = activity,
            title = "Unlock NS Vault",
            negativeText = "Use PIN",
            onSuccess = ::onUnlocked,
        )
    }

    private fun submit() {
        val pin = buffer.toString()
        buffer.clear()
        _uiState.update { it.copy(verifying = true) }
        viewModelScope.launch {
            when (val result = verifyPin(pin)) {
                is PinVerification.Success -> onUnlocked()

                is PinVerification.Mismatch -> _uiState.update {
                    it.copy(
                        verifying = false,
                        enteredCount = 0,
                        errorPulse = it.errorPulse + 1,
                        message = wrongPinMessage(result.attemptsRemaining),
                        messageIsError = true,
                    )
                }

                is PinVerification.LockedOut -> {
                    _uiState.update {
                        it.copy(verifying = false, enteredCount = 0, errorPulse = it.errorPulse + 1)
                    }
                    startLockoutCountdown(result.remainingMillis)
                }
            }
        }
    }

    private fun onUnlocked() {
        _uiState.update { it.copy(success = true, message = null, messageIsError = false) }
        viewModelScope.launch {
            delay(UNLOCK_SETTLE_MS)
            sessionManager.unlock()
            // Reset for the next lock; the view model outlives lock cycles.
            buffer.clear()
            _uiState.update {
                it.copy(success = false, verifying = false, enteredCount = 0, message = null)
            }
        }
    }

    private fun startLockoutCountdown(remainingMillis: Long) {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            var remaining = remainingMillis
            while (remaining > 0) {
                val seconds = ceil(remaining / 1000.0).toLong()
                _uiState.update {
                    it.copy(
                        lockoutRemainingSeconds = seconds,
                        message = "Too many attempts · locked for ${seconds}s",
                        messageIsError = true,
                    )
                }
                delay(1_000)
                remaining -= 1_000
            }
            _uiState.update {
                it.copy(lockoutRemainingSeconds = 0, message = null, messageIsError = false)
            }
        }
    }

    private fun wrongPinMessage(attemptsRemaining: Int): String =
        if (attemptsRemaining == 1) {
            "Wrong PIN · 1 attempt before lockout"
        } else {
            "Wrong PIN · try again"
        }

    companion object {
        private const val UNLOCK_SETTLE_MS = 250L
    }
}
