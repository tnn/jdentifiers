package dk.ceti.jdentifiers.exposed

import dk.ceti.jdentifiers.id.ID
import dk.ceti.jdentifiers.id.IDAble
import dk.ceti.jdentifiers.id.KSortableIDGenerator
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

interface TestOrg : IDAble

object TestOrgTable : IdTable<Long>("organizations") {
    override val id = long("id").entityId()
    override val primaryKey = PrimaryKey(id)
    val name = varchar("name", 255)
}

class TestOrgEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<TestOrgEntity>(TestOrgTable)
    var name by TestOrgTable.name
}

class DaoApiTest {

    private val generator = KSortableIDGenerator()

    @Test
    fun `create entity with application-generated ID`() {
        val db = Database.connect("jdbc:h2:mem:test_dao;DB_CLOSE_DELAY=-1")
        val orgId: ID<TestOrg> = generator.identifier()

        transaction(db) {
            SchemaUtils.create(TestOrgTable)

            val org = TestOrgEntity.new(orgId.asLong()) {
                name = "Acme"
            }
            assertEquals(orgId.asLong(), org.id.value)
            assertEquals("Acme", org.name)
        }
    }

    @Test
    fun `find entity by application-generated ID`() {
        val db = Database.connect("jdbc:h2:mem:test_dao_find;DB_CLOSE_DELAY=-1")
        val orgId: ID<TestOrg> = generator.identifier()

        transaction(db) {
            SchemaUtils.create(TestOrgTable)

            TestOrgEntity.new(orgId.asLong()) {
                name = "Acme"
            }

            val found = TestOrgEntity.findById(orgId.asLong())
            assertNotNull(found)
            assertEquals("Acme", found.name)
        }
    }
}
