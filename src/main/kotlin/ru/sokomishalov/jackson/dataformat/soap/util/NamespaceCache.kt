package ru.sokomishalov.jackson.dataformat.soap.util

import java.util.concurrent.ConcurrentHashMap
import javax.xml.bind.annotation.XmlSchema

/**
 * @author sokomishalov
 */
internal object NamespaceCache {
    private val typeCache: MutableMap<Class<*>, String> = ConcurrentHashMap()

    internal fun getNamespace(clazz: Class<*>): String = typeCache.getOrPut(clazz) {
        clazz.getPackage().getAnnotation(XmlSchema::class.java)?.namespace.orEmpty()
    }
}