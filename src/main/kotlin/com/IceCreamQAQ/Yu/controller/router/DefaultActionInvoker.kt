package com.IceCreamQAQ.Yu.controller.router

import com.IceCreamQAQ.Yu.controller.ActionContext
import com.IceCreamQAQ.Yu.entity.DoNone
import com.IceCreamQAQ.Yu.entity.Result
import java.lang.Exception

abstract class DefaultActionInvoker(level: Int) : DefaultRouter(level) {

    lateinit var befores: Array<MethodInvoker>
    lateinit var invoker: MethodInvoker

    override fun invoke(path: String, context: ActionContext): Boolean {
        if (super.invoke(path, context)) return true
        try {
            for (before in befores) {
                val o = before.invoke(context)
                if (o != null) context.saveSomething(o, toLowerCaseFirstOne(o::class.java.simpleName))
            }
            val result = invoker.invoke(context) ?: return true
            if (result is Result) context.setResult(result)
            else context.setResult(makeResult(result))
        }catch (e:Result){
            context.setResult(e)
        }catch (e:DoNone){

        }catch (e:Exception){

        }
        return true
    }

    private fun toLowerCaseFirstOne(s: String): String {
        return if (Character.isLowerCase(s[0])) s
        else (StringBuilder()).append(Character.toLowerCase(s[0])).append(s.substring(1)).toString();
    }

    abstract fun makeResult(obj:Any):Result
}