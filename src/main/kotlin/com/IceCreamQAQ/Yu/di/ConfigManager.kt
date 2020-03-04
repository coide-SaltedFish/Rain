package com.IceCreamQAQ.Yu.di

import com.IceCreamQAQ.Yu.AppLogger
import com.IceCreamQAQ.Yu.error.ConfigFormatError
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

class ConfigManager {

    private var config: JSONObject = JSONObject()

    constructor(classloader: ClassLoader, logger: AppLogger, runMode: String?) {
        val classpath = File(classloader.getResource("").toURI())
        val f = File(classpath, "conf")

        if (f.isDirectory) {

            loadFolder(f)

            val mode = runMode ?: {
                var m: String? = null
                for (s in config.keys) {
                    m = get("yu.config.runMode", String::class.java, config[s] as JSONObject)
                    if (m != null) break
                }
                m ?: "dev"
            }()


            val ff = File(f, mode)

            if (ff.isDirectory) loadFolder(ff)

            val plugin = File(f, "plugin")
            val plugins = ArrayList<String>()
            if (ff.isDirectory) for (ff in plugin.listFiles()) {
                if (ff.isDirectory) continue
                val name = ff.name

                when {
                    name.endsWith(".properties") -> {
                        plugins.add(name.substring(0, name.length - 11))
                        loadConfigByProperties(ff)
                    }
                    name.endsWith(".json") -> {
                        plugins.add(name.substring(0, name.length - 5))
                        loadConfigByJSON(ff)
                    }
                    name.endsWith(".yml") -> {
                        plugins.add(name.substring(0, name.length - 4))
                        loadConfigByYaml(ff)
                    }
                    name.endsWith(".yaml") -> {
                        plugins.add(name.substring(0, name.length - 5))
                        loadConfigByYaml(ff)
                    }
                }
            }


            val c = JSONObject()
            for (key in config.keys) {
                appendObj(c, config[key] as JSONObject, true)
            }
            config = c

        } else {
            throw RuntimeException("ConfigManager Init Err! Conf folder not found! Conf folder should be at classpath!")
        }

    }

    private fun loadFolder(f: File): List<File> {
        val files = ArrayList<File>()
        for (s in f.listFiles()) {
            if (s.isDirectory) continue
            files.add(f)
            val name = s.name

            when {
                name.endsWith(".properties") -> loadConfigByProperties(s)
                name.endsWith(".json") -> loadConfigByJSON(s)
                name.endsWith(".yml") || name.endsWith(".yaml") -> loadConfigByYaml(s)
            }
        }
        return files
    }

    private fun loadConfigByProperties(file: File) {
        var jo = config[file.name]
        if (jo == null) {
            jo = JSONObject()
            config[file.name] = jo
        }

        val prop = Properties()
        prop.load(FileInputStream(file))

        val o = JSONObject()
        for (oo in prop.keys) {
            val s = checkPropName(oo.toString())
            val ss = s.split(".")
            var ooo = o
            val max = ss.size - 1
            for (i in 0..max) {
                val sss = ss[i]

                if (i == max) {
                    if (sss.startsWith("[")) {
                        val ssss = sss.replace("[", "")
                        val oooo = ooo[ssss]
                        if (oooo is JSONArray) oooo.add(prop[oo])
                        else {
                            val ooooo = JSONArray()
                            if (oooo != null) ooooo.add(oooo)
                            ooooo.add(prop[oo])
                            ooo[ssss] = ooooo
                        }
                    } else ooo[sss] = prop[oo]
                    continue
                }

                if (sss.startsWith("[")) {
                    val index = sss.split("[").size - 2
                    val ssss = sss.substring(index + 1)
                    val oooo = ooo[ssss]
                    if (oooo is JSONArray) {
                        if (oooo.size <= index) {
                            val ooooo = JSONObject()
                            oooo.add(ooooo)
                            ooo = ooooo
                        } else {
                            var ooooo = oooo[index]
                            if (ooooo !is JSONObject) {
                                ooooo = JSONObject()
                                oooo[index] = ooooo
                            }
                            ooo = ooooo
                        }

                    } else {
                        val ooooo = JSONArray()
                        if (oooo != null) ooooo.add(oooo)
                        val oooooo = JSONObject()
                        ooooo.add(oooooo)
                        ooo[ssss] = ooooo
                        ooo = oooooo
                    }
                    continue
                }
                var oooo = ooo[sss]

                if (oooo == null || oooo !is JSONObject) {
                    oooo = JSONObject()
                }

                ooo[sss] = oooo
                ooo = oooo
            }
        }


        appendObj(jo as JSONObject, o)
    }

