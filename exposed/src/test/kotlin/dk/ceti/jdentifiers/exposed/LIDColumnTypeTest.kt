package dk.ceti.jdentifiers.exposed

import dk.ceti.jdentifiers.id.IDAble
import dk.ceti.jdentifiers.id.LID
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private interface TestScope : IDAble

private object TestLidTable : Table("test_lids") {
    val id = lid<TestScope>("id")
    val ref = lid<TestScope>("ref").nullable()
    override val primaryKey = PrimaryKey(id)
}

class LIDColumnTypeTest {

    companion object {
        private val db = Database.connect("jdbc:h2:mem:test_lid;DB_CLOSE_DELAY=-1")
        init {
            transaction(db) { SchemaUtils.create(TestLidTable) }
        }
    }

    @BeforeTest
    fun clean() { transaction(db) { TestLidTable.deleteAll() } }

    @Test
    fun `round-trips through H2`() = transaction(db) {
        val original = LID.fromInt<TestScope>(42)
        TestLidTable.insert { it[id] = original }
        assertEquals(original, TestLidTable.selectAll().single()[TestLidTable.id])
    }

    @Test
    fun `round-trips zero`() = transaction(db) {
        val zero = LID.fromInt<TestScope>(0)
        TestLidTable.insert { it[id] = zero }
        assertEquals(zero, TestLidTable.selectAll().single()[TestLidTable.id])
    }

    @Test
    fun `round-trips Int MIN_VALUE (bit 31 set)`() = transaction(db) {
        val min = LID.fromInt<TestScope>(Int.MIN_VALUE)
        TestLidTable.insert { it[id] = min }
        assertEquals(min, TestLidTable.selectAll().single()[TestLidTable.id])
    }

    @Test
    fun `nullable column round-trips null`() = transaction(db) {
        TestLidTable.insert {
            it[id] = LID.fromInt(1)
            it[ref] = null
        }
        assertNull(TestLidTable.selectAll().single()[TestLidTable.ref])
    }
}
