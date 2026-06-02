package edu.gwu.androidtweets.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()

    private val _loginResult = MutableStateFlow<Result<String>?>(null)
    val loginResult: StateFlow<Result<String>?> = _loginResult.asStateFlow()

    private val _signUpResult = MutableStateFlow<Result<String>?>(null)
    val signUpResult: StateFlow<Result<String>?> = _signUpResult.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = runCatching {
                auth.signInWithEmailAndPassword(email, password).await()
                auth.currentUser!!.email!!
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _signUpResult.value = runCatching {
                auth.createUserWithEmailAndPassword(email, password).await()
                auth.currentUser!!.email!!
            }
        }
    }
}
