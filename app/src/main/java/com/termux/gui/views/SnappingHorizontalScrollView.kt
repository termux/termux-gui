package com.termux.gui.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs


@SuppressLint("ClickableViewAccessibility")
class SnappingHorizontalScrollView(c: Context) : HorizontalScrollView(c) {
    private val SWIPE_MIN_DISTANCE = 5
    private val SWIPE_THRESHOLD_VELOCITY = 300
    
    private var mActiveFeature = 0
    
    
    init {
        val mGestureDetector = GestureDetectorCompat(c, MyGestureDetector())
        setOnTouchListener { v, event -> //If the user swipes
            if (mGestureDetector.onTouchEvent(event)) {
                true
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                val scrollX = scrollX
                val featureWidth = v.measuredWidth
                mActiveFeature = (scrollX + featureWidth / 2) / featureWidth
                val scrollTo = mActiveFeature * featureWidth
                smoothScrollTo(scrollTo, 0)
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
                //right to left
                if (e1.x - e2.x > SWIPE_MIN_DISTANCE && abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    val featureWidth: Int = measuredWidth
                    mActiveFeature = if (mActiveFeature < layout.childCount - 1) mActiveFeature + 1 else layout.childCount - 1
                    smoothScrollTo(mActiveFeature * featureWidth, 0)
                    return true
                } else if (e2.x - e1.x > SWIPE_MIN_DISTANCE && abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    val featureWidth: Int = measuredWidth
                    mActiveFeature = if (mActiveFeature > 0) mActiveFeature - 1 else 0
                    smoothScrollTo(mActiveFeature * featureWidth, 0)
                    return true
                }
            } catch (ignored: Exception) {}
            return false
        }
    }
}