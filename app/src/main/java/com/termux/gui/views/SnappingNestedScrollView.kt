package com.termux.gui.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.core.widget.NestedScrollView
import kotlin.math.abs


@SuppressLint("ClickableViewAccessibility")
class SnappingNestedScrollView(c: Context) : NestedScrollView(c) {
    private val SWIPE_MIN_DISTANCE = 5
    private val SWIPE_THRESHOLD_VELOCITY = 300

    private var mActiveFeature = 0


    init {
        val mGestureDetector = GestureDetectorCompat(c, MyGestureDetector())
        setOnTouchListener { v, event -> //If the user swipes
            if (mGestureDetector.onTouchEvent(event)) {
                true
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                val scrollY = scrollY
                val featureHeight = v.measuredHeight
                mActiveFeature = (scrollY + featureHeight / 2) / featureHeight
                val scrollTo = mActiveFeature * featureHeight
                smoothScrollTo(0, scrollTo)
                true
            } else {
                false
            }
        }
    }

    internal inner class MyGestureDetector : SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null || e2 == null) {
                return false
            }
            try {
                val layout = getChildAt(0) as? ViewGroup ?: return false
                if (e1.y - e2.y > SWIPE_MIN_DISTANCE && abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    val featureHeight: Int = measuredHeight
                    mActiveFeature = if (mActiveFeature < layout.childCount - 1) mActiveFeature + 1 else layout.childCount - 1
                    println(mActiveFeature)
                    smoothScrollTo(0, mActiveFeature * featureHeight)
                    return true
                } else if (e2.y - e1.y > SWIPE_MIN_DISTANCE && abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    val featureHeight: Int = measuredHeight
                    mActiveFeature = if (mActiveFeature > 0) mActiveFeature - 1 else 0
                    smoothScrollTo(0, mActiveFeature * featureHeight)
                    return true
                }
            } catch (ignored: Exception) {ignored.printStackTrace()}
            return false
        }
    }
}