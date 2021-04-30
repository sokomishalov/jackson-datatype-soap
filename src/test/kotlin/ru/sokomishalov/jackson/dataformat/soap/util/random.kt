package ru.sokomishalov.jackson.dataformat.soap.util

import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import kotlin.text.Charsets.UTF_8

/**
 * @author sokomishalov
 */

inline fun <reified T> random(): T = RANDOM.nextObject(T::class.java)

@PublishedApi
internal val RANDOM: EasyRandom = EasyRandom(
    EasyRandomParameters()
        .charset(UTF_8)
        .collectionSizeRange(1, 2)
        .stringLengthRange(5, 20)
        .scanClasspathForConcreteTypes(true)
        .overrideDefaultInitialization(false)
        .ignoreRandomizationErrors(true)
        .excludeField { it.name in NULL_FIELDS }
)

@JvmField
val NULL_FIELDS: List<String> = listOf("language", "locale", "deathDate", "deathDateStatus")