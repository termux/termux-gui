package com.termux.gui.protocol.protobuf

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import com.google.protobuf.MessageLite
import com.termux.gui.App
import com.termux.gui.GUIActivity
import com.termux.gui.Util
import com.termux.gui.protocol.protobuf.v0.GUIProt0
import com.termux.gui.protocol.protobuf.v0.GUIProt0.Create
import com.termux.gui.protocol.protobuf.v0.V0Proto
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.V0Shared
import java.io.OutputStream
import java.lang.Exception
import java.util.Random
import java.util.TreeSet
import java.util.concurrent.LinkedBlockingQueue

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
                v.setOnClickListener {
                    val c = GUIProt0.ClickEvent.newBuilder()
                    if (v is CompoundButton) {
                        c.set = v.isChecked
                    }
                    eventQueue.offer(GUIProt0.Event.newBuilder().setClick(c).build())
                }
            } else {
                v.setOnClickListener(null)
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
                        retClass.getMethod("setSuccess", Boolean::class.java).invoke(ret, true)
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
                                retClass.getMethod("setSuccess", Boolean::class.java).invoke(ret, true)
                            }) retClass.getMethod("setSuccess", Boolean::class.java).invoke(ret, false)
                    }
                } catch (e: Exception) {
                    Log.d(this.javaClass.name, "Exception: ", e)
                    retClass.getMethod("setSuccess", Boolean::class.java).invoke(ret, false)
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