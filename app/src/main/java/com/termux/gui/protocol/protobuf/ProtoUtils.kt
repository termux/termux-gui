package com.termux.gui.protocol.protobuf

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.google.protobuf.MessageLite
import com.termux.gui.App
import com.termux.gui.ConnectionHandler
import com.termux.gui.GUIActivity
import com.termux.gui.Util
import com.termux.gui.protocol.protobuf.v0.GUIProt0
import com.termux.gui.protocol.protobuf.v0.GUIProt0.Create
import com.termux.gui.protocol.protobuf.v0.V0Proto
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.V0Shared
import java.io.OutputStream
import java.lang.Exception
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.HashMap
import kotlin.math.roundToInt

class ProtoUtils {
    companion object {
        fun <T : MessageLite> viewActionOrFail(main: OutputStream, activities: Map<String, DataClasses.ActivityState>,
                                               overlays: Map<String, DataClasses.Overlay>, aid: String, ifActivity: (a: GUIActivity) -> T,
                                               ifOverlay: (o: DataClasses.Overlay) -> T, ifFail: () -> T) {
            val s = activities[aid]
            if (s != null) {
                val a = s.a
                if (a != null) {
                    ifActivity(a)
                } else {
                    ifFail()
                }
            } else {
                val o = overlays[aid]
                if (o != null) {
                    ifOverlay(o)
                } else {
                    ifFail()
                }
            }.writeDelimitedTo(main)
        }
        
        fun <T : MessageLite.Builder> write(builder: T, out: OutputStream) {
            write(builder.build(), out)
        }

        fun <T : MessageLite> write(m: T, out: OutputStream) {
            m.writeDelimitedTo(out)
        }
        
        fun setClickListener(v: View, aid: Int, enabled: Boolean, eventQueue: LinkedBlockingQueue<GUIProt0.Event>) {
            if (enabled) {
                val c = GUIProt0.ClickEvent.newBuilder()
                c.setV(GUIProt0.View.newBuilder().setAid(aid).setId(v.id))
                v.setOnClickListener {
                    if (v is CompoundButton) {
                        c.set = v.isChecked
                    }
                    eventQueue.offer(GUIProt0.Event.newBuilder().setClick(c).build())
                }
            } else {
                v.setOnClickListener(null)
            }
        }

        fun setFocusChangeListener(v: View, aid: Int, enabled: Boolean, eventQueue: LinkedBlockingQueue<GUIProt0.Event>) {
            if (enabled) {
                val c = GUIProt0.FocusChangeEvent.newBuilder()
                c.setV(GUIProt0.View.newBuilder().setAid(aid).setId(v.id))
                v.setOnFocusChangeListener { _, hasFocus ->
                    c.focus = hasFocus
                    eventQueue.offer(GUIProt0.Event.newBuilder().setFocusChange(c).build())
                }
            } else {
                v.onFocusChangeListener = null
            }
        }

        fun setLongClickListener(v: View, aid: Int, enabled: Boolean, eventQueue: LinkedBlockingQueue<GUIProt0.Event>) {
            if (enabled) {
                val c = GUIProt0.LongClickEvent.newBuilder()
                c.setV(GUIProt0.View.newBuilder().setAid(aid).setId(v.id))
                v.setOnLongClickListener {
                    eventQueue.offer(GUIProt0.Event.newBuilder().setLongClick(c).build())
                }
            } else {
                v.setOnLongClickListener(null)
            }
        }

        fun setCheckedListener(v: RadioGroup, aid: Int, enabled: Boolean, eventQueue: LinkedBlockingQueue<GUIProt0.Event>) {
            if (enabled) {
                val c = GUIProt0.SelectedEvent.newBuilder()
                c.setV(GUIProt0.View.newBuilder().setAid(aid).setId(v.id))
                v.setOnCheckedChangeListener { _, checked ->
                    c.selected = checked
                    eventQueue.offer(GUIProt0.Event.newBuilder().setSelected(c).build())
                }
            } else {
                v.setOnCheckedChangeListener(null)
            }
        }

        fun setSpinnerListener(v: Spinner, aid: Int, eventQueue: LinkedBlockingQueue<GUIProt0.Event>) {
            v.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                val c = GUIProt0.ItemSelectedEvent.newBuilder()
                init {
                    c.setV(GUIProt0.View.newBuilder().setAid(aid).setId(v.id))
                }
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    c.selected = position
                    eventQueue.offer(GUIProt0.Event.newBuilder().setItemSelected(c).build())
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    c.selected = -1
                    eventQueue.offer(GUIProt0.Event.newBuilder().setItemSelected(c).build())
                }
            }
        }

        fun setRefreshListener(v: SwipeRefreshLayout, aid: Int, enabled: Boolean, eventQueue: LinkedBlockingQueue<GUIProt0.Event>) {
            if (enabled) {
                val c = GUIProt0.RefreshEvent.newBuilder()
                c.setV(GUIProt0.View.newBuilder().setAid(aid).setId(v.id))
                v.setOnRefreshListener {
                    eventQueue.offer(GUIProt0.Event.newBuilder().setRefresh(c).build())
                }
            } else {
                v.setOnRefreshListener(null)
            }
        }

        fun setTabSelectedListener(v: TabLayout, aid: Int, enabled: Boolean, eventQueue: LinkedBlockingQueue<GUIProt0.Event>) {
            v.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                val c = GUIProt0.ItemSelectedEvent.newBuilder()
                init {
                    c.setV(GUIProt0.View.newBuilder().setAid(aid).setId(v.id))
                }
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    if (tab != null) {
                        c.selected = tab.position
                        eventQueue.offer(GUIProt0.Event.newBuilder().setItemSelected(c).build())
                    }
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }

        @SuppressLint("DiscouragedPrivateApi")
        fun setTextWatcher(v: TextView, aid: Int, enabled: Boolean, eventQueue: LinkedBlockingQueue<GUIProt0.Event>) {
            if (enabled) {
                v.addTextChangedListener(object: TextWatcher {
                    val c = GUIProt0.TextEvent.newBuilder()
                    init {
                        c.setV(GUIProt0.View.newBuilder().setAid(aid).setId(v.id))
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (s != null) {
                            c.text = c.text
                            eventQueue.offer(GUIProt0.Event.newBuilder().setText(c).build())
                        }
                    }
                })
            } else (TextView::class.java.getDeclaredField("mListeners").get(v) as ArrayList<*>).clear()
        }

        private val TOUCH_EVENT_MAP: Map<Int, GUIProt0.TouchEvent.Action>
        init {
            val map = HashMap<Int, GUIProt0.TouchEvent.Action>()
            map[MotionEvent.ACTION_DOWN] = GUIProt0.TouchEvent.Action.down
            map[MotionEvent.ACTION_UP] = GUIProt0.TouchEvent.Action.up
            map[MotionEvent.ACTION_POINTER_DOWN] = GUIProt0.TouchEvent.Action.pointerDown
            map[MotionEvent.ACTION_POINTER_UP] = GUIProt0.TouchEvent.Action.pointerUp
            map[MotionEvent.ACTION_CANCEL] = GUIProt0.TouchEvent.Action.cancel
            map[MotionEvent.ACTION_MOVE] = GUIProt0.TouchEvent.Action.move
            TOUCH_EVENT_MAP = Collections.unmodifiableMap(map)
        }
        
        @SuppressLint("ClickableViewAccessibility")
        fun setTouchListenerProto(v: View, aid: Int, enabled: Boolean, eventQueue: LinkedBlockingQueue<GUIProt0.Event>) {
            if (enabled) {
                val c = GUIProt0.TouchEvent.newBuilder()
                c.setV(GUIProt0.View.newBuilder().setAid(aid).setId(v.id))
                data class PointerData(val x: Int, val y: Int, val id: Int)
                v.setOnTouchListener { _, event ->
                    val mapped = TOUCH_EVENT_MAP[event.actionMasked]
                    if (mapped != null) {
                        c.action = mapped
                        c.index = event.actionIndex
                        c.time = event.eventTime
                        val pd = LinkedList<PointerData>()
                        val rev = Matrix()
                        var inv = false
                        if (v is ImageView) {
                            inv = v.imageMatrix.invert(rev)
                        }
                        for (i in 0 until event.pointerCount) {
                            if (inv) {
                                // if it is an ImageView, automatically transform from view coordinates to image coordinates
                                val pos = FloatArray(2)
                                pos[0] = event.getX(i)
                                pos[1] = event.getY(i)
                                rev.mapPoints(pos)
                                pd.add(PointerData(pos[0].roundToInt(), pos[1].roundToInt(), event.getPointerId(i)))
                            } else {
                                pd.add(PointerData(event.getX(i).roundToInt(), event.getY(i).roundToInt(), event.getPointerId(i)))
                            }
                        }
                        c.clearTouches()
                        if (event.historySize == 0) {
                            for (p in pd) {
                                c.addTouches(GUIProt0.TouchEvent.Touch.newBuilder().addPointers(GUIProt0.TouchEvent.Touch.Pointer.newBuilder().setId(p.id).setX(p.x).setY(p.y)))
                            }
                            eventQueue.offer(GUIProt0.Event.newBuilder().setTouch(c).build())
                        } else {
                            val pdhl = LinkedList<LinkedList<PointerData>>()
                            for (i in 0 until event.historySize) {
                                val pdh = LinkedList<PointerData>()
                                for (a in 0 until event.pointerCount) {
                                    if (inv) {
                                        // if it is an ImageView, automatically transform from view coordinates to image coordinates
                                        val pos = FloatArray(2)
                                        pos[0] = event.getHistoricalX(a, i)
                                        pos[1] = event.getHistoricalY(a, i)
                                        rev.mapPoints(pos)
                                        pdh.add(PointerData(pos[0].roundToInt(), pos[1].roundToInt(), event.getPointerId(a)))
                                    } else {
                                        pdh.add(PointerData(event.getHistoricalX(a, i).roundToInt(), event.getHistoricalY(a, i).roundToInt(), event.getPointerId(a)))
                                    }
                                }
                                pdhl.add(pdh)
                            }
                            pdhl.add(pd)
                            val touches = LinkedList<GUIProt0.TouchEvent.Touch>()
                            for (h in pdhl) {
                                val t = GUIProt0.TouchEvent.Touch.newBuilder()
                                for (p in h) {
                                    t.addPointers(GUIProt0.TouchEvent.Touch.Pointer.newBuilder().setId(p.id).setX(p.x).setY(p.y))
                                }
                                touches.add(t.build())
                            }
                            c.addAllTouches(touches)
                            eventQueue.offer(GUIProt0.Event.newBuilder().setTouch(c).build())
                        }
                    }
                    true
                }
            } else {
                v.setOnTouchListener(null)
            }
        }
        
        class ViewCreator(
            val v: V0Proto,
            val main: OutputStream,
            val activities: MutableMap<Int, DataClasses.ActivityState>,
            val overlays: MutableMap<Int, DataClasses.Overlay>,
            val rand: Random
        ) {
            inline fun <reified T : View> createView(m: Any, crossinline init: (T) -> Unit) {
                val retClass = Class.forName(m.javaClass.canonicalName!!.replace("Request", "Response"))
                val create = m.javaClass.getMethod("getData").invoke(m) as Create
                val ret = retClass.getMethod("newBuilder").invoke(null)
                try {
                    val o = overlays[create.aid]
                    if (o != null) {
                        val v = createViewOverlay(o.usedIds, rand, create, init)
                        V0Shared.setViewOverlay(o, v, create.parent)
                        retClass.getMethod("setId", Int::class.java).invoke(ret, v.id)
                    } else {
                        if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[create.aid]) {
                                val v = createViewActivity<T>(it, rand, create) { view ->
                                    val t = it.theme
                                    if (t != null) {
                                        if (view is TextView) {
                                            view.setTextColor(t.textColor)
                                        }
                                    }
                                    init(view)
                                    Util.setViewActivity(it, view, create.parent)
                                }
                                retClass.getMethod("setId", Int::class.java).invoke(ret, v.id)
                            }) retClass.getMethod("setId", Int::class.java).invoke(ret, -1)
                    }
                } catch (e: Exception) {
                    Log.d(this.javaClass.name, "Exception: ", e)
                    retClass.getMethod("setId", Int::class.java).invoke(ret, -1)
                }
                write(ret as MessageLite.Builder, main)
            }
        }
        
        /*
        class ViewCreator(
            val v: V0Proto,
            val main: OutputStream,
            val activities: MutableMap<Int, DataClasses.ActivityState>,
            val overlays: MutableMap<Int, DataClasses.Overlay>,
            val rand: Random
        ) {
            inline fun <reified T : View, reified R : MessageLite> createView(create: Create, crossinline init: (T) -> Unit) {
                val ret = R::class.java.getMethod("newBuilder").invoke(null)
                try {
                    val o = overlays[create.aid]
                    if (o != null) {
                        val v = createViewOverlay(o.usedIds, rand, create, init)
                        V0Shared.setViewOverlay(o, v, create.parent)
                        R::class.java.getMethod("setSuccess", Boolean::class.java).invoke(ret, true)
                    } else {
                        if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[create.aid]) {
                                createViewActivity<T>(it, rand, create) { view ->
                                    val t = it.theme
                                    if (t != null) {
                                        if (view is TextView) {
                                            view.setTextColor(t.textColor)
                                        }
                                    }
                                    init(view)
                                    Util.setViewActivity(it, view, create.parent)
                                }
                                R::class.java.getMethod("setSuccess", Boolean::class.java).invoke(ret, true)
                            }) R::class.java.getMethod("setSuccess", Boolean::class.java).invoke(ret, false)
                    }
                } catch (e: Exception) {
                    Log.d(this.javaClass.name, "Exception: ", e)
                    R::class.java.getMethod("setSuccess", Boolean::class.java).invoke(ret, false)
                }
                write(ret as MessageLite.Builder, main)
            }
        }
         */
        inline fun <reified T : View> createViewActivity(a: GUIActivity, rand: Random, create: Create, init: (T) -> Unit): T {
            return createViewRaw(a, Util.generateViewID(rand, a), create, init)
        }

        inline fun <reified T : View> createViewOverlay(usedIds: TreeSet<Int>, rand: Random, create: Create, init: (T) -> Unit): T {
            return createViewRaw(App.APP!!, Util.generateViewIDRaw(rand, usedIds), create, init)
        }

        inline fun <reified T : View> createViewRaw(c: Context, id: Int, create: Create, init: (T) -> Unit): T {
            val v = T::class.java.getConstructor(Context::class.java).newInstance(c)
            v.id = id
            v.visibility = when (create.v) {
                GUIProt0.Visibility.visible -> View.VISIBLE
                GUIProt0.Visibility.hidden -> View.INVISIBLE
                GUIProt0.Visibility.gone -> View.GONE
                else -> View.VISIBLE
            }
            init(v)
            return v
        }
        
        
        
    }
}