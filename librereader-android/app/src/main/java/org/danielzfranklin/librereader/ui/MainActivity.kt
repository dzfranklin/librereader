package org.danielzfranklin.librereader.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.danielzfranklin.librereader.databinding.MainActivityBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}