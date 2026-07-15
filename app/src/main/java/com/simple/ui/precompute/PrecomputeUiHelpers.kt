package com.simple.ui.precompute

import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.simple.t.R
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.PrecomputedView

class PrecomputeUiHelpers(
    private val activity: AppCompatActivity,
) {

    private val dp by lazy { activity.resources.displayMetrics.density }

    fun addSectionLabel(container: LinearLayout, label: String) {
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(20) }

        container.addView(TextView(activity).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setBackgroundColor(0xFF6200EE.toInt())
        }, lp)
    }

    fun addCards(container: LinearLayout, specs: List<DrawSpec>) {
        specs.forEachIndexed { index, spec ->
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = if (index == 0) dp(10) else dp(10) }

            container.addView(PrecomputedView(activity).apply {
                this.spec = spec
                setBackgroundResource(R.drawable.card_background)
                elevation = 2 * dp
            }, lp)
        }
    }

    fun dp(value: Int): Int = (value * dp).toInt()

    fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, activity.resources.displayMetrics)
}
