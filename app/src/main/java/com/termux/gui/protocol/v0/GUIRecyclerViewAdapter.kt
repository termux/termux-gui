package com.termux.gui.protocol.v0

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.termux.gui.GUIActivity
import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap

class GUIRecyclerViewAdapter(private val a: GUIActivity) : RecyclerView.Adapter<GUIRecyclerViewAdapter.GUIViewHolder>() {
    class GUIViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class ViewData(val index: Int, val id: Int, @Transient var v: View) : Serializable
    
    
    
    
    private val views = SortedList(ViewData::class.java, object: SortedList.Callback<ViewData>() {
        override fun compare(o1: ViewData?, o2: ViewData?): Int {
            if (o1 == null || o2 == null) {
                return 0
            }
            return o1.index - o2.index
        }
        override fun onInserted(position: Int, count: Int) {
            notifyItemRangeInserted(position, count)
        }
        override fun onRemoved(position: Int, count: Int) {
            notifyItemRangeRemoved(position, count)
        }
        override fun onMoved(fromPosition: Int, toPosition: Int) {
            notifyItemMoved(fromPosition, toPosition)
        }
        override fun onChanged(position: Int, count: Int) {
            notifyItemRangeChanged(position, count)
        }
        override fun areContentsTheSame(oldItem: ViewData?, newItem: ViewData?): Boolean {
            return areItemsTheSame(oldItem, newItem)
        }
        override fun areItemsTheSame(item1: ViewData?, item2: ViewData?): Boolean {
            if (item1 == null || item2 == null) {
                return false
            }
            return item1.index == item2.index
        }
    })
    
    @SuppressLint("NotifyDataSetChanged")
    fun clearViews() {
        views.clear()
        notifyDataSetChanged()
    }
    

    fun getViewByIndex(index: Int) : ViewData? {
        return try {
            views.get(index)
        } catch (e: ArrayIndexOutOfBoundsException) {
            null
        }
    }
    
    fun setViewByIndex(index: Int, v: View) {
        val vd = views.get(index)
        if (vd != null) {
            vd.v = v
            notifyItemChanged(index)
        } else {
            val newvd = ViewData(index, v.id, v)
            views.add(newvd)
            notifyItemChanged(newvd.index)
        }
    }
    
    
    fun exportViewList(): LinkedList<ViewData> {
        val l = LinkedList<ViewData>()
        return l
    }
    
    fun importViewList(l: LinkedList<ViewData>) {
        clearViews()
        val sorted = l.sortedBy { it.index }
        views.addAll(sorted)
    }
    
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GUIViewHolder {
        return GUIViewHolder(FrameLayout(a))
    }

    override fun onBindViewHolder(holder: GUIViewHolder, position: Int) {
        val f = holder.itemView as FrameLayout
        f.removeAllViews()
        f.addView(views[position].v)
    }

    override fun getItemCount(): Int {
        return views.size()
    }
}