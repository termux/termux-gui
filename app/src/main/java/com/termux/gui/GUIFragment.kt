package com.termux.gui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import java.util.*

class GUIFragment : Fragment() {

    val usedIds: TreeSet<Int> = TreeSet()
    

    private var root: FrameLayout? = null
    
    init {
        @Suppress("DEPRECATION")
        retainInstance = true
    }
    var theme: GUIActivity.GUITheme? = null
        set(t) {
            field = t
            if (t != null) {
                requireActivity().window.decorView.background = ColorDrawable(t.windowBackground)
                requireActivity().window.statusBarColor = t.statusBarColor
            }
        }
    
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        //println("createView")
        if (root == null) {
            root = FrameLayout(requireActivity())
        }
        return root as FrameLayout
    }
    
    
    
    
}