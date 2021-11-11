package com.termux.gui

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.Serializable
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

open class GUIActivity : AppCompatActivity() {
    
    
    // a Tree to store the view hierarchy. The existence of View s isn't stored by android itself
    private data class Node(val parent: Node?, val id: Int, val claz: Class<out View>, val children: LinkedList<Node> = LinkedList<Node>()) : Serializable
    
    
    
    companion object {
        private const val THEME_KEY = "tgui_heme"
        private const val IDS_KEY = "gui_used_ids"
        private const val VIEWS_KEY = "gui_views"
        private const val DATA_KEY = "gui_data"
    }
    val usedIds: TreeSet<Int> = TreeSet()
    init {
        usedIds.add(R.id.root)
    }
    var theme: GUITheme? = null
        set(t) {
            field = t
            if (t != null) {
                window.decorView.background = ColorDrawable(t.windowBackground)
                window.statusBarColor = t.statusBarColor
            }
        }
    
    data class ActivityData(var autopip: Boolean = false) : Serializable
    var data = ActivityData()
    
    data class GUITheme(val statusBarColor: Int, val colorPrimary: Int, var windowBackground: Int, val textColor: Int, val colorAccent: Int) : Serializable
    var eventQueue : LinkedBlockingQueue<ConnectionHandler.Event>? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("oncreate activity")
        if (intent.getBooleanExtra("pip", false)) {
            println("pip")
            setTheme(R.style.Theme_TermuxGUI_NoAnimation)
            @Suppress("DEPRECATION")
            enterPictureInPictureMode()
            overridePendingTransition(0,0)
        } else {
            setTheme(R.style.Theme_TermuxGUI)
        }
        setContentView(R.layout.activity_gui)
        if (savedInstanceState != null) {
            theme = savedInstanceState.getSerializable(THEME_KEY) as? GUITheme?
            val ids = savedInstanceState.getSerializable(IDS_KEY) as? TreeSet<*>
            if (ids != null) {
                val l = ids.filterIsInstance(Int::class.java)
                usedIds.clear()
                for (id in l) {
                    usedIds.add(id)
                }
            }
            val d = savedInstanceState.getSerializable(DATA_KEY) as? ActivityData
            if (d != null) {
                data = d
            }
            val tree = savedInstanceState.getSerializable(VIEWS_KEY) as? Node
            if (tree != null) {
                findViewById<FrameLayout>(R.id.root).addView(buildHierarchyFromTree(tree))
            }
        }
    }

    private fun buildHierarchyFromTree(tree: Node): View {
        val v = tree.claz.getConstructor(Context::class.java).newInstance(this)
        v.id = tree.id
        //println("untree-ed ${v::class}")
        if (v is ViewGroup) {
            for (n in tree.children) {
                v.addView(buildHierarchyFromTree(n))
            }
        }
        return v
    }

    @Suppress("DEPRECATION")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        val ev = eventQueue
        if (ev != null) {
            try {
                ev.add(ConnectionHandler.Event("pipchanged", ConnectionHandler.gson.toJsonTree(isInPictureInPictureMode)))
            } catch (ignored: Exception) {}
        }
    }

    @Suppress("DEPRECATION")
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val ev = eventQueue
        if (ev != null) {
            try {
                ev.add(ConnectionHandler.Event("UserLeaveHint", null))
            } catch (ignored: Exception) {}
            if (data.autopip) {
                enterPictureInPictureMode()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(THEME_KEY, theme)
        outState.putSerializable(IDS_KEY, usedIds)
        val root = findViewById<FrameLayout>(R.id.root).getChildAt(0)
        if (root != null) {
            val tree = createViewTree(root, null)
            outState.putSerializable(VIEWS_KEY, tree)
        }
        outState.putSerializable(DATA_KEY, data)
    }
    
    private fun createViewTree(start: View, parent: Node?) : Node {
        val children = LinkedList<Node>()
        if (start !is ViewGroup) {
            //println("tree-ed ${start::class}")
            return Node(parent, start.id, start::class.java, children)
        }
        val tree = Node(parent, start.id, start::class.java, children)
        for (i in 0 until start.childCount) {
            children.add(createViewTree(start.getChildAt(i), tree))
        }
        //println("tree-ed ${start::class}")
        return tree
    }
    
    
    
    
}