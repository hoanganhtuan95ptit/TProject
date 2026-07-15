package com.simple.ui.precompute

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.simple.t.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        PrecomputeDemoScreen(this).render()
    }
}
