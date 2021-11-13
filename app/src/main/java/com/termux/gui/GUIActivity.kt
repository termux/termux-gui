package com.termux.gui

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.termux.gui.protocol.v0.GUIRecyclerViewAdapter
import java.io.Serializable
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.HashMap

open class GUIActivity : AppCompatActivity() {
    
    
    // a Tree to store the view hierarchy. The existence of View s isn't stored by android itself
    private data class Node(val parent: Node?, val id: Int, val claz: Class<out View>, val children: LinkedList<Node> = LinkedList<Node>()) : Serializable
    
    
    
    companion object {
        private const val THEME_KEY = "tgui_heme"
        private const val IDS_KEY = "gui_used_ids"
        private const val VIEWS_KEY = "gui_views"
        private const val DATA_KEY = "gui_data"
        private const val RECYCLERVIEWS_KEY = "rec_data"
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

    val recyclerviews = HashMap<Int, GUIRecyclerViewAdapter>()

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("oncreate activity")
        if (intent.getBooleanExtra("pip", false)) {
            println("pip")
            setTheme(R.style.Theme_TermuxGUI_NoAnimation)
            @Suppress("DEPRECATION")
            enterPictureInPictureMode()
            overridePendingTransition(0,0)
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
            val recs = savedInstanceState.getSerializable(RECYCLERVIEWS_KEY) as? HashMap<Int, Pair<LinkedList<GUIRecyclerViewAdapter.ViewData>, Pair<LinkedList<Node>, LinkedList<SparseArray<Parcelable>>>>>
            if (recs != null) {
                for (r in recs) {
                    val rec = GUIRecyclerViewAdapter(this)
                    for (i in 0 until r.value.first.size) {
                        r.value.first[i].v = buildHierarchyFromTree(r.value.second.first[i])
                        r.value.first[i].v.restoreHierarchyState(r.value.second.second[i])
                    }
                    rec.importViewList(r.value.first)
                }
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
        val recs = HashMap<Int, Pair<LinkedList<GUIRecyclerViewAdapter.ViewData>, Pair<LinkedList<Node>, LinkedList<SparseArray<Parcelable>>>>>()
        for (r in recyclerviews) {
            val l = r.value.exportViewList()
            val n = LinkedList<Node>()
            val pl = LinkedList<SparseArray<Parcelable>>()
            for (v in l) {
                val p = SparseArray<Parcelable>()
                val view = v.v
                n.add(createViewTree(view, null))
                view.saveHierarchyState(p)
                pl.add(p)
            }
            recs[r.key] = Pair(l, Pair(n, pl))
        }
        outState.putSerializable(RECYCLERVIEWS_KEY, recs)
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
    
    @Suppress("UNCHECKED_CAST")
    fun <T> findViewReimplemented(id: Int, recid: Int?, recindex: Int?) : T? {
        if (recid != null && recindex != null) {
            val rec = recyclerviews[recid]
            if (rec != null) {
                val el = rec.getViewByIndex(recindex)
                if (el != null) {
                    return el.v.findViewById<View>(id) as? T
                }
            }
            return null
        }
        return findViewById(id)
    }
    
    
}