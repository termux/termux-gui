package com.termux.gui

import android.app.ActivityManager
import android.content.Context
import android.net.LocalSocket
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.termux.gui.protocol.v0.GUIRecyclerViewAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.DataOutputStream
import java.io.FileDescriptor
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.roundToInt

class Util {
    companion object {
        
        fun getTaskInfo(tasks: LinkedList<ActivityManager.AppTask>, task: ActivityManager.AppTask): ActivityManager.RecentTaskInfo? {
            try {
                return task.taskInfo
            } catch (e: IllegalArgumentException) {
                tasks.remove(task)
            }
            return null
        }
        
        @Suppress("DEPRECATION")
        fun getTaskId(info: ActivityManager.RecentTaskInfo): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                info.taskId
            } else {
                info.id
            }
        }
        
        fun toPX(a: Context, dip: Int): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip.toFloat(), a.resources.displayMetrics).roundToInt()
        }
        
        fun sendMessageFd(w: DataOutputStream, ret: String, s: LocalSocket, fd: FileDescriptor) {
            val bytes = ret.toByteArray(StandardCharsets.UTF_8)
            w.writeInt(bytes.size)
            s.setFileDescriptorsForSend(arrayOf(fd))
            w.write(bytes)
            w.flush()
        }

        fun sendMessage(w: DataOutputStream, ret: String) {
            val bytes = ret.toByteArray(StandardCharsets.UTF_8)
            w.writeInt(bytes.size)
            w.write(bytes)
            w.flush()
        }

        fun generateViewID(rand: Random, a: GUIActivity): Int {
            return generateViewIDRaw(rand, a.usedIds)
        }

        fun generateViewIDRaw(rand: Random, usedIds: MutableSet<Int>): Int {
            var id = rand.nextInt(Integer.MAX_VALUE)
            while (usedIds.contains(id)) {
                id = rand.nextInt(Integer.MAX_VALUE)
            }
            usedIds.add(id)
            return id
        }

        fun runOnUIThreadBlocking(r: Runnable) {
            var e: Exception? = null
            runBlocking(Dispatchers.Main) {
                launch {
                    try {
                        r.run()
                    } catch (ex: java.lang.Exception) {
                        e = ex
                    }
                }
            }
            val ex = e
            if (ex != null) {
                throw ex
            }
        }
        
        fun setViewActivity(a: GUIActivity, v: View, parent: Int?, recid: Int?, recindex: Int?) {
            val t = a.theme
            if (t != null) {
                if (v is TextView) {
                    v.setTextColor(t.textColor)
                }
            }
            if (parent == null) {
                if (t != null) {
                    v.setBackgroundColor(t.windowBackground)
                }
                val fl = a.findViewReimplemented(R.id.root, recid, recindex) as? FrameLayout
                fl?.removeAllViews()
                fl?.addView(v)
            } else {
                val g = a.findViewReimplemented<ViewGroup>(parent, recid, recindex)
                if (g is LinearLayout) {
                    val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1F)
                    if (g.orientation == LinearLayout.VERTICAL) {
                        p.width = LinearLayout.LayoutParams.MATCH_PARENT
                    } else {
                        p.height = LinearLayout.LayoutParams.MATCH_PARENT
                    }
                    v.layoutParams = p
                }
                g?.addView(v)
            }
        }
        
        fun removeViewRecursive(v: View?, usedIds: MutableSet<Int>, recyclerviews: HashMap<Int, GUIRecyclerViewAdapter>) {
            if (v != null) {
                if (v is ViewGroup) {
                    if (v is RecyclerView) {
                        val rv = recyclerviews[v.id]
                        if (rv != null) {
                            usedIds.removeAll(rv.exportViewList().map { it.id })
                            recyclerviews.remove(v.id)
                        }
                    } else {
                        while (v.childCount > 0) {
                            removeViewRecursive(v.getChildAt(0), usedIds, recyclerviews)
                        }
                    }
                }
                val p = v.parent
                if (p is ViewGroup) {
                    p.removeView(v)
                }
                usedIds.remove(v.id)
            }
        }
    }
}