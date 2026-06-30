package com.simple.ui.precompute

interface PrecomputedHost {

    val delegate: PrecomputedDelegate

    var spec: DrawSpec?
        get() = delegate.spec
        set(value) {
            delegate.spec = value
        }
}