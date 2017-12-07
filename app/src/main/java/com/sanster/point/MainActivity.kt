package com.sanster.point

import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadTestImage()
    }

    private fun loadTestImage() {
        Glide.with(this)
                .asBitmap()
                .load(R.raw.test)
                .into(object: SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                        imageView.setImageBitmap(resource!!)
                    }
                })
    }
}
