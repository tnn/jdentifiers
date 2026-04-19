package dk.ceti.jdentifiers.exposed

import dk.ceti.jdentifiers.id.GID
import dk.ceti.jdentifiers.id.ID
import dk.ceti.jdentifiers.id.IDAble
import dk.ceti.jdentifiers.id.LID
import org.jetbrains.exposed.v1.core.BasicUuidColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import java.nio.ByteBuffer
import java.util.UUID

class IDColumnType<T : IDAble> : ColumnType<ID<T>>() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.longType()

    override fun valueFromDB(value: Any): ID<T> = when (value) {
        is Long -> ID.fromLong(value)
        is Number -> ID.fromLong(value.toLong())
        is String -> ID.fromLong(value.toLong())
        else -> error("Unexpected value for ID: $value (${value::class.qualifiedName})")
    }

    override fun notNullValueToDB(value: ID<T>): Long = value.asLong()
}

class LIDColumnType<T : IDAble> : ColumnType<LID<T>>() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.integerType()

    override fun valueFromDB(value: Any): LID<T> = when (value) {
        is Int -> LID.fromInt(value)
        is Number -> LID.fromInt(value.toInt())
        is String -> LID.fromInt(value.toInt())
        else -> error("Unexpected value for LID: $value (${value::class.qualifiedName})")
    }

    override fun notNullValueToDB(value: LID<T>): Int = value.asInt()
}

class GIDColumnType<T : IDAble> : BasicUuidColumnType<GID<T>>() {
    override fun valueFromDB(value: Any): GID<T> = when (value) {
        is UUID -> GID.fromUuid(value)
        is ByteArray -> GID.fromUuid(ByteBuffer.wrap(value).let { UUID(it.long, it.long) })
        is String if value.isHexAndDashFormat() -> GID.fromString(value)
        is String -> GID.fromUuid(ByteBuffer.wrap(value.toByteArray()).let { UUID(it.long, it.long) })
        is ByteBuffer -> GID.fromUuid(value.let { UUID(it.long, it.long) })
        else -> error("Unexpected value for GID: $value (${value::class.qualifiedName})")
    }

    override fun notNullValueToDB(value: GID<T>): Any =
        originalDataTypeProvider.uuidToDB(value.asUUID())
}

fun <T : IDAble> Table.id(name: String): Column<ID<T>> =
    registerColumn(name, IDColumnType())

fun <T : IDAble> Table.lid(name: String): Column<LID<T>> =
    registerColumn(name, LIDColumnType())

fun <T : IDAble> Table.gid(name: String): Column<GID<T>> =
    registerColumn(name, GIDColumnType())
