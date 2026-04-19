package dk.ceti.jdentifiers.exposed

import dk.ceti.jdentifiers.id.GID
import dk.ceti.jdentifiers.id.IDAble
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private interface TestGlobal : IDAble

private object TestGidTable : Table("test_gids") {
    val id = gid<TestGlobal>("id")
    val ref = gid<TestGlobal>("ref").nullable()
    override val primaryKey = PrimaryKey(id)
}

class GIDColumnTypeTest {

    companion object {
        private val db = Database.connect("jdbc:h2:mem:test_gid;DB_CLOSE_DELAY=-1")
        init {
            transaction(db) { SchemaUtils.create(TestGidTable) }
        }
    }

    @BeforeTest
    fun clean() { transaction(db) { TestGidTable.deleteAll() } }

    @Test
    fun `round-trips through H2`() = transaction(db) {
        val original = GID.fromUuid<TestGlobal>(
            UUID.fromString("01234567-89ab-7def-8123-456789abcdef")
        )
        TestGidTable.insert { it[id] = original }
        assertEquals(original, TestGidTable.selectAll().single()[TestGidTable.id])
    }

    @Test
    fun `round-trips nil UUID`() = transaction(db) {
        val nil = GID.fromUuid<TestGlobal>(UUID(0L, 0L))
        TestGidTable.insert { it[id] = nil }
        assertEquals(nil, TestGidTable.selectAll().single()[TestGidTable.id])
    }

    @Test
    fun `nullable column round-trips null`() = transaction(db) {
        TestGidTable.insert {
            it[id] = GID.fromUuid(UUID.randomUUID())
            it[ref] = null
        }
        assertNull(TestGidTable.selectAll().single()[TestGidTable.ref])
    }
}
