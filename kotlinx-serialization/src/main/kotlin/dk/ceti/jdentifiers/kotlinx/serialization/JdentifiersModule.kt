@file:JvmName("JdentifiersModule")

package dk.ceti.jdentifiers.kotlinx.serialization

import dk.ceti.jdentifiers.id.GID
import dk.ceti.jdentifiers.id.ID
import dk.ceti.jdentifiers.id.LID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule

@Suppress("UNCHECKED_CAST")
val jdentifiersSerializersModule: SerializersModule = SerializersModule {
    contextual(ID::class, IDSerializer as KSerializer<ID<*>>)
    contextual(GID::class, GIDSerializer as KSerializer<GID<*>>)
    contextual(LID::class, LIDSerializer as KSerializer<LID<*>>)
}
