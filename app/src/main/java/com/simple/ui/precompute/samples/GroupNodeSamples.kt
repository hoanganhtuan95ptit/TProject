package com.simple.ui.precompute.samples

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.GroupNode
import com.simple.ui.precompute.node.GroupSpec
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.SpaceNode
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.text.BigText

// ─────────────────────────────────────────────────────────────────────────────
// Ví dụ subclass minh hoạ "B kế thừa A" — cách tùy biến của [GroupNode]/[GroupSpec].
//
// 5 pattern phổ biến, xếp theo mức độ can thiệp:
//   1. TagRow             → override buildChildren()    (sinh children từ data).
//   2. SelfContainedGroup → tự sinh toàn bộ children + spec từ config nội bộ.
//   3. IndentedColumn     → override transformChild()   (bọc / trang trí từng child).
//   4. SectionGroup       → override finalizeChildren() (chèn header/separator).
//   5. CardGroup          → override createSpec()       (dùng subclass GroupSpec
//                                                        tự vẽ nền/overlay).
//
// 3 pattern đầu tùy biến **children**. Pattern 4 tùy biến **cách vẽ** — subclass
// GroupSpec dùng hook drawBackground / drawOverlay để trang trí toàn nhóm.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Ví dụ 1 — sinh children từ data list.
 *
 * B kế thừa A ([GroupNode]) và **override [buildChildren]** để mỗi phần tử
 * trong [labels] thành 1 [TextNode].
 */
class TagRow(
    private val labels: List<String>,
    private val textSizePx: Float = 14f,
    private val textColor: Int = Color.BLACK,
    gap: Int = 8,
    padding: EdgeInsets = EdgeInsets.ZERO,
    override val id: Any? = null
) : GroupNode(
    orientation = Orientation.HORIZONTAL,
    gap = gap,
    crossAlign = CrossAlign.CENTER,
    padding = padding
) {

    override fun buildChildren(): List<LayoutNode> =
        labels.map { label ->
            TextNode(
                text = BigText(label),
                textSizePx = textSizePx,
                color = textColor
            )
        }
}

/**
 * Ví dụ 2 — caller không truyền child node.
 *
 * Node nhận data/config đơn giản rồi tự dựng toàn bộ children trong
 * [buildChildren]. Đồng thời nó override [createSpec] để tự vẽ nền card.
 */
class SelfContainedGroup(
    private val title: String,
    private val subtitle: String,
    private val status: String,
    private val titleTextSizePx: Float = 16f,
    private val subtitleTextSizePx: Float = 13f,
    private val statusTextSizePx: Float = 12f,
    private val fillColor: Int = 0xFF263238.toInt(),
    private val accentColor: Int = 0xFF03DAC5.toInt(),
    private val cornerRadius: Float = 16f,
    padding: EdgeInsets = EdgeInsets.all(16),
    override val id: Any? = null,
    override val onClick: (() -> Unit)? = null
) : GroupNode(
    orientation = Orientation.VERTICAL,
    gap = 8,
    padding = padding,
    layoutWidth = LayoutDimension.MatchParent,
    onClick = onClick
) {

    override fun buildChildren(): List<LayoutNode> =
        listOf(
            TextNode(
                text = BigText(title),
                textSizePx = titleTextSizePx,
                color = Color.WHITE,
                typeface = Typeface.DEFAULT_BOLD,
                layoutWidth = LayoutDimension.MatchParent
            ),
            TextNode(
                text = BigText(subtitle),
                textSizePx = subtitleTextSizePx,
                color = 0xCCFFFFFF.toInt(),
                layoutWidth = LayoutDimension.MatchParent
            ),
            TextNode(
                text = BigText(status),
                textSizePx = statusTextSizePx,
                color = accentColor,
                typeface = Typeface.DEFAULT_BOLD,
                padding = EdgeInsets.symmetric(h = 10, v = 4),
                layoutWidth = LayoutDimension.MatchParent
            )
        )

    override fun createSpec(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        placedChildren: List<DrawSpec>
    ): GroupSpec =
        CardGroupSpec(
            left = x,
            top = y,
            width = width,
            height = height,
            children = placedChildren,
            node = this,
            fillColor = fillColor,
            cornerRadius = cornerRadius
        )
}

