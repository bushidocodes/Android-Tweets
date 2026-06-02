package edu.gwu.androidtweets

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import edu.gwu.androidtweets.databinding.ActivityMainBinding
import edu.gwu.androidtweets.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs: SharedPreferences = getSharedPreferences("android-tweets", Context.MODE_PRIVATE)
        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.login.isEnabled = false
        binding.signUp.isEnabled = false

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginResult.collect { result ->
                    result ?: return@collect
                    if (result.isSuccess) {
                        val email = result.getOrNull()!!
                        firebaseAnalytics.logEvent("login_success", null)
                        Toast.makeText(this@MainActivity, "Logged in as $email!", Toast.LENGTH_LONG).show()
                        sharedPrefs.edit()
                            .putString("SAVED_USERNAME", binding.username.text.toString())
                            .apply()
                        startActivity(Intent(this@MainActivity, MapsActivity::class.java))
                    } else {
                        val e = result.exceptionOrNull()
                        val reason = if (e is FirebaseAuthInvalidCredentialsException)
                            "invalid_credentials" else "generic_failure"
                        firebaseAnalytics.logEvent("login_failed", Bundle().apply {
                            putString("error_type", reason)
                        })
                        Toast.makeText(this@MainActivity, "Failed to log in: $e", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signUpResult.collect { result ->
                    result ?: return@collect
                    if (result.isSuccess) {
                        Toast.makeText(this@MainActivity, "Signed up as ${result.getOrNull()}!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to sign up: ${result.exceptionOrNull()}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        binding.login.setOnClickListener {
            viewModel.login(binding.username.text.toString(), binding.password.text.toString())
        }

        binding.signUp.setOnClickListener {
            viewModel.signUp(binding.username.text.toString(), binding.password.text.toString())
        }

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val enable = binding.username.text.isNotEmpty() && binding.password.text.isNotEmpty()
                binding.login.isEnabled = enable
                binding.signUp.isEnabled = enable
            }
        }

        binding.username.addTextChangedListener(textWatcher)
        binding.password.addTextChangedListener(textWatcher)
        binding.username.setText(sharedPrefs.getString("SAVED_USERNAME", ""))
    }
}
