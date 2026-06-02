package edu.gwu.androidtweets

import android.location.Address
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import edu.gwu.androidtweets.api.MastodonApi
import edu.gwu.androidtweets.api.dto.toTweet
import edu.gwu.androidtweets.databinding.ActivityTweetsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TweetsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTweetsBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private val currentTweets: MutableList<Tweet> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTweetsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()

        @Suppress("DEPRECATION")
        val address: Address = intent.getParcelableExtra("address")!!

        if (savedInstanceState != null) {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            val savedTweets = savedInstanceState.getSerializable("TWEETS") as List<Tweet>
            currentTweets.addAll(savedTweets)
            binding.recyclerView.adapter = TweetsAdapter(currentTweets)
            binding.recyclerView.layoutManager = LinearLayoutManager(this)
        } else {
            getTweetsFromMastodon(address)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("TWEETS", ArrayList(currentTweets))
    }

    private fun getTweetsFromMastodon(address: Address) {
        binding.addTweet.hide()
        binding.tweetContent.visibility = View.GONE

        // Use the most specific available location name for display and search
        val city = address.locality
            ?: address.subAdminArea
            ?: address.adminArea
            ?: address.countryName
            ?: ""
        val displayCity = city.ifEmpty { address.getAddressLine(0) ?: "Unknown" }
        setTitle(getString(R.string.tweets_title, displayCity))

        lifecycleScope.launch {
            try {
                val statuses = withContext(Dispatchers.IO) {
                    val cityTag = city.toHashtag()
                    // Try filtered by city first; fall back to unfiltered #Android if no results
                    val filtered = if (cityTag.isNotEmpty()) {
                        MastodonApi.service.tagTimeline(hashtag = "Android", cityTag = cityTag)
                    } else emptyList()

                    filtered.ifEmpty {
                        MastodonApi.service.tagTimeline(hashtag = "Android")
                    }
                }
                val tweets = statuses.map { it.toTweet() }
                currentTweets.clear()
                currentTweets.addAll(tweets)
                binding.recyclerView.adapter = TweetsAdapter(tweets)
                binding.recyclerView.layoutManager = LinearLayoutManager(this@TweetsActivity)
            } catch (e: Exception) {
                Log.e("TweetsActivity", "Failed to retrieve posts", e)
                Toast.makeText(this@TweetsActivity, "Failed to retrieve posts!", Toast.LENGTH_LONG).show()
            }
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

private fun String.toHashtag(): String = lowercase()
    .replace(Regex("[^a-z0-9]"), "")
    .trim()
