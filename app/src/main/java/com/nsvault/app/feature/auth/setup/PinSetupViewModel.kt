package com.nsvault.app.feature.auth.setup

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsvault.app.data.auth.SessionManager
import com.nsvault.app.domain.model.PinLength
import com.nsvault.app.domain.usecase.SetBiometricEnabledUseCase
import com.nsvault.app.domain.usecase.SetPinUseCase
import com.nsvault.app.feature.auth.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinSetupViewModel @Inject constructor(
    private val setPin: SetPinUseCase,
    private val setBiometricEnabled: SetBiometricEnabledUseCase,
    private val sessionManager: SessionManager,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

    enum class Step { Welcome, Enter, Confirm, Biometric, Success }

    data class UiState(
        val step: Step = Step.Welcome,
        val pinLength: PinLength = PinLength.FOUR,
        val enteredCount: Int = 0,
        val errorPulse: Int = 0,
        val mismatch: Boolean = false,
        val biometricAvailable: Boolean = false,
    )

    private val _uiState = MutableStateFlow(
        UiState(biometricAvailable = biometricAuthenticator.isAvailable),
    )
    val uiState = _uiState.asStateFlow()

    private val buffer = StringBuilder()
    private var firstPin: String = ""

    fun onLengthSelected(length: PinLength) {
        _uiState.update { it.copy(pinLength = length) }
    }

    fun onBegin() {
        buffer.clear()
        _uiState.update { it.copy(step = Step.Enter, enteredCount = 0) }
    }

    fun onDigit(digit: Int) {
        val state = _uiState.value
        if (state.step != Step.Enter && state.step != Step.Confirm) return
        if (buffer.length >= state.pinLength.digits) return
        buffer.append(digit)
        _uiState.update { it.copy(enteredCount = buffer.length) }
        if (buffer.length == state.pinLength.digits) {
            onPinComplete()
        }
    }

    fun onBackspace() {
        if (buffer.isEmpty()) return
        buffer.deleteCharAt(buffer.length - 1)
        _uiState.update { it.copy(enteredCount = buffer.length) }
    }

    fun onStartOver() {
        firstPin = ""
        buffer.clear()
        _uiState.update {
            it.copy(step = Step.Enter, enteredCount = 0, mismatch = false)
        }
    }

    fun onEnableBiometric(activity: FragmentActivity) {
        biometricAuthenticator.authenticate(
            activity = activity,
            title = "Confirm fingerprint",
            subtitle = "Verify once to enable fingerprint unlock",
            negativeText = "Cancel",
            onSuccess = { completeSetup(enableBiometric = true) },
        )
    }

    fun onSkipBiometric() = completeSetup(enableBiometric = false)

    private fun onPinComplete() {
        viewModelScope.launch {
            // Let the final dot's pop-in land before advancing.
            delay(DOT_SETTLE_MS)
            when (_uiState.value.step) {
                Step.Enter -> {
                    firstPin = buffer.toString()
                    buffer.clear()
                    _uiState.update {
                        it.copy(step = Step.Confirm, enteredCount = 0, mismatch = false)
                    }
                }

                Step.Confirm -> {
                    val matches = buffer.toString() == firstPin
                    buffer.clear()
                    if (matches) {
                        if (_uiState.value.biometricAvailable) {
                            _uiState.update { it.copy(step = Step.Biometric, enteredCount = 0) }
                        } else {
                            completeSetup(enableBiometric = false)
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                enteredCount = 0,
                                errorPulse = it.errorPulse + 1,
                                mismatch = true,
                            )
                        }
                    }
                }

                else -> Unit
            }
        }
    }

    private fun completeSetup(enableBiometric: Boolean) {
        val pin = firstPin
        firstPin = ""
        _uiState.update { it.copy(step = Step.Success) }
        viewModelScope.launch {
            delay(SUCCESS_HOLD_MS)
            // Unlock before persisting so the root state can never pass
            // through Locked mid-setup.
            sessionManager.unlock()
            setPin(pin, _uiState.value.pinLength)
            setBiometricEnabled(enableBiometric)
        }
    }

    companion object {
        private const val DOT_SETTLE_MS = 150L
        private const val SUCCESS_HOLD_MS = 1200L
    }
}
