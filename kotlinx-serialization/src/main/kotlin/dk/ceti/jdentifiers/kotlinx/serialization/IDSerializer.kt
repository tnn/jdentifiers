package dk.ceti.jdentifiers.kotlinx.serialization

import dk.ceti.jdentifiers.id.ID
import dk.ceti.jdentifiers.id.IDAble
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object IDSerializer : KSerializer<ID<IDAble>> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(ID::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ID<IDAble>) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ID<IDAble> {
        val text = decoder.decodeString()
        try {
            return ID.fromString(text)
        } catch (e: IllegalArgumentException) {
            throw SerializationException("Invalid ID: $text", e)
        }
    }
}
