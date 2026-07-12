package com.blooddonation.app

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val donateBtn = findViewById<Button>(R.id.donateBtn)
        val findDonorBtn = findViewById<Button>(R.id.findDonorBtn)

        donateBtn.setOnClickListener {
            Toast.makeText(this, "Donate Blood feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        findDonorBtn.setOnClickListener {
            Toast.makeText(this, "Find Donor feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}