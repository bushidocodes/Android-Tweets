package edu.gwu.androidtweets

import android.location.Address
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import edu.gwu.androidtweets.databinding.ActivityTweetsBinding

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
            getTweetsFromTwitter(address)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("TWEETS", ArrayList(currentTweets))
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

    private fun getTweetsFromTwitter(address: Address) {
        binding.addTweet.hide()
        binding.tweetContent.visibility = View.GONE
        setTitle(getString(R.string.tweets_title, address.locality ?: "Unknown"))

        Thread {
            val twitterManager = TwitterManager()
            try {
                val oAuthToken = twitterManager.retrieveOAuthToken(
                    apiKey = getString(R.string.twitter_api_key),
                    apiSecret = getString(R.string.twitter_api_secret)
                )
                val tweets = twitterManager.retrieveTweets(
                    oAuthToken = oAuthToken,
                    latitude = address.latitude,
                    longitude = address.longitude
                )
                currentTweets.clear()
                currentTweets.addAll(tweets)

                runOnUiThread {
                    binding.recyclerView.adapter = TweetsAdapter(tweets)
                    binding.recyclerView.layoutManager = LinearLayoutManager(this@TweetsActivity)
                }
            } catch (exception: Exception) {
                Log.e("TweetsActivity", "Retrieving Tweets failed", exception)
                runOnUiThread {
                    Toast.makeText(this@TweetsActivity, "Failed to retrieve Tweets!", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    fun getFakeTweets(): List<Tweet> {
        return listOf(
            Tweet(handle = "@nickcapurso", username = "Nick Capurso", content = "We're learning lists!", iconUrl = "https://...."),
            Tweet(username = "Android Central", handle = "@androidcentral", content = "NVIDIA Shield TV vs. Shield TV Pro: Which should I buy?", iconUrl = "https://...."),
            Tweet(username = "DC Android", handle = "@DCAndroid", content = "FYI - another great integration for the @Firebase platform", iconUrl = "https://...."),
            Tweet(username = "KotlinConf", handle = "@kotlinconf", content = "Can't make it to KotlinConf this year? We have a surprise for you. We'll be live streaming the keynotes, closing panel and an entire track over the 2 main conference days. Sign-up to get notified once we go live!", iconUrl = "https://...."),
            Tweet(username = "Android Summit", handle = "@androidsummit", content = "What a #Keynote! @SlatteryClaire is the Director of Performance at Speechless, and that's exactly how she left us after her amazing (and interactive!) #keynote at #androidsummit. #DCTech #AndroidDev #Android", iconUrl = "https://...."),
            Tweet(username = "Fragmented Podcast", handle = "@FragmentedCast", content = ".... annnnnnnnnd we're back!\n\nThis week @donnfelker talks about how it's Ok to not know everything and how to set yourself up mentally for JIT (Just In Time [learning]). Listen in here: \nhttp://fragmentedpodcast.com/episodes/135/ ", iconUrl = "https://...."),
            Tweet(username = "Jake Wharton", handle = "@JakeWharton", content = "Free idea: location-aware physical password list inside a password manager. Mostly for garage door codes and the like. I want to open my password app, switch to the non-URL password section, and see a list of things sorted by physical distance to me.", iconUrl = "https://...."),
            Tweet(username = "Droidcon Boston", handle = "@droidconbos", content = "#DroidconBos will be back in Boston next year on April 8-9!", iconUrl = "https://...."),
            Tweet(username = "AndroidWeekly", handle = "@androidweekly", content = "Latest Android Weekly Issue 327 is out!\nhttp://androidweekly.net/ #latest-issue  #AndroidDev", iconUrl = "https://...."),
            Tweet(username = ".droidconSF", handle = "@droidconSF", content = "Drum roll please.. Announcing droidcon SF 2018! November 19-20 @ Mission Bay Conference Center. Content and programming by @tsmith & @joenrv.", iconUrl = "https://...."),
        )
    }
}
