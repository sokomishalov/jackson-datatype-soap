/*
 * Copyright (c) 2021-present Mikhael Sokolov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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