    private fun loadConfigByJSON(file: File) {
        var jo = config[file.name]
        if (jo == null) {
            jo = JSONObject()
            config[file.name] = jo
        }

        val sb = StringBuilder()

        val fr = BufferedReader(FileReader(file))
        var s = fr.readLine()
        while (s != null) {
            sb.append(s)
            s = fr.readLine()
        }

        val o = JSON.parseObject(sb.toString())

        appendObj(jo as JSONObject, o)
    }

    private fun loadConfigByYaml(file: File) {
    }

    private fun appendObj(o: JSONObject, oo: JSONObject, append: Boolean = false) {
        for (s in oo.keys) {
            val v = o[s]
            val vv = oo[s]
            if (v == null) o[s] = vv
            else if (v is JSONObject && vv is JSONObject) appendObj(v, vv, append)
            else if (append && v is JSONArray && vv is JSONArray) v.addAll(vv)
            else o[s] = vv
        }
    }

    fun <T> get(key: String, type: Class<T>, jo: JSONObject = config): T? {
        var co: JSONObject? = jo
        val ns = key.split(".")
        val max = ns.size - 1
        for (i in 0..max) {
            val n = ns[i]

            if (n.endsWith("]")) {
                val nn = n.substring(0, n.length - 1).split("[")
                val oo = co?.get(nn[0]) ?: return null
                if (oo !is JSONArray) throw ConfigFormatError("Config Format Error: key $key is not a array!")

                val num = Integer.parseInt(nn[1])
                if (i == max) return oo.getObject(num, type)
                val ooo = oo.get(num)
                if (ooo !is JSONObject) throw ConfigFormatError("Config Format Error: key $key is not a object!")
                co = ooo
            } else {
                val oo = co?.get(n) ?: return null
                try {
                    if (i == max) {
                        if (oo is JSONObject) {
                            return oo.toJavaObject(type)
                        }
                        return oo as T
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw ConfigFormatError("Config Format Error: key $key's value not a ${type.name} object!")
                }
                if (oo !is JSONObject) throw ConfigFormatError("Config Format Error: key $key is not a object!")
                co = oo
            }

        }
        return co?.getObject(ns[max + 1], type)
    }

    fun <T> getArray(key: String, type: Class<T>): MutableList<T>? {
        val o = get(key, Any::class.java) ?: return null
        if (o is JSONArray) return o.toJavaList(type)
//        throw ConfigFormatError("Config Format Error: key $key is not a array!")
        val ja = JSONArray()
        ja.add(o)
        return ja.toJavaList(type)
    }

    private fun checkPropName(name: String): String {
        return when (name) {
            "yu.scanPackages" -> "yu.[scanPackages"
            else -> name
        }
    }

//    fun <T> toArray(key: String, type: Class<T>):Array<T>?{
////        val jr = getArray(key, type)?:return null
//        val o = get(key, Any::class.java) ?: return null
//        if (o is JSONArray) return o.toJavaList(type)
////        throw ConfigFormatError("Config Format Error: key $key is not a array!")
//        val ja = JSONArray()
//        ja.add(o)
//        return ja.toArray(Array<T>())
//    }
}

