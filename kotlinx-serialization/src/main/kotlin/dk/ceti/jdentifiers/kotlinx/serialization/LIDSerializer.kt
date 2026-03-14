package dk.ceti.jdentifiers.kotlinx.serialization

import dk.ceti.jdentifiers.id.IDAble
import dk.ceti.jdentifiers.id.LID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object LIDSerializer : KSerializer<LID<IDAble>> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(LID::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LID<IDAble>) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LID<IDAble> {
        val text = decoder.decodeString()
        try {
            return LID.fromString<IDAble>(text)
        } catch (e: IllegalArgumentException) {
            throw SerializationException("Invalid LID: $text", e)
        }
    }
}
