package edu.gwu.androidtweets

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gwu.androidtweets.databinding.FragmentTweetsBinding
import edu.gwu.androidtweets.viewmodel.MapsViewModel
import edu.gwu.androidtweets.viewmodel.TweetsViewModel
import kotlinx.coroutines.launch

class TweetsFragment : Fragment() {

    private var _binding: FragmentTweetsBinding? = null
    private val binding get() = _binding!!

    // Reads the address selected in MapsFragment (activity-scoped)
    private val mapsViewModel: MapsViewModel by activityViewModels()
    private val viewModel: TweetsViewModel by viewModels { TweetsViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTweetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val address = mapsViewModel.selection.value!!.address

        val city = address.locality
            ?: address.subAdminArea
            ?: address.adminArea
            ?: address.countryName
            ?: address.getAddressLine(0)
            ?: "Unknown"
        requireActivity().title = getString(R.string.tweets_title, city)

        binding.addTweet.hide()
        binding.tweetContent.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tweets.collect { result ->
                    when {
                        result == null -> {}
                        result.isSuccess -> {
                            val tweets = result.getOrNull()!!
                            binding.recyclerView.adapter = TweetsAdapter(tweets)
                            binding.recyclerView.layoutManager =
                                LinearLayoutManager(requireContext())
                        }
                        else -> {
                            Log.e("TweetsFragment", "Failed to load posts", result.exceptionOrNull())
                            Toast.makeText(requireContext(), "Failed to retrieve posts!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        if (viewModel.tweets.value == null) {
            viewModel.loadTweets(address)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
