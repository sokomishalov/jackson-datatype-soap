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

package ru.sokomishalov.jackson.datatype.soap

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.PropertyName
import com.fasterxml.jackson.databind.PropertyName.NO_NAME
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.util.ClassUtil
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import ru.sokomishalov.jackson.datatype.soap.ser.SoapEnvelopeSerializers
import ru.sokomishalov.jackson.datatype.soap.util.NamespaceCache
import java.lang.reflect.Field
import java.lang.reflect.Method

open class SoapJaxbAnnotationIntrospector : JaxbAnnotationIntrospector(TypeFactory.defaultInstance()) {

    override fun findNamespace(config: MapperConfig<*>?, ann: Annotated?): String? = ann?.rawType?.let { t ->
        val annotationNamespace = ann.annotated.getAnnotation(XmlElement::class.java)?.namespace

        when {
            // present namespace
            annotationNamespace != null && annotationNamespace != MARKER_FOR_DEFAULT -> annotationNamespace

            // attributes do not have namespaces
            ann.annotated.isAnnotationPresent(XmlAttribute::class.java) -> ""

            // fields with primitives or collections have parent namespaces
            ann.annotated is Field || ann.annotated is Method || ClassUtil.isJDKClass(t) -> ann.parentNamespace

            // other classes have their own namespaces
            else -> t.namespace
        }
    }

    /**
     * `nillable` elements always include `null` (as `xsi:nil`), other members fall back to class-level and mapper-wide
     * inclusion, which [SoapModule] defaults to `NON_NULL` like a JAXB marshaller. An explicit `@JsonInclude` on the member wins.
     */
    override fun findPropertyInclusion(a: Annotated): JsonInclude.Value = when {
        a is AnnotatedMember && a.hasAnnotation(JsonInclude::class.java) -> JsonInclude.Value.empty()
        else -> super.findPropertyInclusion(a)
    }

    override fun findNullSerializer(ann: Annotated?): Any? = when {
        ann is AnnotatedMember && ann.isNillable && ann.includesNulls -> SoapEnvelopeSerializers.XsiNilSerializer
        else -> null
    }

    override fun findWrapperName(ann: Annotated): PropertyName? = when {
        ann.type.isCollectionLikeType && !ann.hasAnnotation(XmlElementWrapper::class.java) -> NO_NAME
        else -> super.findWrapperName(ann)
    }

    protected val Annotated.isNillable: Boolean get() =
        getAnnotation(XmlElement::class.java)?.nillable == true || getAnnotation(XmlElementWrapper::class.java)?.nillable == true

    // a null serializer is used even when inclusion suppresses nulls, so an explicit @JsonInclude must be ALWAYS to get xsi:nil
    protected val Annotated.includesNulls: Boolean get() =
        getAnnotation(JsonInclude::class.java)?.value.let { it == null || it == JsonInclude.Include.ALWAYS }

    protected val Annotated.parentNamespace: String get() = when (val annotatedElement = annotated) {
        is Field -> annotatedElement.declaringClass.namespace
        is Method -> annotatedElement.declaringClass.namespace
        else -> ""
    }
    protected val Class<*>.namespace: String get() = NamespaceCache.getNamespace(this)
}