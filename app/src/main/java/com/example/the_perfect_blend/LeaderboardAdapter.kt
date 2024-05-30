package com.example.the_perfect_blend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.the_perfect_blend.R

class LeaderboardAdapter(private val leaderboardItems: List<Pair<String, Int>>) :
    RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    class LeaderboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emailTextView: TextView = itemView.findViewById(R.id.emailTextView)
        val colorsMatchedTextView: TextView = itemView.findViewById(R.id.colorsMatchedTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.leaderboard_item, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val (email, colorsMatched) = leaderboardItems[position]
        holder.emailTextView.text = email
        holder.colorsMatchedTextView.text = colorsMatched.toString()
    }

    override fun getItemCount() = leaderboardItems.size
}
