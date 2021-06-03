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
package ru.sokomishalov.jackson.dataformat.soap

import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider
import com.fasterxml.jackson.databind.ser.SerializerFactory
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.dataformat.xml.ser.XmlSerializerProvider
import com.fasterxml.jackson.dataformat.xml.util.XmlRootNameLookup
import javax.xml.XMLConstants.NULL_NS_URI
import javax.xml.namespace.QName

/**
 * @author sokomishalov
 */
internal class AlwaysUseNamespacesXmlSerializerProvider : XmlSerializerProvider {

    internal constructor() : super(XmlRootNameLookup())
    internal constructor(src: XmlSerializerProvider, config: SerializationConfig?, f: SerializerFactory?) : super(src, config, f)

    override fun createInstance(config: SerializationConfig?, jsf: SerializerFactory?): DefaultSerializerProvider {
        return AlwaysUseNamespacesXmlSerializerProvider(this, config, jsf)
    }

    override fun _initWithRootName(xgen: ToXmlGenerator, rootName: QName?) {
        super._initWithRootName(xgen, rootName)
        xgen.staxWriter.setDefaultNamespace(NULL_NS_URI)
    }
}