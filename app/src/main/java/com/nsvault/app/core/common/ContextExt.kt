package com.nsvault.app.core.common

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * Walks the context chain to the hosting [FragmentActivity], which
 * BiometricPrompt requires.
 */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
