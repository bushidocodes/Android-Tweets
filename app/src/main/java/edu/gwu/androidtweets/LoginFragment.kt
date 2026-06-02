package edu.gwu.androidtweets

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import edu.gwu.androidtweets.databinding.FragmentLoginBinding
import edu.gwu.androidtweets.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPrefs = requireContext()
            .getSharedPreferences("android-tweets", Context.MODE_PRIVATE)

        binding.login.isEnabled = false
        binding.signUp.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginResult.collect { result ->
                    result ?: return@collect
                    if (result.isSuccess) {
                        val email = result.getOrNull()!!
                        Toast.makeText(requireContext(), "Logged in as $email!", Toast.LENGTH_SHORT).show()
                        sharedPrefs.edit()
                            .putString("SAVED_USERNAME", email)
                            .apply()
                        findNavController().navigate(R.id.action_loginFragment_to_mapsFragment)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            result.exceptionOrNull()?.message ?: "Login failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signUpResult.collect { result ->
                    result ?: return@collect
                    if (result.isSuccess) {
                        Toast.makeText(requireContext(), "Account created! You can now log in.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            result.exceptionOrNull()?.message ?: "Sign up failed",
                            Toast.LENGTH_LONG
                        ).show()
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
                val enable = binding.username.text?.isNotEmpty() == true &&
                        binding.password.text?.isNotEmpty() == true
                binding.login.isEnabled = enable
                binding.signUp.isEnabled = enable
            }
        }

        binding.username.addTextChangedListener(textWatcher)
        binding.password.addTextChangedListener(textWatcher)
        binding.username.setText(sharedPrefs.getString("SAVED_USERNAME", ""))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
