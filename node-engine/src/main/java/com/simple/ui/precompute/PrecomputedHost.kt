package com.simple.ui.precompute

interface PrecomputedHost {

    val delegate: PrecomputedDelegate

    /** Kết quả đo phẳng từ [LayoutEngine.measure] hoặc [LayoutEngine.build]. */
    var result: LayoutResult?
        get() = delegate.result
        set(value) {
            delegate.result = value
        }
}
