package edu.gwu.androidtweets

import android.app.Application
import edu.gwu.androidtweets.viewmodel.AuthViewModel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [AuthViewModel]'s local validation logic.
 * No Firebase, no coroutines, no test dispatcher setup needed — login/signUp
 * are synchronous since Phase 6 replaced the cloud calls with local checks.
 */
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        viewModel = AuthViewModel(mockk<Application>(relaxed = true))
    }

    // ---------- initial state ----------

    @Test
    fun `loginResult and signUpResult start as null`() {
        assertNull(viewModel.loginResult.value)
        assertNull(viewModel.signUpResult.value)
    }

    // ---------- login: success ----------

    @Test
    fun `login succeeds with valid email and password`() {
        viewModel.login("demo@demo.com", "demo123")

        val result = viewModel.loginResult.value
        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        assertEquals("demo@demo.com", result.getOrNull())
    }

    @Test
    fun `login returns the email as the success value`() {
        viewModel.login("user@example.org", "password1")

        assertEquals("user@example.org", viewModel.loginResult.value!!.getOrNull())
    }

    // ---------- login: failure ----------

    @Test
    fun `login fails when email has no at-sign`() {
        viewModel.login("notanemail", "demo123")

        assertTrue(viewModel.loginResult.value!!.isFailure)
    }

    @Test
    fun `login fails when password is fewer than 6 characters`() {
        viewModel.login("demo@demo.com", "short")

        assertTrue(viewModel.loginResult.value!!.isFailure)
    }

    @Test
    fun `login fails when email is empty`() {
        viewModel.login("", "demo123")

        assertTrue(viewModel.loginResult.value!!.isFailure)
    }

    @Test
    fun `login fails when password is empty`() {
        viewModel.login("demo@demo.com", "")

        assertTrue(viewModel.loginResult.value!!.isFailure)
    }

    @Test
    fun `login failure message is human-readable`() {
        viewModel.login("bad", "x")

        val message = viewModel.loginResult.value!!.exceptionOrNull()?.message
        assertNotNull("Failure should include an error message", message)
        assertTrue("Message should be non-empty", message!!.isNotEmpty())
    }

    // ---------- signUp ----------

    @Test
    fun `signUp succeeds with valid email and password`() {
        viewModel.signUp("new@user.com", "password123")

        assertTrue(viewModel.signUpResult.value!!.isSuccess)
        assertEquals("new@user.com", viewModel.signUpResult.value!!.getOrNull())
    }

    @Test
    fun `signUp fails with invalid credentials`() {
        viewModel.signUp("notanemail", "pw")

        assertTrue(viewModel.signUpResult.value!!.isFailure)
    }

    // ---------- state isolation ----------

    @Test
    fun `login result does not affect signUp result`() {
        viewModel.login("demo@demo.com", "demo123")

        assertNull("signUpResult should remain null after login", viewModel.signUpResult.value)
    }

    @Test
    fun `signUp result does not affect login result`() {
        viewModel.signUp("demo@demo.com", "demo123")

        assertNull("loginResult should remain null after signUp", viewModel.loginResult.value)
    }
}
