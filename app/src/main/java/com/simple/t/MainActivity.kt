package com.simple.phonetics.ui.precompute

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.simple.t.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val container = findViewById<LinearLayout>(R.id.container)
        val dp = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels

        // Dữ liệu mẫu
        val items = listOf(
            Triple("Xem thông tin tổng quan về thay đổi đối với ứng dụng và kiểm soát thời điểm phát hành/thời điểm gửi đi các thay đổi để Google xem xét.", "/həˈloʊ/", "Xin chào"),
            Triple("World", "/wɜːrld/", "Thế giới"),
            Triple("Android", "/ˈændrɔɪd/", "Hệ điều hành"),
            Triple("Kotlin", "/ˈkɒtlɪn/", "Ngôn ngữ lập trình"),
            Triple("Layout", "/ˈleɪaʊt/", "Bố cục"),
            Triple("Engine", "/ˈendʒɪn/", "Động cơ"),
            Triple("Precompute", "/priːkəmˈpjuːt/", "Tính trước"),
            Triple("Canvas", "/ˈkænvəs/", "Khung vẽ"),
            Triple("Android", "/ˈændrɔɪd/", "Hệ điều hành"),
            Triple("Kotlin", "/ˈkɒtlɪn/", "Ngôn ngữ lập trình"),
            Triple("Layout", "/ˈleɪaʊt/", "Bố cục"),
            Triple("Engine", "/ˈendʒɪn/", "Động cơ"),
            Triple("Precompute", "/priːkəmˈpjuːt/", "Tính trước"),
            Triple("Canvas", "/ˈkænvəs/", "Khung vẽ"),
        )

        // Icon load async qua Glide từ resource → demo ImageSource.ResSource.
        val dp48 = (48 * dp).toInt()
        val iconSource = ImageSource.ResSource(R.mipmap.ic_launcher)

        // Loader dùng chung — Glide tự cache nên không tốn gì.
        val loader = GlideBitmapLoader(this)

        lifecycleScope.launch {
            val specs = withContext(Dispatchers.Default) {
                items.map { (word, ipa, meaning) ->
                    val node = buildCardNode(word, ipa, meaning, iconSource, dp48)
                    LayoutEngine.measure(node, Constraints(screenWidth - (32 * dp).toInt()))
                }
            }

            for ((index, spec) in specs.withIndex()) {
                // Card background wrapper
                val card = PrecomputedView(this@MainActivity).apply {
                    this.bitmapLoader = loader
                    this.spec = spec
                    setBackgroundResource(R.drawable.card_background)
                    elevation = 2 * dp
                }

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) topMargin = (12 * dp).toInt()
                }

                container.addView(card, lp)
            }

            // Thêm label cuối
            val footer = TextView(this@MainActivity).apply {
                text = "${items.size} items — rendered with PrecomputedView"
                setTextColor(Color.GRAY)
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, (16 * dp).toInt(), 0, (16 * dp).toInt())
            }
            container.addView(footer)
        }
    }

    /**
     * Build LayoutNode cho một card: icon (trái) + (word + ipa + meaning) dọc (phải).
     */
    private fun buildCardNode(
        word: String,
        ipa: String,
        meaning: String,
        iconSource: ImageSource,
        iconSizePx: Int
    ): LayoutNode {
        val dp = resources.displayMetrics.density
        val sp16 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, resources.displayMetrics)
        val sp14 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        val sp12 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
        val dp12 = (12 * dp).toInt()
        val dp4 = (4 * dp).toInt()

        return LayoutNode.Linear(
            orientation = Orientation.HORIZONTAL,
            crossAlign = CrossAlign.CENTER,
            gap = dp12,
            padding = EdgeInsets.all(dp12),
            children = listOf(
                // Icon bên trái — load qua BitmapLoader (Glide)
                LayoutNode.Image(source = iconSource, width = iconSizePx, height = iconSizePx),
                // Text bên phải
                LayoutNode.Linear(
                    orientation = Orientation.VERTICAL,
                    gap = dp4,
                    children = listOf(
                        LayoutNode.Text(
                            text = word,
                            textSizePx = sp16,
                            color = Color.BLACK,
                            typeface = Typeface.DEFAULT_BOLD,
                            maxLines = 3
                        ),
                        LayoutNode.Text(
                            text = ipa,
                            textSizePx = sp14,
                            color = 0xFF6200EE.toInt(),
                            maxLines = 1
                        ),
                        LayoutNode.Text(
                            text = meaning,
                            textSizePx = sp12,
                            color = Color.GRAY,
                            maxLines = 2
                        )
                    )
                )
            )
        )
    }
}
