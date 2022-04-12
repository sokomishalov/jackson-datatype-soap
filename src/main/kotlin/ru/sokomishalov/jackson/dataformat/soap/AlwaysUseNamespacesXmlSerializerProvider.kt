package ru.sokomishalov.jackson.dataformat.soap

import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider
import com.fasterxml.jackson.databind.ser.SerializerFactory
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.dataformat.xml.ser.XmlSerializerProvider
import com.fasterxml.jackson.dataformat.xml.util.XmlRootNameLookup
import javax.xml.XMLConstants.NULL_NS_URI
import javax.xml.namespace.QName

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