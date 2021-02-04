package org.danielzfranklin.librereader.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.Spanned
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.danielzfranklin.librereader.R
import org.danielzfranklin.librereader.databinding.ReaderPagesViewBinding
import timber.log.Timber

@SuppressLint("ViewConstructor")
class ReaderPagesView(
    context: Context,
    private val book: Book,
    private val initialPosition: BookPosition
) : LinearLayout(context) {
    private val inflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val binding = ReaderPagesViewBinding.inflate(inflater, this, true)
    private val pager = binding.pager

    private lateinit var sectionPages: List<Spanned>

    private val fontSize = 25f

    init {
        val measurementView = TextView(context)
        measurementView.textSize = fontSize
        val textPaint = measurementView.paint

        // Enqueue for when layout is done. See <https://stackoverflow.com/a/24035591>
        post {
            sectionPages = computeSectionPages(initialPosition, textPaint)
            pager.adapter = PagerAdapter(sectionPages, fontSize)
        }
    }

    private fun computeSectionPages(pos: BookPosition, textPaint: TextPaint): List<Spanned> {
        if (pos !is BookPosition.Position) {
            TODO()
        }

        val section = book.sections[pos.sectionIndex]
        val props = BookSection.PageDisplayProperties(
            binding.root.width,
            binding.root.height,
            textPaint
        )
        return section.paginate(props)
    }

    private class PagerAdapter(private val pages: List<Spanned>, private val fontSize: Float) :
        RecyclerView.Adapter<PagerAdapter.PagerViewHolder>() {

        private class PagerViewHolder(view: View, fontSize: Float) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(R.id.text)

            init {
                textView.textSize = fontSize

                textView.movementMethod = BetterLinkMovementMethod.newInstance().apply {
                    setOnLinkClickListener { _, url ->
                        // Handle click or return false to let the framework handle this link.
                        Timber.w("Clicked %s", url)
                        true
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.page, parent, false)

            return PagerViewHolder(view, fontSize)
        }

        override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
            holder.textView.text = pages[position].trim()
        }

        override fun getItemCount() = pages.size
    }
}