package com.nsvault.app.domain.model

enum class PinLength(val digits: Int) {
    FOUR(4),
    SIX(6),
    ;

    companion object {
        fun fromDigits(digits: Int): PinLength = entries.first { it.digits == digits }
    }
}

sealed interface PinVerification {
    /** PIN correct; the vault may open. */
    data object Success : PinVerification

    /** PIN wrong. [attemptsRemaining] until a lockout begins. */
    data class Mismatch(val attemptsRemaining: Int) : PinVerification

    /** Too many failures. No attempts until [remainingMillis] elapses. */
    data class LockedOut(val remainingMillis: Long) : PinVerification
}
