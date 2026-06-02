package edu.gwu.androidtweets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import edu.gwu.androidtweets.databinding.RowTweetBinding

class TweetsAdapter(val tweets: List<Tweet>) : RecyclerView.Adapter<TweetsAdapter.ViewHolder>() {

    class ViewHolder(val binding: RowTweetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowTweetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = tweets.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tweet = tweets[position]
        holder.binding.username.text = tweet.username
        holder.binding.handle.text = tweet.handle
        holder.binding.tweetContent.text = tweet.content

        if (tweet.iconUrl.isNotEmpty()) {
            holder.binding.icon.load(tweet.iconUrl)
        }
    }
}
