package org.danielzfranklin.librereader.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.databinding.MainActivityBinding
import org.danielzfranklin.librereader.repo.Repo
import org.danielzfranklin.librereader.ui.reader.ReaderActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: Remove example content
        binding.exampleBookButton.setOnClickListener {
            val repo = Repo.get()

            val bookId = repo.importBook(
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(packageName)
                    .path(R.raw.frankenstein_public_domain.toString())
                    .build()
            )

            ReaderActivity.start(this, bookId)
        }
    }
}