package edu.gwu.androidtweets

import android.location.Address
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import edu.gwu.androidtweets.databinding.ActivityTweetsBinding
import edu.gwu.androidtweets.viewmodel.TweetsViewModel
import kotlinx.coroutines.launch

class TweetsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTweetsBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private val viewModel: TweetsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTweetsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()

        @Suppress("DEPRECATION")
        val address: Address = intent.getParcelableExtra("address")!!

        val city = address.locality
            ?: address.subAdminArea
            ?: address.adminArea
            ?: address.countryName
            ?: address.getAddressLine(0)
            ?: "Unknown"
        setTitle(getString(R.string.tweets_title, city))

        binding.addTweet.hide()
        binding.tweetContent.visibility = View.GONE

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tweets.collect { result ->
                    when {
                        result == null -> {}
                        result.isSuccess -> {
                            val tweets = result.getOrNull()!!
                            binding.recyclerView.adapter = TweetsAdapter(tweets)
                            binding.recyclerView.layoutManager = LinearLayoutManager(this@TweetsActivity)
                        }
                        else -> {
                            Log.e("TweetsActivity", "Failed to retrieve posts", result.exceptionOrNull())
                            Toast.makeText(this@TweetsActivity, "Failed to retrieve posts!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        // Guard against re-fetching on rotation — ViewModel already has the result
        if (viewModel.tweets.value == null) {
            viewModel.loadTweets(address)
        }
    }

    private fun getTweetsFromFirebase(address: Address) {
        val state = address.adminArea ?: "Unknown"
        setTitle(getString(R.string.tweets_title, state))

        val reference = firebaseDatabase.getReference("tweets/$state")

        binding.addTweet.setOnClickListener {
            val tweet = Tweet(
                username = FirebaseAuth.getInstance().currentUser!!.email!!,
                handle = FirebaseAuth.getInstance().currentUser!!.email!!,
                iconUrl = "https://i.imgur.com/DvpvklR.png",
                content = binding.tweetContent.text.toString()
            )
            reference.push().setValue(tweet)
        }

        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@TweetsActivity,
                    "Failed to retrieve data from Firebase: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val tweets = snapshot.children.mapNotNull { it.getValue(Tweet::class.java) }
                binding.recyclerView.adapter = TweetsAdapter(tweets)
                binding.recyclerView.layoutManager = LinearLayoutManager(this@TweetsActivity)
            }
        })
    }
}
