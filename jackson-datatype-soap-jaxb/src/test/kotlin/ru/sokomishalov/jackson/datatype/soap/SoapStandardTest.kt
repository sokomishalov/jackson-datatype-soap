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
package ru.sokomishalov.jackson.datatype.soap

import com.oracle.xmlns.apps.mdm.customer.GetPersonOutput
import com.siebel.xml.swipersonio.Contact
import com.siebel.xml.swipersonio.ListOfSwiPersonIO
import de.addmore.grouplist.grouplist_001.AppMonDetailsStrict
import de.addmore.grouplist.grouplist_001.ControllObject
import de.addmore.grouplist.grouplist_001.GetGroupListRequest
import javax.xml.bind.JAXBElement
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonView
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.sokomishalov.jackson.datatype.soap.SoapConstants.SOAP_11_ENVELOPE_NAMESPACE
import ru.sokomishalov.jackson.datatype.soap.SoapConstants.SOAP_12_ENVELOPE_NAMESPACE
import ru.sokomishalov.jackson.datatype.soap.SoapConstants.SOAP_ADDRESSING_NAMESPACE
import ru.sokomishalov.jackson.datatype.soap.SoapConstants.XML_SCHEMA_NAMESPACE
import java.math.BigInteger
import javax.xml.namespace.QName
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants.START_ELEMENT
import kotlin.test.*

/**
 * Behaviour required by SOAP 1.1 (https://www.w3.org/TR/2000/NOTE-SOAP-20000508/),
 * SOAP 1.2 (https://www.w3.org/TR/soap12-part1/) and WS-Addressing 1.0 (https://www.w3.org/TR/ws-addr-core/).
 */
class SoapStandardTest {

    private val mapper = SoapMapper()
    private val soap12Mapper = SoapMapper(SoapModule(soapEnvelopeNamespace = SOAP_12_ENVELOPE_NAMESPACE))

