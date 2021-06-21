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

import com.oracle.xmlns.apps.mdm.customer.GetPersonOutput
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.junit.jupiter.api.Test
import ru.sokomishalov.commons.core.log.Loggable
import ru.sokomishalov.commons.core.string.isNotNullOrBlank
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName
import javax.xml.stream.XMLInputFactory
import kotlin.test.*


class SoapMapperTest {

    private val mapper = SoapMapper()

    @Test
    fun `Serialize pojo`() {
        val header = random<SoapAddressingHeaders>()
        val body = random<GetPersonOutput>().apply {
            listOfSwiPersonIO.contact.forEach { it.id = JAXBElement(QName("id"), String::class.java, "1500232321") }
        }

        val envelope = SoapEnvelope(body = body, header = header)

        val result = mapper.writeValueAsString(envelope)

        logInfo { result }

        assertTrue { result.isNotNullOrBlank() }

        listOf(
            ":Action>",
            ":To>",
            ":From>",
            ":ReplyTo>",
            ":FaultTo>",
            ":Address>",
            ":Envelope",
            ":Contact>",
            ":Id>",
            ":Body>",
            "ExternalSystemId=",
            "ExternalSystemName=",
            ":ListOfSwiPersonIO",
            ":FirstName>",
            ":LastName>"
        ).forEach {
            assertTrue("$it is not present") { it in result }
        }

        val xif = XMLInputFactory.newFactory()
        val xsr = xif.createXMLStreamReader(result.byteInputStream())
        while (runCatching { xsr.name.localPart != "getPerson_Output" }.getOrElse { true }) {
            xsr.next()
        }

        val unmarshaller = JAXBContext.newInstance(GetPersonOutput::class.java).createUnmarshaller()
        val unmarshalled = unmarshaller.unmarshal(xsr) as GetPersonOutput

        assertEquals(body.listOfSwiPersonIO.externalSystemId, unmarshalled.listOfSwiPersonIO.externalSystemId)
        assertEquals(body.listOfSwiPersonIO.language, unmarshalled.listOfSwiPersonIO.language)
        assertEquals(body.listOfSwiPersonIO.contact.size, unmarshalled.listOfSwiPersonIO.contact.size)
        assertEquals(body.listOfSwiPersonIO.contact[0].lastName, unmarshalled.listOfSwiPersonIO.contact[0].lastName)
    }

    @Test
    fun `Deserialize pojo`() {
        val content = readResource("/example/get_person_output_ws_addr.xml")
        val soapEnvelope = mapper.readValue<SoapAddressingHeaders?, GetPersonOutput?>(content)

        logInfo { mapper.writeValueAsString(soapEnvelope) }
        assertNotNull(soapEnvelope)

        assertNotNull(soapEnvelope.header)
        assertNotNull(soapEnvelope.header?.action)
        assertNotNull(soapEnvelope.header?.messageId)
        assertNotNull(soapEnvelope.header?.from)
        assertNotNull(soapEnvelope.header?.to)
        assertNotNull(soapEnvelope.header?.replyTo)
        assertNotNull(soapEnvelope.header?.faultTo)

        assertNotNull(soapEnvelope.body)
        val pojo = soapEnvelope.body
        assertNotNull(pojo)
        assertNotNull(pojo.listOfSwiPersonIO)
        assertEquals(2, pojo.listOfSwiPersonIO?.contact?.size)
        pojo.listOfSwiPersonIO.contact.forEach {
            assertTrue { it.id.value.isNotNullOrBlank() }
            assertTrue { it.firstName.isNotNullOrBlank() }
            assertTrue { it.lastName.isNotNullOrBlank() }
        }
    }

    @Test
    fun `Deserialize soap 1_1 fault`() {
        val content = readResource("/example/soap_fault_1_1.xml")
        val soapFault = assertFailsWith<SoapFault> { mapper.readValueBody<GetPersonOutput>(content) }

        logInfo { mapper.writeValueAsString(soapFault) }
        assertNotNull(soapFault)
        assertNotNull(soapFault)
        assertFalse { soapFault.message.isNullOrBlank() }
        assertFalse { soapFault.code.isNullOrBlank() }
    }

    @Test
    fun `Deserialize soap 1_2 fault`() {
        val content = readResource("/example/soap_fault_1_2.xml")
        val soapFault = assertFailsWith<SoapFault> { mapper.readValueBody<GetPersonOutput>(content) }

        logInfo { mapper.writeValueAsString(soapFault) }
        assertNotNull(soapFault)
        assertNotNull(soapFault)
        assertFalse { soapFault.message.isNullOrBlank() }
        assertFalse { soapFault.code.isNullOrBlank() }
    }

    private fun readResource(path: String) = javaClass.getResource(path)?.readText().orEmpty()
    private inline fun <reified T> random(): T = RANDOM.nextObject(T::class.java)

    companion object : Loggable {
        private val RANDOM: EasyRandom = EasyRandom(
            EasyRandomParameters()
                .charset(Charsets.UTF_8)
                .collectionSizeRange(1, 2)
                .stringLengthRange(5, 20)
                .scanClasspathForConcreteTypes(true)
                .overrideDefaultInitialization(false)
                .ignoreRandomizationErrors(true)
                .excludeField { it.name in NULL_FIELDS }
        )

        @JvmField
        val NULL_FIELDS: List<String> = listOf("language", "locale")
    }

}