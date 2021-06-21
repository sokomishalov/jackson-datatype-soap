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
@file:Suppress("MemberVisibilityCanBePrivate")

package ru.sokomishalov.jackson.dataformat.soap

import com.fasterxml.jackson.databind.PropertyName
import com.fasterxml.jackson.databind.PropertyName.NO_NAME
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.util.ClassUtil
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
            ann.annotated is Field || ClassUtil.isJDKClass(t) -> (ann.annotated as Field).declaringClass.namespace

            // other classes have their own namespaces
            else -> t.namespace
        }
    }

    override fun findNullSerializer(ann: Annotated?): Any? = when {
        isOutputAsAttribute(ann) == true -> null
        else -> SoapEnvelopeSerializers.XsiNilSerializer
    }

    override fun findWrapperName(ann: Annotated): PropertyName? = when {
        ann.type.isCollectionLikeType && !ann.hasAnnotation(XmlElementWrapper::class.java) -> NO_NAME
        else -> super.findWrapperName(ann)
    }

    protected val Class<*>.namespace: String get() = NamespaceCache.getNamespace(this)
}