    @Nested
    inner class Serialization {

        @Test
        fun `SOAP 1_2 namespace is the one from the spec`() {
            assertEquals("http://www.w3.org/2003/05/soap-envelope", SOAP_12_ENVELOPE_NAMESPACE)

            val elements = elements(soap12Mapper.writeValueAsString(SoapEnvelope.fromBody(request())))

            assertEquals(QName(SOAP_12_ENVELOPE_NAMESPACE, "Envelope"), elements[0])
            assertEquals(QName(SOAP_12_ENVELOPE_NAMESPACE, "Body"), elements[1])
        }

        @Test
        fun `SOAP 1_1 is the default`() {
            val elements = elements(mapper.writeValueAsString(SoapEnvelope.fromBody(request())))

            assertEquals(QName(SOAP_11_ENVELOPE_NAMESPACE, "Envelope"), elements[0])
        }

        @Test
        fun `absent header is omitted, body is always written`() {
            val elements = elements(mapper.writeValueAsString(SoapEnvelope<Nothing, Any>()))

            assertEquals(listOf(QName(SOAP_11_ENVELOPE_NAMESPACE, "Envelope"), QName(SOAP_11_ENVELOPE_NAMESPACE, "Body")), elements)
        }

        @Test
        fun `WS-Addressing values are escaped`() {
            val headers = SoapAddressingHeaders(
                action = "urn:a?x=1&y=<2>",
                messageId = "uuid:\"quoted\"",
                to = "http://example.com/?a&b",
                replyTo = SoapAddressingHeaders.Endpoint(address = "http://example.com/?c&d")
            )

            val xml = mapper.writeValueAsString(SoapEnvelope(header = headers, body = request()))
            assertTrue { elements(xml).isNotEmpty() } // well-formed

            assertEquals(headers, mapper.readValue<SoapAddressingHeaders, GetGroupListRequest>(xml).header)
        }

        @Test
        fun `WS-Addressing headers are in the addressing namespace`() {
            val headers = SoapAddressingHeaders(
                action = "urn:a",
                messageId = "urn:m",
                from = "urn:f",
                to = "urn:t",
                replyTo = SoapAddressingHeaders.Endpoint("urn:r", "urn:s"),
                faultTo = SoapAddressingHeaders.Endpoint("urn:ft")
            )

            val elements = elements(mapper.writeValueAsString(SoapEnvelope(header = headers, body = request())))

            listOf("Action", "MessageID", "From", "To", "ReplyTo", "FaultTo", "Address", "ServiceName").forEach {
                assertContains(elements, QName(SOAP_ADDRESSING_NAMESPACE, it))
            }
        }

        @Test
        fun `multiple headers are written with their root element names`() {
            val header = SoapMultipleHeaders(ControllObject().apply { timeout = 1 }, AppMonDetailsStrict().apply { bpId = "id"; bpName = "name" })

            val elements = elements(mapper.writeValueAsString(SoapEnvelope(header = header, body = request())))

            assertContains(elements, QName(GROUP_LIST_NS, "controllObject"))
            assertContains(elements, QName(GROUP_LIST_NS, "appMonDetailsStrict"))
            assertFalse { elements.any { it.localPart == "ControllObject" || it.localPart == "AppMonDetailsStrict" } }
        }

        @Test
        fun `single header is written with its root element name`() {
            val elements = elements(mapper.writeValueAsString(SoapEnvelope(header = ControllObject().apply { timeout = 1 }, body = request())))

            assertEquals(QName(SOAP_11_ENVELOPE_NAMESPACE, "Header"), elements[1])
            assertEquals(QName(GROUP_LIST_NS, "controllObject"), elements[2])
        }

        @Test
        fun `root element namespace and default name follow XmlRootElement`() {
            assertEquals(QName("urn:explicit", "explicit"), elements(mapper.writeValueAsString(SoapEnvelope.fromBody(ExplicitRoot())))[2])
            assertEquals(QName("", "defaultNameRoot"), elements(mapper.writeValueAsString(SoapEnvelope.fromBody(DefaultNameRoot())))[2])
            assertEquals(QName("", "urlRequest"), elements(mapper.writeValueAsString(SoapEnvelope.fromBody(URLRequest())))[2])
            assertEquals(QName("", "url"), elements(mapper.writeValueAsString(SoapEnvelope.fromBody(URL())))[2])
        }

        @Test
        fun `JAXBElement body is written with its name`() {
            val body = JAXBElement(QName("urn:x", "wrapped"), String::class.java, "value")

            val xml = mapper.writeValueAsString(SoapEnvelope.fromBody(body))

            assertEquals(QName("urn:x", "wrapped"), elements(xml)[2])
            assertContains(xml, ">value<")
        }

        @Test
        fun `nil JAXBElement body is written as xsi nil`() {
            val body = JAXBElement(QName("urn:x", "wrapped"), String::class.java, null)

            val xml = mapper.writeValueAsString(SoapEnvelope.fromBody(body))

            assertEquals(listOf(QName("urn:x", "wrapped")), nilElements(xml))
        }

        @Test
        fun `null is xsi nil only for nillable elements`() {
            val body = GetPersonOutput().apply {
                listOfSwiPersonIO = ListOfSwiPersonIO().apply {
                    contact.add(Contact().apply { lastName = "Ivanov" })
                }
            }

            val xml = mapper.writeValueAsString(SoapEnvelope.fromBody(body))
            val elements = elements(xml)

            // nillable = true
            assertEquals(listOf("BirthDate", "MF", "Terminated"), nilElements(xml).map { it.localPart })
            // nillable = false
            assertFalse { elements.any { it.localPart == "FirstName" } }
            assertFalse { elements.any { it.localPart == "rowCount" } }
            assertContains(elements.map { it.localPart }, "LastName")
        }

        @Test
        fun `mapper-wide inclusion is respected`() {
            val default = elements(mapper.writeValueAsString(SoapEnvelope.fromBody(EmptyValues()))).map { it.localPart }
            val nonEmpty = elements(SoapMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY).writeValueAsString(SoapEnvelope.fromBody(EmptyValues()))).map { it.localPart }

            assertContains(default, "empty")
            assertFalse { "nothing" in default }
            assertFalse { "empty" in nonEmpty }
        }