/**
 * Ví dụ 3 — thêm padding thụt lề cho từng child.
 *
 * B kế thừa A và **override [transformChild]** để bọc mỗi child trong một
 * [GroupNode] con có thêm padding riêng. Đây là pattern đơn giản nhất để
 * chèn "trang trí" xung quanh mỗi child mà không đụng tới bản thân child.
 */
class IndentedColumn(
    initialChildren: List<LayoutNode>,
    private val itemPadding: EdgeInsets = EdgeInsets.all(12),
    gap: Int = 8,
    padding: EdgeInsets = EdgeInsets.ZERO,
    override val id: Any? = null
) : GroupNode(
    orientation = Orientation.VERTICAL,
    gap = gap,
    padding = padding,
    initialChildren = initialChildren,
    layoutWidth = LayoutDimension.MatchParent
) {

    override fun transformChild(index: Int, child: LayoutNode): LayoutNode =
        GroupNode(
            orientation = Orientation.VERTICAL,
            padding = itemPadding,
            layoutWidth = LayoutDimension.MatchParent,
            initialChildren = listOf(child)
        )
}

/**
 * Ví dụ 4 — chèn header / separator vào list.
 *
 * B kế thừa A và **override [finalizeChildren]** để prepend một [TextNode]
 * làm header và chèn [SpaceNode] xen kẽ giữa các item — không đụng tới
 * items gốc.
 */
class SectionGroup(
    private val headerTitle: String,
    initialChildren: List<LayoutNode>,
    private val headerTextSizePx: Float = 16f,
    private val headerTextColor: Int = Color.DKGRAY,
    private val headerPaddingVerticalPx: Int = 8,
    private val separatorSize: Int = 1,
    padding: EdgeInsets = EdgeInsets.ZERO,
    override val id: Any? = null
) : GroupNode(
    orientation = Orientation.VERTICAL,
    gap = 0,
    padding = padding,
    initialChildren = initialChildren,
    layoutWidth = LayoutDimension.MatchParent
) {

    override fun finalizeChildren(transformed: List<LayoutNode>): List<LayoutNode> {

        if (transformed.isEmpty()) return listOf(headerNode())
        val out = ArrayList<LayoutNode>(transformed.size * 2 + 1)
        out.add(headerNode())
        for (i in transformed.indices) {
            out.add(transformed[i])
            if (i < transformed.lastIndex) out.add(separatorNode())
        }
        return out
    }

    private fun headerNode(): LayoutNode =
        TextNode(
            text = BigText(headerTitle),
            textSizePx = headerTextSizePx,
            color = headerTextColor,
            typeface = Typeface.DEFAULT_BOLD,
            padding = EdgeInsets.symmetric(v = headerPaddingVerticalPx),
            layoutWidth = LayoutDimension.MatchParent
        )

    private fun separatorNode(): LayoutNode =
        SpaceNode(
            layoutWidth = LayoutDimension.MatchParent,
            layoutHeight = LayoutDimension.Fixed(separatorSize)
        )
}

/**
 * Ví dụ 5 — subclass **cả GroupNode lẫn GroupSpec** để tự vẽ nền bo góc
 * bao quanh children mà không cần dựng thêm [com.simple.ui.precompute.node.BackgroundNode]
 * hay [com.simple.ui.precompute.node.ConstraintNode] chồng chéo.
 *
 * Pattern: override [GroupNode.createSpec] để trả về một [GroupSpec] tùy
 * biến. Spec đó override [GroupSpec.drawBackground] để vẽ round-rect vào
 * bounds local `(0, 0, width, height)`.
 *
 * ⚠️ Vì [CardGroupSpec] có state riêng ([fillColor], [cornerRadius], paint
 * cached), nó bắt buộc override [DrawSpec.withPosition] để clone không mất
 * state khi engine reposition từ cache.
 */
