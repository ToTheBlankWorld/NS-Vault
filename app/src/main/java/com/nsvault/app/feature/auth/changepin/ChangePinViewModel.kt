package com.nsvault.app.feature.auth.changepin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsvault.app.domain.model.PinLength
import com.nsvault.app.domain.model.PinVerification
import com.nsvault.app.domain.repository.AuthRepository
import com.nsvault.app.domain.usecase.SetPinUseCase
import com.nsvault.app.domain.usecase.VerifyPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class ChangePinViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val verifyPin: VerifyPinUseCase,
    private val setPin: SetPinUseCase,
) : ViewModel() {

    enum class Step { VerifyCurrent, Enter, Confirm, Success }

    data class UiState(
        val step: Step = Step.VerifyCurrent,
        val currentLength: PinLength = PinLength.FOUR,
        val newLength: PinLength = PinLength.FOUR,
        val enteredCount: Int = 0,
        val errorPulse: Int = 0,
        val message: String? = null,
        val messageIsError: Boolean = false,
        val lockoutRemainingSeconds: Long = 0,
        val verifying: Boolean = false,
    ) {
        val activeLength: PinLength
            get() = if (step == Step.VerifyCurrent) currentLength else newLength

        val inputEnabled: Boolean
            get() = !verifying && lockoutRemainingSeconds == 0L && step != Step.Success
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val buffer = StringBuilder()
    private var newPin: String = ""
    private var lockoutJob: Job? = null

    init {
        viewModelScope.launch {
            val current = authRepository.pinLength.first() ?: PinLength.FOUR
            _uiState.update { it.copy(currentLength = current, newLength = current) }
        }
    }

    fun onNewLengthSelected(length: PinLength) {
        if (_uiState.value.step != Step.Enter) return
        buffer.clear()
        _uiState.update { it.copy(newLength = length, enteredCount = 0) }
    }

    fun onDigit(digit: Int) {
        val state = _uiState.value
        if (!state.inputEnabled) return
        if (buffer.length >= state.activeLength.digits) return
        buffer.append(digit)
        _uiState.update { it.copy(enteredCount = buffer.length) }
        if (buffer.length == state.activeLength.digits) {
            onPinComplete()
        }
    }

    fun onBackspace() {
        if (!_uiState.value.inputEnabled || buffer.isEmpty()) return
        buffer.deleteCharAt(buffer.length - 1)
        _uiState.update { it.copy(enteredCount = buffer.length) }
    }

    fun onStartOver() {
        newPin = ""
        buffer.clear()
        _uiState.update {
            it.copy(step = Step.Enter, enteredCount = 0, message = null, messageIsError = false)
        }
    }

    private fun onPinComplete() {
        viewModelScope.launch {
            delay(DOT_SETTLE_MS)
            when (_uiState.value.step) {
                Step.VerifyCurrent -> verifyCurrent()

                Step.Enter -> {
                    newPin = buffer.toString()
                    buffer.clear()
                    _uiState.update {
                        it.copy(step = Step.Confirm, enteredCount = 0, message = null)
                    }
                }

                Step.Confirm -> {
                    val matches = buffer.toString() == newPin
                    buffer.clear()
                    if (matches) {
                        val pin = newPin
                        newPin = ""
                        setPin(pin, _uiState.value.newLength)
                        _uiState.update { it.copy(step = Step.Success) }
                    } else {
                        _uiState.update {
                            it.copy(
                                enteredCount = 0,
                                errorPulse = it.errorPulse + 1,
                                message = "PINs didn't match — try again",
                                messageIsError = true,
                            )
                        }
                    }
                }

                Step.Success -> Unit
            }
        }
    }

    private suspend fun verifyCurrent() {
        val pin = buffer.toString()
        buffer.clear()
        _uiState.update { it.copy(verifying = true) }
        when (val result = verifyPin(pin)) {
            is PinVerification.Success -> _uiState.update {
                it.copy(
                    step = Step.Enter,
                    verifying = false,
                    enteredCount = 0,
                    message = null,
                    messageIsError = false,
                )
            }

            is PinVerification.Mismatch -> _uiState.update {
                it.copy(
                    verifying = false,
                    enteredCount = 0,
                    errorPulse = it.errorPulse + 1,
                    message = "Wrong PIN · try again",
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

    companion object {
        private const val DOT_SETTLE_MS = 150L
    }
}