        @Test
        fun `class-level JsonInclude is respected`() {
            val elements = elements(mapper.writeValueAsString(SoapEnvelope.fromBody(NonEmptyValues()))).map { it.localPart }

            assertFalse { "empty" in elements }
            assertContains(elements, "value")
        }

        @Test
        fun `nil JAXBElement property is written as xsi nil`() {
            val body = GetPersonOutput().apply {
                listOfSwiPersonIO = ListOfSwiPersonIO().apply {
                    contact.add(Contact().apply {
                        id = JAXBElement(QName(SIEBEL_NS, "Id"), String::class.java, null)
                        lastName = "Ivanov"
                    })
                }
            }

            assertContains(nilElements(mapper.writeValueAsString(SoapEnvelope.fromBody(body))).map { it.localPart }, "Id")
        }

        @Test
        fun `empty multiple headers are omitted`() {
            val elements = elements(mapper.writeValueAsString(SoapEnvelope(header = SoapMultipleHeaders<Any>(emptyList()), body = request())))

            assertFalse { QName(SOAP_11_ENVELOPE_NAMESPACE, "Header") in elements }
        }

        @Test
        fun `explicit JsonInclude wins over nillable-based inclusion`() {
            val elements = elements(mapper.writeValueAsString(SoapEnvelope.fromBody(ExplicitInclusion()))).map { it.localPart }

            assertContains(elements, "always")
            assertFalse { "omitted" in elements }
            assertFalse { "nillableNonNull" in elements }
        }

        @Test
        fun `ObjectWriter settings apply to header and body`() {
            val envelope = SoapEnvelope(header = ViewValues(), body = ViewValues())

            val all = elements(mapper.writeValueAsString(envelope)).map { it.localPart }
            val public = elements(mapper.writerWithView(Public::class.java).writeValueAsString(envelope)).map { it.localPart }

            assertEquals(2, all.count { it == "internal" })
            assertEquals(2, public.count { it == "public" })
            assertFalse { "internal" in public }
        }

