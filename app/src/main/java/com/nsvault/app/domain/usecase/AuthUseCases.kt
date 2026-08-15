package com.nsvault.app.domain.usecase

import com.nsvault.app.domain.model.PinLength
import com.nsvault.app.domain.model.PinVerification
import com.nsvault.app.domain.repository.AuthRepository
import javax.inject.Inject

class SetPinUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(pin: String, length: PinLength) =
        authRepository.setPin(pin, length)
}

class VerifyPinUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(pin: String): PinVerification =
        authRepository.verifyPin(pin)
}

class SetBiometricEnabledUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(enabled: Boolean) =
        authRepository.setBiometricEnabled(enabled)
}
