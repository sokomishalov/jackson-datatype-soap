@file:Suppress("MemberVisibilityCanBePrivate")

package ru.sokomishalov.jackson.dataformat.soap

import com.fasterxml.jackson.databind.PropertyName
import com.fasterxml.jackson.databind.PropertyName.NO_NAME
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector
import ru.sokomishalov.jackson.dataformat.soap.ser.SoapEnvelopeSerializers
import ru.sokomishalov.jackson.dataformat.soap.util.NamespaceCache
import java.lang.reflect.Field
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper


open class SoapJaxbAnnotationIntrospector @JvmOverloads constructor(
    typeFactory: TypeFactory = TypeFactory.defaultInstance()
) : JaxbAnnotationIntrospector(typeFactory) {

    override fun findNamespace(ann: Annotated): String? = ann.rawType?.let { t ->
        val annotationNamespace = ann.annotated.getAnnotation(XmlElement::class.java)?.namespace

        when {
            // present namespace
            annotationNamespace != null && annotationNamespace != MARKER_FOR_DEFAULT -> annotationNamespace

            // attributes do not have namespaces
            ann.annotated.isAnnotationPresent(XmlAttribute::class.java) -> ""

            // fields with primitives or collections have parent namespaces
            ann.annotated is Field && t.isFromJdk -> (ann.annotated as Field).declaringClass.namespace

            // other classes have their own namespaces
            else -> t.namespace
        }
    }

    override fun findNullSerializer(ann: Annotated?): Any? = when {
        isOutputAsAttribute(ann) == true -> null
        else -> SoapEnvelopeSerializers.XsiNilSerializer
    }

    override fun findWrapperName(ann: Annotated): PropertyName? = when {
        ann.type.isCollectionLikeType && ann.hasAnnotation(XmlElementWrapper::class.java).not() -> NO_NAME
        else -> super.findWrapperName(ann)
    }

    protected val Class<*>.isFromJdk: Boolean get() = javaClass.name.startsWith("java")
    protected val Class<*>.namespace: String get() = NamespaceCache.getNamespace(this)
}