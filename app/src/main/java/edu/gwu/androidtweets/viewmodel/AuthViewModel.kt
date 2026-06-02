package edu.gwu.androidtweets.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local authentication — no cloud dependency.
 * Accepts any valid-looking email + password ≥ 6 characters.
 * Sufficient for demo purposes; swap in a real auth backend when needed.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _loginResult = MutableStateFlow<Result<String>?>(null)
    val loginResult: StateFlow<Result<String>?> = _loginResult.asStateFlow()

    private val _signUpResult = MutableStateFlow<Result<String>?>(null)
    val signUpResult: StateFlow<Result<String>?> = _signUpResult.asStateFlow()

    fun login(email: String, password: String) {
        _loginResult.value = validate(email, password)
    }

    fun signUp(email: String, password: String) {
        _signUpResult.value = validate(email, password)
    }

    private fun validate(email: String, password: String): Result<String> =
        if (email.contains('@') && password.length >= 6)
            Result.success(email)
        else
            Result.failure(IllegalArgumentException("Enter a valid email and a password of at least 6 characters."))
}
