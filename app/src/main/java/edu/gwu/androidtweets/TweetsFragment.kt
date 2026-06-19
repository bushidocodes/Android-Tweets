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
import androidx.recyclerview.widget.RecyclerView
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

    private var tweetsAdapter: TweetsAdapter? = null

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

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = recyclerView.layoutManager as LinearLayoutManager
                if (lm.findLastVisibleItemPosition() >= lm.itemCount - 3) {
                    viewModel.loadMoreTweets()
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tweets.collect { result ->
                    when {
                        result == null -> {}
                        result.isSuccess -> {
                            val tweets = result.getOrNull()!!
                            val current = tweetsAdapter
                            if (current == null) {
                                tweetsAdapter = TweetsAdapter(tweets).also {
                                    binding.recyclerView.adapter = it
                                }
                            } else {
                                val newCount = tweets.size - current.itemCount
                                if (newCount > 0) {
                                    current.appendTweets(tweets.subList(current.itemCount, tweets.size))
                                }
                            }
                        }
                        else -> {
                            Log.e("TweetsFragment", "Failed to load posts", result.exceptionOrNull())
                            Toast.makeText(requireContext(), "Failed to retrieve posts!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoadingMore.collect { loading ->
                    binding.loadingMore.visibility = if (loading) View.VISIBLE else View.GONE
                }
            }
        }

        if (viewModel.tweets.value == null) {
            viewModel.loadTweets(address)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tweetsAdapter = null
        _binding = null
    }
}
