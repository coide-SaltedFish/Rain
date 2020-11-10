package com.IceCreamQAQ.Yu

import com.IceCreamQAQ.Yu.annotation.NotSearch
import com.IceCreamQAQ.Yu.util.YuParaValueException
import com.alibaba.fastjson.JSON
import java.lang.reflect.Modifier

fun Class<*>.isBean()= !(this.isInterface || Modifier.isAbstract(this.modifiers))

fun <T> String.toObject(clazz: Class<T>) = JSON.parseObject(this,clazz)
fun String.toJSONObject() = JSON.parseObject(this)
fun Any.toJSONString() = JSON.toJSONString(this)

fun paraError(text:String) = YuParaValueException(text)

fun String.toLowerCaseFirstOne(): String {
    return if (Character.isLowerCase(this[0])) this
    else (StringBuilder()).append(Character.toLowerCase(this[0])).append(this.substring(1)).toString();
}