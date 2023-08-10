package com.snapshotandroid.circularimageview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.snapshotandroid.circleimageviewer.CustomImageView

class MainActivity : AppCompatActivity() {

    //create a variable for CustomImageView
    private lateinit var customImageView: CustomImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialize the variable for CustomImageView
        customImageView = findViewById(R.id.profileImg)
    }
}