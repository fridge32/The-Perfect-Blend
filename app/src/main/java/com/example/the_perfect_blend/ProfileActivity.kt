package com.example.the_perfect_blend

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var colorsMatchedTextView: TextView
    private lateinit var colorsSeenTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var leaderboardRecyclerView: RecyclerView
    private lateinit var leaderboardAdapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://the-perfect-blend-b4608-default-rtdb.asia-southeast1.firebasedatabase.app")

        // Initialize UI elements
        colorsMatchedTextView = findViewById(R.id.colorsMatchedTextView)
        colorsSeenTextView = findViewById(R.id.colorsSeenTextView)
        logoutButton = findViewById(R.id.logoutButton)

        leaderboardRecyclerView = findViewById(R.id.leaderboardRecyclerView)
        leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        fetchUserStats()
        fetchLeaderboard()

        // Get current user
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userStatsRef = database.reference.child("user_stats").child(currentUser.uid)

            // Fetch user stats
            userStatsRef.child("colors_seen").get().addOnSuccessListener { snapshot ->
                val colorsSeen = snapshot.getValue(Int::class.java) ?: 0
                colorsSeenTextView.text = "Total Colors Seen: $colorsSeen"
            }

            userStatsRef.child("colors_matched").get().addOnSuccessListener { snapshot ->
                val colorsMatched = snapshot.getValue(Int::class.java) ?: 0
                colorsMatchedTextView.text = "Total Colors Matched: $colorsMatched"
            }
        }
    }
    private fun fetchUserStats() {
        val currentUser = auth.currentUser ?: return
        val userStatsRef = database.reference.child("user_stats").child(currentUser.uid)
        userStatsRef.child("colors_seen").get().addOnSuccessListener { snapshot ->
            val colorsSeen = snapshot.getValue(Int::class.java) ?: 0
            colorsSeenTextView.text = "Total Colors Seen: $colorsSeen"
        }
        userStatsRef.child("colors_matched").get().addOnSuccessListener { snapshot ->
            val colorsMatched = snapshot.getValue(Int::class.java) ?: 0
            colorsMatchedTextView.text = "Total Colors Matched: $colorsMatched"
        }
    }

    private fun fetchLeaderboard() {
        val leaderboardRef = database.reference.child("user_stats").orderByChild("colors_matched").limitToLast(10)
        leaderboardRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val leaderboardItems = mutableListOf<Pair<String, Int>>()
                for (userSnapshot in snapshot.children) {
                    val email = userSnapshot.child("email").getValue(String::class.java) ?: "Unknown"
                    val colorsMatched = userSnapshot.child("colors_matched").getValue(Int::class.java) ?: 0
                    leaderboardItems.add(Pair(email, colorsMatched))
                }
                leaderboardItems.reverse() // To show highest first
                leaderboardAdapter = LeaderboardAdapter(leaderboardItems)
                leaderboardRecyclerView.adapter = leaderboardAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ProfileActivity", "loadLeaderboard:onCancelled", error.toException())
            }
        })
    }
}