        @Test
        fun `copied mapper keeps SOAP behaviour`() {
            val envelope = SoapEnvelope(header = SoapAddressingHeaders(action = "urn:a"), body = request())

            assertEquals(mapper.writeValueAsString(envelope), mapper.copy().writeValueAsString(envelope))
            assertEquals(soap12Mapper.writeValueAsString(envelope), soap12Mapper.copy().writeValueAsString(envelope))
        }
    }

    @Nested
    inner class Faults {

        @Test
        fun `SOAP 1_1 fault`() {
            val fault = assertFailsWith<SoapFault> {
                mapper.readValueBody<GetPersonOutput>(
                    """
                    <s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE">
                        <s:Body>
                            <s:Fault>
                                <faultcode>s:Server</faultcode>
                                <faultstring>Server error</faultstring>
                                <faultactor>http://example.com/actor</faultactor>
                                <detail>
                                    <m:error xmlns:m="urn:m">
                                        <m:code>42</m:code>
                                        <faultstring>not the fault string</faultstring>
                                        <faultcode>not the fault code</faultcode>
                                    </m:error>
                                </detail>
                            </s:Fault>
                        </s:Body>
                    </s:Envelope>
                    """
                )
            }

            assertEquals("Server error", fault.message)
            assertEquals("s:Server", fault.code)
            assertEquals(emptyList(), fault.subcodes)
            assertEquals("http://example.com/actor", fault.node)
            assertNull(fault.role)
            assertEquals("42", fault.detail?.at("/error/code")?.asText())
        }

        @Test
        fun `SOAP 1_2 fault`() {
            val fault = assertFailsWith<SoapFault> {
                soap12Mapper.readValueBody<GetPersonOutput>(
                    """
                    <e:Envelope xmlns:e="$SOAP_12_ENVELOPE_NAMESPACE">
                        <e:Body>
                            <e:Fault>
                                <e:Code>
                                    <e:Value>e:Sender</e:Value>
                                    <e:Subcode>
                                        <e:Value>m:MessageTimeout</e:Value>
                                        <e:Subcode>
                                            <e:Value>m:Retry</e:Value>
                                        </e:Subcode>
                                    </e:Subcode>
                                </e:Code>
                                <e:Reason>
                                    <e:Text xml:lang="en">Sender timeout</e:Text>
                                    <e:Text xml:lang="de">Zeitüberschreitung</e:Text>
                                </e:Reason>
                                <e:Node>http://example.com/node</e:Node>
                                <e:Role>http://example.com/role</e:Role>
                                <e:Detail>
                                    <m:MaxTime xmlns:m="urn:m">P5M</m:MaxTime>
                                    <e:Value>not the code</e:Value>
                                </e:Detail>
                            </e:Fault>
                        </e:Body>
                    </e:Envelope>
                    """
                )
            }

            assertEquals("Sender timeout", fault.message)
            assertEquals("e:Sender", fault.code)
            assertEquals(listOf("m:MessageTimeout", "m:Retry"), fault.subcodes)
            assertEquals("http://example.com/node", fault.node)
            assertEquals("http://example.com/role", fault.role)
            assertEquals("P5M", fault.detail?.get("MaxTime")?.asText())
        }

        @Test
        fun `fault text with entities and CDATA is read whole`() {
            val fault = assertFailsWith<SoapFault> {
                mapper.readValueBody<GetPersonOutput>(
                    """<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Body><s:Fault><faultcode>s:Client</faultcode><faultstring>a &amp; b <![CDATA[<c>]]> d</faultstring></s:Fault></s:Body></s:Envelope>"""
                )
            }

            assertEquals("a & b <c> d", fault.message)
        }

        @Test
        fun `fault without detail`() {
            val fault = assertFailsWith<SoapFault> {
                mapper.readValueBody<GetPersonOutput>(
                    """<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Body><s:Fault><faultcode>s:Client</faultcode><faultstring>x</faultstring></s:Fault></s:Body></s:Envelope>"""
                )
            }

            assertNull(fault.detail)
            assertNull(fault.node)
        }

        @Test
        fun `empty fault detail`() {
            val fault = assertFailsWith<SoapFault> {
                mapper.readValueBody<GetPersonOutput>(
                    """<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Body><s:Fault><detail/><faultcode>s:Client</faultcode><faultstring>x</faultstring></s:Fault></s:Body></s:Envelope>"""
                )
            }

            assertEquals("s:Client", fault.code)
            assertEquals("x", fault.message)
        }

        @Test
        fun `fault children after detail are still read`() {
            val fault = assertFailsWith<SoapFault> {
                mapper.readValueBody<GetPersonOutput>(
                    """<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Body><s:Fault><detail><m:e xmlns:m="urn:m">1</m:e></detail><faultcode>s:Server</faultcode><faultstring>late</faultstring></s:Fault></s:Body></s:Envelope>"""
                )
            }

            assertEquals("s:Server", fault.code)
            assertEquals("late", fault.message)
            assertEquals("1", fault.detail?.get("e")?.asText())
        }

        @Test
        fun `fault is thrown when header is present`() {
            assertFailsWith<SoapFault> {
                mapper.readValue<SoapAddressingHeaders, GetPersonOutput>(
                    """<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Header><wsa:Action xmlns:wsa="$SOAP_ADDRESSING_NAMESPACE">urn:fault</wsa:Action></s:Header><s:Body><s:Fault><faultcode>s:Client</faultcode><faultstring>x</faultstring></s:Fault></s:Body></s:Envelope>"""
                )
            }
        }

        @Test
        fun `payload element named Fault outside the envelope namespace is not a fault`() {
            val body = mapper.readValueBody<BusinessFault>(
                """<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Body><m:Fault xmlns:m="urn:m"><m:reason>business</m:reason></m:Fault></s:Body></s:Envelope>"""
            )

            assertEquals("business", body?.reason)
        }
    }

    @Nested
    inner class Deserialization {

        @Test
        fun `whitespace-only header does not swallow the body`() {
            val envelope = mapper.readValue<ControllObject, GetGroupListRequest>(
                """<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Header>   </s:Header><s:Body>${requestXml(5)}</s:Body></s:Envelope>"""
            )

            assertNull(envelope.header)
            assertEquals(BigInteger.valueOf(5), envelope.body?.rowCount)
        }

        @Test
        fun `header elements named Body or Fault are not taken for the envelope ones`() {
            val content = """
                <s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE">
                    <s:Header>
                        <x:Body xmlns:x="urn:x"><x:a>1</x:a></x:Body>
                        <s:Fault/>
                    </s:Header>
                    <s:Body>${requestXml(5)}</s:Body>
                </s:Envelope>
            """

            assertEquals(BigInteger.valueOf(5), mapper.readValueBody<GetGroupListRequest>(content)?.rowCount)
        }

        @Test
        fun `custom header is read next to other headers`() {
            val envelope = mapper.readValue<ControllObject, GetGroupListRequest>(
                """
                <s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE">
                    <s:Header>
                        <g:controllObject xmlns:g="$GROUP_LIST_NS"><g:timeout>7</g:timeout></g:controllObject>
                        <o:Security xmlns:o="urn:sec"><o:Body>t</o:Body></o:Security>
                    </s:Header>
                    <s:Body>${requestXml(5)}</s:Body>
                </s:Envelope>
                """
            )

            assertEquals(7, envelope.header?.timeout)
            assertEquals(BigInteger.valueOf(5), envelope.body?.rowCount)
        }

        @Test
        fun `WS-Addressing headers next to foreign headers`() {
            val envelope = mapper.readValue<SoapAddressingHeaders, GetGroupListRequest>(
                """
                <s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE">
                    <s:Header>
                        <o:Security xmlns:o="urn:sec" s:mustUnderstand="1"><o:Token>t</o:Token></o:Security>
                        <wsa:Action xmlns:wsa="$SOAP_ADDRESSING_NAMESPACE">urn:a</wsa:Action>
                        <wsa:MessageID xmlns:wsa="$SOAP_ADDRESSING_NAMESPACE">urn:m</wsa:MessageID>
                    </s:Header>
                    <s:Body>${requestXml(5)}</s:Body>
                </s:Envelope>
                """
            )

            assertEquals("urn:a", envelope.header?.action)
            assertEquals("urn:m", envelope.header?.messageId)
            assertEquals(BigInteger.valueOf(5), envelope.body?.rowCount)
        }

        @Test
        fun `WS-Addressing headers next to foreign headers with a strict reader`() {
            val type = mapper.typeFactory.constructParametricType(SoapEnvelope::class.java, SoapAddressingHeaders::class.java, GetGroupListRequest::class.java)
            val content = """
                <s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE">
                    <s:Header>
                        <o:Security xmlns:o="urn:sec"><o:Token>t</o:Token></o:Security>
                        <wsa:Action xmlns:wsa="$SOAP_ADDRESSING_NAMESPACE">urn:a</wsa:Action>
                        <wsa:RelatesTo xmlns:wsa="$SOAP_ADDRESSING_NAMESPACE">urn:r</wsa:RelatesTo>
                        <wsa:ReplyTo xmlns:wsa="$SOAP_ADDRESSING_NAMESPACE"><wsa:Address>urn:reply</wsa:Address><wsa:ReferenceParameters/></wsa:ReplyTo>
                    </s:Header>
                    <s:Body>${requestXml(5)}</s:Body>
                </s:Envelope>
            """

            val envelope = mapper.readerFor(type).with(FAIL_ON_UNKNOWN_PROPERTIES).readValue<SoapEnvelope<SoapAddressingHeaders, GetGroupListRequest>>(content)

            assertEquals("urn:a", envelope.header?.action)
            assertEquals("urn:reply", envelope.header?.replyTo?.address)
            assertEquals(BigInteger.valueOf(5), envelope.body?.rowCount)
        }

        @Test
        fun `missing header`() {
            val envelope = mapper.readValue<SoapAddressingHeaders, GetGroupListRequest>(
                """<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Body>${requestXml(5)}</s:Body></s:Envelope>"""
            )

            assertNull(envelope.header)
            assertEquals(BigInteger.valueOf(5), envelope.body?.rowCount)
        }

        @Test
        fun `empty body`() {
            assertNull(mapper.readValueBody<GetGroupListRequest>("""<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Body/></s:Envelope>"""))
            assertNull(mapper.readValueBody<GetGroupListRequest>("""<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Body>  </s:Body></s:Envelope>"""))
        }

        @Test
        fun `comments and XML declaration around payload`() {
            val content = """<?xml version="1.0" encoding="UTF-8"?><!-- c --><s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><!-- c --><s:Body><!-- c -->${requestXml(5)}<!-- c --></s:Body></s:Envelope>"""

            assertEquals(BigInteger.valueOf(5), mapper.readValueBody<GetGroupListRequest>(content)?.rowCount)
        }

        @Test
        fun `SOAP 1_2 envelope`() {
            val envelope = mapper.readValue<SoapAddressingHeaders, GetGroupListRequest>(
                """<e:Envelope xmlns:e="$SOAP_12_ENVELOPE_NAMESPACE"><e:Header><wsa:To xmlns:wsa="$SOAP_ADDRESSING_NAMESPACE">urn:t</wsa:To></e:Header><e:Body>${requestXml(5)}</e:Body></e:Envelope>"""
            )

            assertEquals("urn:t", envelope.header?.to)
            assertEquals(BigInteger.valueOf(5), envelope.body?.rowCount)
        }

        @Test
        fun `ObjectReader is supported and its settings apply to header and body`() {
            val type = mapper.typeFactory.constructParametricType(SoapEnvelope::class.java, ControllObject::class.java, GetGroupListRequest::class.java)
            fun content(header: String = "", body: String = "") =
                """<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Header><g:controllObject xmlns:g="$GROUP_LIST_NS"><g:timeout>7</g:timeout>$header</g:controllObject></s:Header><s:Body><g:GetGroupListRequest xmlns:g="$GROUP_LIST_NS"><g:rowCount>5</g:rowCount>$body</g:GetGroupListRequest></s:Body></s:Envelope>"""

            val envelope = mapper.readerFor(type).readValue<SoapEnvelope<ControllObject, GetGroupListRequest>>(content())
            assertEquals(7, envelope.header?.timeout)
            assertEquals(BigInteger.valueOf(5), envelope.body?.rowCount)

            val strict = mapper.readerFor(type).with(FAIL_ON_UNKNOWN_PROPERTIES)
            strict.readValue<Any>(content())
            assertFailsWith<UnrecognizedPropertyException> { strict.readValue<Any>(content(body = "<g:unknown>1</g:unknown>")) }
            assertFailsWith<UnrecognizedPropertyException> { strict.readValue<Any>(content(header = "<g:unknown>1</g:unknown>")) }
        }

        @Test
        fun `ObjectReader parser features apply to the body`() {
            val content = """<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Body><m:Fault xmlns:m="urn:m"><m:reason/></m:Fault></s:Body></s:Envelope>"""
            val type = mapper.typeFactory.constructParametricType(SoapEnvelope::class.java, Nothing::class.java, BusinessFault::class.java)

            assertEquals("", mapper.readerFor(type).readValue<SoapEnvelope<Nothing, BusinessFault>>(content).body?.reason)
            assertNull(mapper.readerFor(type).with(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL).readValue<SoapEnvelope<Nothing, BusinessFault>>(content).body?.reason)
        }

        @Test
        fun `empty custom header element`() {
            val envelope = mapper.readValue<ControllObject, GetGroupListRequest>(
                """<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Header><g:controllObject xmlns:g="$GROUP_LIST_NS"/></s:Header><s:Body>${requestXml(5)}</s:Body></s:Envelope>"""
            )

            assertEquals(BigInteger.valueOf(5), envelope.body?.rowCount)
        }

        @Test
        fun `Java-style API`() {
            val content = """<s:Envelope xmlns:s="$SOAP_11_ENVELOPE_NAMESPACE"><s:Body>${requestXml(5)}</s:Body></s:Envelope>"""

            assertEquals(BigInteger.valueOf(5), mapper.readValue(content, GetGroupListRequest::class.java, ControllObject::class.java).body?.rowCount)
        }
    }

    @Nested
    inner class RoundTrip {

        @Test
        fun `WS-Addressing headers`() {
            val envelope = SoapEnvelope(
                header = SoapAddressingHeaders(
                    action = "urn:a",
                    messageId = "urn:m",
                    from = "urn:f",
                    to = "urn:t",
                    replyTo = SoapAddressingHeaders.Endpoint("urn:r"),
                    faultTo = SoapAddressingHeaders.Endpoint("urn:ft")
                ),
                body = request()
            )

            val read = mapper.readValue<SoapAddressingHeaders, GetGroupListRequest>(mapper.writeValueAsString(envelope))

            assertEquals(envelope.header, read.header)
            assertEquals(envelope.body?.rowCount, read.body?.rowCount)
        }

        @Test
        fun `custom header`() {
            val envelope = SoapEnvelope(header = ControllObject().apply { timeout = 7 }, body = request())

            val read = mapper.readValue<ControllObject, GetGroupListRequest>(mapper.writeValueAsString(envelope))

            assertEquals(7, read.header?.timeout)
            assertEquals(envelope.body?.rowCount, read.body?.rowCount)
        }

        @Test
        fun `SOAP 1_2`() {
            val envelope = SoapEnvelope(header = SoapAddressingHeaders(action = "urn:a"), body = request())

            val read = soap12Mapper.readValue<SoapAddressingHeaders, GetGroupListRequest>(soap12Mapper.writeValueAsString(envelope))

            assertEquals(envelope.header, read.header)
            assertEquals(envelope.body?.rowCount, read.body?.rowCount)
        }
    }

    private fun request(rowCount: Long = 5) = GetGroupListRequest().apply { this.rowCount = BigInteger.valueOf(rowCount) }

    private fun requestXml(rowCount: Long) = """<g:GetGroupListRequest xmlns:g="$GROUP_LIST_NS"><g:rowCount>$rowCount</g:rowCount></g:GetGroupListRequest>"""

    private fun elements(xml: String): List<QName> {
        val reader = XMLInputFactory.newFactory().createXMLStreamReader(xml.reader())
        return buildList {
            while (reader.hasNext()) {
                if (reader.next() == START_ELEMENT) add(reader.name)
            }
        }
    }

    private fun nilElements(xml: String): List<QName> {
        val reader = XMLInputFactory.newFactory().createXMLStreamReader(xml.reader())
        return buildList {
            while (reader.hasNext()) {
                if (reader.next() == START_ELEMENT && reader.getAttributeValue(XML_SCHEMA_NAMESPACE, "nil") == "true") add(reader.name)
            }
        }
    }

    @XmlRootElement(name = "explicit", namespace = "urn:explicit")
    class ExplicitRoot {
        var value: String? = "v"
    }

    @XmlRootElement
    class DefaultNameRoot {
        var value: String? = "v"
    }

    @XmlRootElement
    class URLRequest {
        var value: String? = "v"
    }

    @XmlRootElement
    class URL {
        var value: String? = "v"
    }

    @XmlRootElement(name = "values", namespace = "urn:v")
    class EmptyValues {
        var empty: String? = ""
        var nothing: String? = null
    }

    @XmlRootElement(name = "values", namespace = "urn:v")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    class NonEmptyValues {
        var empty: String? = ""
        var value: String? = "v"
    }

    interface Public
    interface Internal

    @XmlRootElement(name = "view", namespace = "urn:view")
    class ViewValues {
        @JsonView(Public::class)
        var public: String? = "p"
        @JsonView(Internal::class)
        var internal: String? = "i"
    }

    @XmlRootElement(name = "included", namespace = "urn:i")
    class ExplicitInclusion {
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var always: String? = null
        var omitted: String? = null
        @XmlElement(nillable = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        var nillableNonNull: String? = null
    }

    @XmlRootElement(name = "Fault", namespace = "urn:m")
    class BusinessFault {
        var reason: String? = null
    }

    private companion object {
        const val GROUP_LIST_NS = "http://www.addmore.de/GroupList/GroupList-001.wsdl"
        const val SIEBEL_NS = "http://www.siebel.com/xml/SwiPersonIO"
    }
}
