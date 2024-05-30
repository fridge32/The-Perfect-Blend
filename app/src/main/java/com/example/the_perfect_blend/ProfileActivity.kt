package com.example.the_perfect_blend

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var searchFriendEditText: EditText
    private lateinit var addFriendButton: Button
    private lateinit var leaderboardTitle: TextView
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        searchFriendEditText = findViewById(R.id.searchFriendEditText)
        addFriendButton = findViewById(R.id.addFriendButton)
        leaderboardTitle = findViewById(R.id.leaderboardTitle)
        logoutButton = findViewById(R.id.logoutButton)

        addFriendButton.setOnClickListener {
            val friendEmail = searchFriendEditText.text.toString().trim()
            // Logic to add friend using friendEmail
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
