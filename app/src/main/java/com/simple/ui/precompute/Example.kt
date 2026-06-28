package com.simple.ui.precompute

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.ImageNode
import com.simple.ui.precompute.node.ImageSource
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.TextNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Luồng tách bạch:
 *   1. Build LayoutNode (rẻ, immutable)         — bất kỳ thread nào
 *   2. LayoutEngine.measure(node, constraints)  — Dispatchers.Default
 *   3. view.spec = result                       — UI thread
 *
 * View chỉ làm bước 3 → vẽ.
 */
object Example {

    /** Build cây mô tả — không đo, chỉ là data. */
    fun cardNode(
        context: Context,
        iconResId: Int,
        word: String,
        ipa: String
    ): LayoutNode {
        val res = context.resources

        val sp16 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 16f, res.displayMetrics
        )
        val sp14 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 14f, res.displayMetrics
        )
        val dp = res.displayMetrics.density
        val dp8 = (8 * dp).toInt()
        val dp12 = (12 * dp).toInt()
        val dp48 = (48 * dp).toInt()

        return LinearNode(
            orientation = Orientation.HORIZONTAL,
            crossAlign = CrossAlign.CENTER,
            gap = dp12,
            padding = EdgeInsets.all(dp12),
            children = listOf(
                ImageNode(
                    source = ImageSource.ResSource(iconResId),
                    width = dp48,
                    height = dp48
                ),
                LinearNode(
                    orientation = Orientation.VERTICAL,
                    gap = dp8 / 2,
                    children = listOf(
                        TextNode(
                            text = word,
                            textSizePx = sp16,
                            color = Color.BLACK,
                            typeface = Typeface.DEFAULT_BOLD,
                            maxLines = 1
                        ),
                        TextNode(
                            text = ipa,
                            textSizePx = sp14,
                            color = Color.DKGRAY,
                            maxLines = 1
                        )
                    )
                )
            )
        )
    }
}

/**
 * Ví dụ ViewModel: đo ở bg, expose DrawSpec qua StateFlow.
 *
 * Fragment chỉ cần:
 *   viewLifecycleOwner.lifecycleScope.launch {
 *       viewModel.spec.collect { view.spec = it }
 *   }
 */
class CardViewModel : ViewModel() {

    private val _spec = MutableStateFlow<DrawSpec?>(null)
    val spec: StateFlow<DrawSpec?> = _spec

    /** Gọi khi đã biết width (vd lấy từ displayMetrics hoặc onLayout của container). */
    fun prepare(node: LayoutNode, availableWidthPx: Int) {
        viewModelScope.launch {
            val s = withContext(Dispatchers.Default) {
                LayoutEngine.measure(node, Constraints(availableWidthPx))
            }
            _spec.value = s
        }
    }
}
