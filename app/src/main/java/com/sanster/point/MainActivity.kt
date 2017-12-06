package com.sanster.point

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadTestImage()
    }

    private fun loadTestImage() {
        val uriString = "android.resource://$packageName/${R.raw.test}"
        imageView.load(uriString)
    }
}