class CardGroup(
    initialChildren: List<LayoutNode>,
    private val fillColor: Int = Color.WHITE,
    private val cornerRadius: Float = 16f,
    padding: EdgeInsets = EdgeInsets.all(16),
    override val id: Any? = null
) : GroupNode(
    orientation = Orientation.VERTICAL,
    gap = 8,
    padding = padding,
    initialChildren = initialChildren,
    layoutWidth = LayoutDimension.MatchParent
) {

    override fun createSpec(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        placedChildren: List<DrawSpec>
    ): GroupSpec =
        CardGroupSpec(
            left = x,
            top = y,
            width = width,
            height = height,
            children = placedChildren,
            node = this,
            fillColor = fillColor,
            cornerRadius = cornerRadius
        )
}

/**
 * Spec đi kèm [CardGroup] — subclass [GroupSpec] để tự vẽ nền bo góc trước
 * khi vẽ children.
 */
class CardGroupSpec(
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    children: List<DrawSpec>,
    node: LayoutNode,
    private val fillColor: Int,
    private val cornerRadius: Float
) : GroupSpec(left, top, width, height, node, children) {

    private val fillPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }

    private val rect = RectF()

    override fun drawBackground(canvas: Canvas) {

        if (width <= 0 || height <= 0) return
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)
    }

    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec =
        CardGroupSpec(
            left = newLeft,
            top = newTop,
            width = width,
            height = height,
            children = children,
            node = node,
            fillColor = fillColor,
            cornerRadius = cornerRadius
        )

    override fun collectDrawAndHitSpecs(
        draws: MutableList<DrawSpec>,
        hits: MutableList<DrawSpec>,
        currentDepth: Int
    ) {

        depth = currentDepth
        draws.add(this)
        if (isClickable) hits.add(this)

        val childDepth = currentDepth + 1
        for (i in children.indices) {
            children[i].collectDrawAndHitSpecs(draws, hits, childDepth)
        }
    }

    override fun collectDraws(out: MutableList<DrawSpec>) {
        val hits = ArrayList<DrawSpec>()
        collectDrawAndHitSpecs(out, hits)
    }

    override fun collectHits(out: MutableList<DrawSpec>) {
        val draws = ArrayList<DrawSpec>()
        collectDrawAndHitSpecs(draws, out)
    }
}

class SelfContainedGroupNode(
    private val title: String,
    private val subtitle: String,
    private val status: String,
    private val titleTextSizePx: Float = 16f,
    private val subtitleTextSizePx: Float = 13f,
    private val statusTextSizePx: Float = 12f,
    private val accentColor: Int = 0xFF03DAC5.toInt(),
    padding: EdgeInsets = EdgeInsets.all(16),
    override val id: Any? = null,
    override val onClick: (() -> Unit)? = null
) : GroupNode(
    orientation = Orientation.VERTICAL,
    gap = 8,
    padding = padding,
    layoutWidth = LayoutDimension.MatchParent,
    onClick = onClick
) {

    override fun buildChildren(): List<LayoutNode> =
        listOf(
            TextNode(
                text = BigText(title),
                textSizePx = titleTextSizePx,
                color = Color.WHITE,
                typeface = Typeface.DEFAULT_BOLD,
                layoutWidth = LayoutDimension.MatchParent
            ),
            TextNode(
                text = BigText(subtitle),
                textSizePx = subtitleTextSizePx,
                color = 0xCCFFFFFF.toInt(),
                layoutWidth = LayoutDimension.MatchParent
            ),
            TextNode(
                text = BigText(status),
                textSizePx = statusTextSizePx,
                color = accentColor,
                typeface = Typeface.DEFAULT_BOLD,
                padding = EdgeInsets.symmetric(h = 10, v = 4),
                layoutWidth = LayoutDimension.MatchParent
            )
        )

    override fun createSpec(x: Int, y: Int, width: Int, height: Int, placedChildren: List<DrawSpec>): GroupSpec = SelfContainedGroupSpec(
        left = x,
        top = y,
        width = width,
        height = height,
        children = placedChildren,
        node = this,
    )
}


class SelfContainedGroupSpec(
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    children: List<DrawSpec>,
    node: LayoutNode,
) : GroupSpec(
    left,
    top,
    width,
    height,
    node,
    children
) {

    init {
        onClick = {

        }
    }

    override fun onAttachedToWindow(view: View) {
        super.onAttachedToWindow(view)
    }

    override fun onDetachedFromWindow(view: View) {
        super.onDetachedFromWindow(view)
    }
}
