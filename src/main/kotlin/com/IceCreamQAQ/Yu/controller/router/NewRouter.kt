package com.IceCreamQAQ.Yu.controller.router

import com.IceCreamQAQ.Yu.annotation.AutoBind
import com.IceCreamQAQ.Yu.controller.NewActionContext
import com.IceCreamQAQ.Yu.toLowerCaseFirstOne
import java.lang.reflect.Method
import java.util.regex.Pattern
import javax.inject.Named

open class MatchItem(
        val needSave: Boolean,
        matchString: String,
        val matchNames: Array<String>?,
        val router: NewRouter
) {
    val p: Pattern = Pattern.compile(matchString)

    fun invoke(path: String, context: NewActionContext): Boolean {
        return router.invoke(path, context)
    }
}

@AutoBind
interface NewRouter {
    operator fun invoke(path: String, context: NewActionContext): Boolean
}

interface NewMethodInvoker {
    @Throws(Exception::class)
    fun invoke(context: NewActionContext): Any?
}

interface CatchInvoker {
    @Throws(Exception::class)
    fun invoke(context: NewActionContext, error: Throwable): Any?
}

interface ActionInvoker : NewRouter

open class NewRouterImpl(val level: Int) : NewRouter {

    val needMath = ArrayList<MatchItem>()
    val noMatch = HashMap<String, NewRouterImpl>()

    override fun invoke(path: String, context: NewActionContext): Boolean {
        val cps = context.path.size
        val nextPath = when {
            level > cps -> return false
            level == cps -> ""
            else -> context.path[level]
        }
        val nor = noMatch[path]
        if (nor != null) return nor.invoke(nextPath, context)
        for (matchItem in needMath) {
            val m = matchItem.p.matcher(path)
            if (m.find()) {
                if (matchItem.needSave) {
                    for ((i, name) in matchItem.matchNames!!.withIndex()) {
                        context[name] = m.group(i + 1)
                    }
                }
                if (matchItem.invoke(nextPath, context)) return true
            }
        }
        return false
    }

}

open class NewActionInvoker(level: Int, method: Method, instance: Any) : NewRouterImpl(level) {

    open lateinit var globalBefores:MutableList<NewMethodInvoker>
    open lateinit var globalAfters:MutableList<NewMethodInvoker>
    open lateinit var globalCatchs:MutableList<CatchInvoker>


    open lateinit var befores: Array<NewMethodInvoker>
    open val invoker: NewMethodInvoker = NewReflectMethodInvoker(method, instance)
    open lateinit var afters: Array<NewMethodInvoker>
    open lateinit var catchs: Array<CatchInvoker>

    override fun invoke(path: String, context: NewActionContext): Boolean {
        if (super.invoke(path, context)) return true
        try {
            for (before in globalBefores) {
                val o = before.invoke(context)
                if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
            }
            for (before in befores) {
                val o = before.invoke(context)
                if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
            }
            val result = invoker.invoke(context)
            context.onSuccess(result)
            for (after in globalAfters) {
                val o = after.invoke(context)
                if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
            }
            for (after in afters) {
                val o = after.invoke(context)
                if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
            }
        } catch (e: Exception) {
            val er= context.onError(e) ?: return true
            context["exception"] = er
            try {
                for (catch in globalCatchs) {
                    val o=catch.invoke(context,er)
                    if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
                }
                for (catch in catchs) {
                    val o=catch.invoke(context,er)
                    if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
                }
            }catch (ee : Exception){
                throw context.onError(ee) ?: return true
            }
        }
        return true
    }

}

open class NewReflectMethodInvoker(val method: Method, val instance: Any) : NewMethodInvoker {

    var returnFlag: Boolean = false
    var mps: Array<MethodPara?>? = null

    init {
        returnFlag = (method.returnType?.name ?: "void") != "void"

        val paras = method.parameters!!
        val mps = arrayOfNulls<MethodPara>(paras.size)

        for (i in paras.indices) {
            val para = paras[i]!!
            val name = para.getAnnotation(Named::class.java)!!
            mps[i] = MethodPara(para.type, 0, name.value)
        }

        this.mps = mps
    }

    override fun invoke(context: NewActionContext): Any? {
        val mps = mps!!
        val paras = arrayOfNulls<Any>(mps.size)

        for (i in mps.indices) {
            val mp = mps[i] ?: continue
            paras[i] = when (mp.type) {
                0 -> context[mp.data as String]
                else -> null
            }
        }

        val re = if (mps.isEmpty()) method.invoke(instance)
        else {
            method.invoke(instance, *paras)
        }
        if (returnFlag) return re
        return null
    }

    data class MethodPara(
            val clazz: Class<*>,
            val type: Int,
            val data: Any
    )
}

open class ReflectCatchInvoker(val type: Class<out Throwable>, val methodInvoker: NewMethodInvoker):CatchInvoker {

//    private  = NewReflectMethodInvoker(method, instance)

    override fun invoke(context: NewActionContext, error: Throwable): Any? {
        if (!type.isAssignableFrom(error::class.java))return null
        return methodInvoker.invoke(context)
    }
}