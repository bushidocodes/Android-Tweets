package edu.gwu.androidtweets

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import edu.gwu.androidtweets.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs: SharedPreferences = getSharedPreferences("android-tweets", Context.MODE_PRIVATE)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        binding.login.isEnabled = false
        binding.signUp.isEnabled = false

        binding.login.setOnClickListener {
            val inputtedUsername: String = binding.username.text.toString()
            val inputtedPassword: String = binding.password.text.toString()

            firebaseAuth
                .signInWithEmailAndPassword(inputtedUsername, inputtedPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        firebaseAnalytics.logEvent("login_success", null)

                        val user: FirebaseUser = firebaseAuth.currentUser!!
                        Toast.makeText(this, "Logged in as ${user.email}!", Toast.LENGTH_LONG).show()

                        sharedPrefs.edit()
                            .putString("SAVED_USERNAME", binding.username.text.toString())
                            .apply()

                        startActivity(Intent(this, MapsActivity::class.java))
                    } else {
                        val exception = task.exception
                        val reason = if (exception is FirebaseAuthInvalidCredentialsException)
                            "invalid_credentials" else "generic_failure"

                        firebaseAnalytics.logEvent("login_failed", Bundle().apply {
                            putString("error_type", reason)
                        })

                        Toast.makeText(this, "Failed to log in: $exception", Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.signUp.setOnClickListener {
            val inputtedUsername: String = binding.username.text.toString()
            val inputtedPassword: String = binding.password.text.toString()

            firebaseAuth
                .createUserWithEmailAndPassword(inputtedUsername, inputtedPassword)
                .addOnCompleteListener { task: Task<AuthResult> ->
                    if (task.isSuccessful) {
                        val user: FirebaseUser = firebaseAuth.currentUser!!
                        Toast.makeText(this, "Signed up as ${user.email}!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed to sign up: ${task.exception}", Toast.LENGTH_LONG).show()
                    }
                }
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
