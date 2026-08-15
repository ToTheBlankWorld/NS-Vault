package com.nsvault.app.feature.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over [BiometricPrompt] restricted to Class 3 (strong)
 * biometrics. Cancellation is reported as a `null` error so callers can
 * fall back to the PIN silently.
 */
@Singleton
class BiometricAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val isAvailable: Boolean
        get() = BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        negativeText: String,
        subtitle: String? = null,
        onSuccess: () -> Unit,
        onError: (message: String?) -> Unit = {},
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val userCancelled = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED
                    onError(if (userCancelled) null else errString.toString())
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply { subtitle?.let(::setSubtitle) }
            .setNegativeButtonText(negativeText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(promptInfo)
    }
}
