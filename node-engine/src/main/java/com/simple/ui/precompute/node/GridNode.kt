package com.simple.ui.precompute.node

import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.MeasureContext
import kotlin.math.max

/**
 * A container that lays out children in a grid with a fixed number of [columnCount].
 *
 * - [columnCount]: Number of columns in the grid.
 * - [columnGap]: Horizontal space between columns.
 * - [rowGap]: Vertical space between rows.
 * - [padding]: Padding around the entire grid.
 */
data class GridNode(
    val columnCount: Int,
    val children: List<LayoutNode>,
    val columnGap: Int = 0,
    val rowGap: Int = 0,
    override val padding: EdgeInsets = EdgeInsets.ZERO,
    override val layoutWidth: LayoutDimension = LayoutDimension.WrapContent,
    override val layoutHeight: LayoutDimension = LayoutDimension.WrapContent,
    override val id: Any? = null,
    override val onClick: (() -> Unit)? = null
) : LayoutNode() {

    init {
        require(columnCount > 0) { "columnCount must be > 0, was $columnCount" }
        require(columnGap >= 0) { "columnGap must be >= 0, was $columnGap" }
        require(rowGap >= 0) { "rowGap must be >= 0, was $rowGap" }
    }

    override fun measure(ctx: MeasureContext, c: Constraints, x: Int, y: Int): DrawSpec {
        val p = padding
        val measureMaxW = layoutWidth.maxForMeasure(c.maxWidth)
        val measureMaxH = layoutHeight.maxForMeasure(c.maxHeight)
        
        val innerMaxW = (measureMaxW - p.horizontal).coerceAtLeast(0)
        val innerMaxH = (measureMaxH - p.vertical).coerceAtLeast(0)

        // Calculate column width based on available width
        val totalColumnGap = columnGap * (columnCount - 1)
        
        // If width is WrapContent and we don't have a fixed maxWidth, we might need a different strategy.
        // But in this engine, usually we have a maxWidth from the screen.
        val resolvedInnerMaxW = if (innerMaxW == Int.MAX_VALUE) {
            // Fallback: if we can't determine width, we can't divide it.
            // This is a rare case for a GridNode.
            0 
        } else {
            innerMaxW
        }

        val columnWidth = (resolvedInnerMaxW - totalColumnGap).coerceAtLeast(0) / columnCount

        val rowCount = (children.size + columnCount - 1) / columnCount
        val measuredChildren = arrayOfNulls<DrawSpec>(children.size)
        val rowHeights = IntArray(rowCount) { 0 }

        // Measure all children
        children.forEachIndexed { index, child ->
            val rowIndex = index / columnCount
            val childConstraints = Constraints(columnWidth, innerMaxH)
            val spec = ctx.measure(child, childConstraints, 0, 0)
            
            // For a grid, we usually want items to fill the column width
            val finalSpec = spec.withSize(columnWidth, spec.height)
            
            measuredChildren[index] = finalSpec
            rowHeights[rowIndex] = max(rowHeights[rowIndex], finalSpec.height)
        }

        val totalRowGap = if (rowCount > 0) (rowCount - 1) * rowGap else 0
        val naturalContentHeight = rowHeights.sum() + totalRowGap

        val finalWidth = layoutWidth.resolve(resolvedInnerMaxW + p.horizontal, c.maxWidth)
        val finalHeight = layoutHeight.resolve(naturalContentHeight + p.vertical, c.maxHeight)

        val innerW = (finalWidth - p.horizontal).coerceAtLeast(0)
        val actualColumnWidth = (innerW - totalColumnGap).coerceAtLeast(0) / columnCount

        // Position children
        val placed = ArrayList<DrawSpec>(children.size)
        var currentY = y + p.top
        
        for (rowIndex in 0 until rowCount) {
            var currentX = x + p.left
            val rowHeight = rowHeights[rowIndex]
            
            for (colIndex in 0 until columnCount) {
                val childIndex = rowIndex * columnCount + colIndex
                if (childIndex >= children.size) break
                
                val spec = measuredChildren[childIndex]!!
                
                // If the actual column width differs from what we used for measurement (e.g. due to MatchParent resolving)
                val finalSpec = if (spec.width != actualColumnWidth) {
                    spec.withSize(actualColumnWidth, spec.height)
                } else {
                    spec
                }
                
                placed.add(finalSpec.withPosition(currentX, currentY))
                
                currentX += actualColumnWidth + columnGap
            }
            currentY += rowHeight + rowGap
        }

        return GroupSpec(x, y, finalWidth, finalHeight, this, placed)
    }
}
