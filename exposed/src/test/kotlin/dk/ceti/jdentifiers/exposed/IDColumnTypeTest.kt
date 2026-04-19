package dk.ceti.jdentifiers.exposed

import dk.ceti.jdentifiers.id.ID
import dk.ceti.jdentifiers.id.IDAble
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

private interface TestEntity : IDAble

private object TestTable : Table("test_ids") {
    val id = id<TestEntity>("id")
    val ref = id<TestEntity>("ref").nullable()
    override val primaryKey = PrimaryKey(id)
}

class IDColumnTypeTest {

    companion object {
        private val db = Database.connect("jdbc:h2:mem:test_id;DB_CLOSE_DELAY=-1")
        init {
            transaction(db) { SchemaUtils.create(TestTable) }
        }
    }

    @BeforeTest
    fun clean() { transaction(db) { TestTable.deleteAll() } }

    @Test
    fun `round-trips through H2`() = transaction(db) {
        val original = ID.fromLong<TestEntity>(123456789L)
        TestTable.insert { it[id] = original }
        assertEquals(original, TestTable.selectAll().single()[TestTable.id])
    }

    @Test
    fun `round-trips zero`() = transaction(db) {
        val zero = ID.fromLong<TestEntity>(0L)
        TestTable.insert { it[id] = zero }
        assertEquals(zero, TestTable.selectAll().single()[TestTable.id])
    }

    @Test
    fun `round-trips Long MAX_VALUE`() = transaction(db) {
        val max = ID.fromLong<TestEntity>(Long.MAX_VALUE)
        TestTable.insert { it[id] = max }
        assertEquals(max, TestTable.selectAll().single()[TestTable.id])
    }

    @Test
    fun `round-trips Long MIN_VALUE (bit 63 set)`() = transaction(db) {
        val min = ID.fromLong<TestEntity>(Long.MIN_VALUE)
        TestTable.insert { it[id] = min }
        assertEquals(min, TestTable.selectAll().single()[TestTable.id])
    }

    @Test
    fun `nullable column stores and retrieves null`() = transaction(db) {
        TestTable.insert {
            it[id] = ID.fromLong(1L)
            it[ref] = null
        }
        assertNull(TestTable.selectAll().single()[TestTable.ref])
    }

    @Test
    fun `nullable column stores and retrieves value`() = transaction(db) {
        val refVal = ID.fromLong<TestEntity>(42L)
        TestTable.insert {
            it[id] = ID.fromLong(1L)
            it[ref] = refVal
        }
        assertEquals(refVal, TestTable.selectAll().single()[TestTable.ref])
    }
}
