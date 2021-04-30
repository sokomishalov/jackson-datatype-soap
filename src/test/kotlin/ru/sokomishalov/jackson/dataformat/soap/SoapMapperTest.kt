package ru.sokomishalov.jackson.dataformat.soap

import com.fasterxml.jackson.module.kotlin.readValue
import com.oracle.xmlns.apps.mdm.customer.GetPersonInput
import com.oracle.xmlns.apps.mdm.customer.GetPersonOutput
import org.junit.jupiter.api.Test
import ru.sokomishalov.commons.core.log.Loggable
import ru.sokomishalov.commons.core.string.isNotNullOrBlank
import ru.sokomishalov.jackson.dataformat.soap.util.random
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName
import kotlin.test.*

class SoapMapperTest {

    private val mapper = SoapMapper()

    @Test
    fun `Serialize pojo`() {
        val header = random<GetPersonInput>()
        val body = random<GetPersonOutput>().apply {
            listOfSwiPersonIO.contact.forEach { it.id = JAXBElement(QName("id"), String::class.java, "1500232321") }
        }

        val envelope = SoapEnvelope(body = body, header = header)

        val result = mapper.writeValueAsString(envelope)

        logInfo { result }

        assertTrue { result.isNotNullOrBlank() }

        listOf(
            ":Envelope",
            ":Contact>",
            ":PartyUId>",
            ":Body>",
            "ExternalSystemId=",
            "ExternalSystemName=",
            ":ListOfSwiPersonIO",
            ":FirstName>",
            ":FirstNameLat>"
        ).forEach {
            assertTrue("$it is not present") { it in result }
        }
    }

    @Test
    fun `Deserialize pojo`() {
        val content = readResource("/example/get_person_output.xml")
        val soapEnvelope = mapper.readValue<GetPersonInput, GetPersonOutput>(content)

        logInfo { mapper.writeValueAsString(soapEnvelope) }
        assertNotNull(soapEnvelope)

        assertNotNull(soapEnvelope.header)
        assertNotNull(soapEnvelope.header?.listOfSwiPersonIO?.contact?.firstOrNull()?.id)
        assertNotNull(soapEnvelope.header?.listOfSwiPersonIO?.contact?.firstOrNull()?.partyUId)

        assertNotNull(soapEnvelope.body)
        val pojo = soapEnvelope.body
        assertNotNull(pojo)
        assertNotNull(pojo.listOfSwiPersonIO)
        assertNotNull(pojo.listOfSwiPersonIO.contact)
        assertTrue { pojo.listOfSwiPersonIO.contact.isNotEmpty() }
        assertNotNull(pojo.listOfSwiPersonIO.contact[0].id?.value)
        pojo.listOfSwiPersonIO.contact.forEach {
            assertTrue { it.partyUId.isNotNullOrBlank() }
            assertTrue { it.firstName.isNotNullOrBlank() }
            assertTrue { it.firstNameLat.isNotNullOrBlank() }
            assertEquals(5, it.listOfContactAlternatePhone.contactAlternatePhone.size)
        }
    }

    @Test
    fun `Serialize pojo with ws addressing`() {
        val body = random<GetPersonOutput>()
        val header = random<SoapAddressingHeaders>()

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
            ":PartyUId>",
            ":Body>",
            "ExternalSystemId=",
            "ExternalSystemName=",
            ":ListOfSwiPersonIO",
            ":FirstName>",
            ":FirstNameLat>"
        ).forEach {
            assertTrue("$it is not present") { it in result }
        }
    }

    @Test
    fun `Deserialize pojo with ws addressing`() {
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
        assertNotNull(pojo.listOfSwiPersonIO.contact)
        assertTrue { pojo.listOfSwiPersonIO.contact.isNotEmpty() }
        pojo.listOfSwiPersonIO.contact.forEach {
            assertTrue { it.partyUId.isNotNullOrBlank() }
            assertTrue { it.firstName.isNotNullOrBlank() }
            assertTrue { it.firstNameLat.isNotNullOrBlank() }
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

    companion object : Loggable

}