package dk.ceti.jdentifiers.exposed

import dk.ceti.jdentifiers.id.ID
import dk.ceti.jdentifiers.id.IDAble
import dk.ceti.jdentifiers.id.KSortableIDGenerator
import dk.ceti.jdentifiers.id.LID
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private interface Team : IDAble

private object TeamsTable : Table("teams") {
    val orgId = id<TestOrg>("organization_id")
    val teamId = lid<Team>("team_id")
    val name = varchar("name", 255)
    override val primaryKey = PrimaryKey(orgId, teamId)
}

class CompositeKeyTest {

    companion object {
        private val db = Database.connect("jdbc:h2:mem:test_composite;DB_CLOSE_DELAY=-1")
        init {
            transaction(db) { SchemaUtils.create(TeamsTable) }
        }
    }

    private val generator = KSortableIDGenerator()

    @BeforeTest
    fun clean() { transaction(db) { TeamsTable.deleteAll() } }

    @Test
    fun `LID scoped by organization`() = transaction(db) {
        val orgA: ID<TestOrg> = generator.identifier()
        val orgB: ID<TestOrg> = generator.identifier()
        val teamA1: LID<Team> = generator.localIdentifier()
        val teamA2: LID<Team> = generator.localIdentifier()
        val teamB1: LID<Team> = generator.localIdentifier()

        TeamsTable.insert {
            it[orgId] = orgA; it[teamId] = teamA1; it[name] = "Engineering"
        }
        TeamsTable.insert {
            it[orgId] = orgA; it[teamId] = teamA2; it[name] = "Sales"
        }
        TeamsTable.insert {
            it[orgId] = orgB; it[teamId] = teamB1; it[name] = "Support"
        }

        val orgATeams = TeamsTable.selectAll()
            .where { TeamsTable.orgId eq orgA }
            .map { it[TeamsTable.name] }
        assertEquals(listOf("Engineering", "Sales"), orgATeams)

        val team = TeamsTable.selectAll()
            .where { (TeamsTable.orgId eq orgA) and (TeamsTable.teamId eq teamA2) }
            .single()
        assertEquals("Sales", team[TeamsTable.name])
    }

    @Test
    fun `same LID in different organizations does not conflict`() = transaction(db) {
        val orgA: ID<TestOrg> = generator.identifier()
        val orgB: ID<TestOrg> = generator.identifier()
        val sameLid = LID.fromInt<Team>(1)

        TeamsTable.insert {
            it[orgId] = orgA; it[teamId] = sameLid; it[name] = "Alpha"
        }
        TeamsTable.insert {
            it[orgId] = orgB; it[teamId] = sameLid; it[name] = "Beta"
        }

        assertEquals(2, TeamsTable.selectAll().count())
    }

    @Test
    fun `duplicate composite key is rejected`() {
        assertFailsWith<Exception> {
            transaction(db) {
                val orgId: ID<TestOrg> = generator.identifier()
                val teamId = LID.fromInt<Team>(1)

                TeamsTable.insert {
                    it[TeamsTable.orgId] = orgId
                    it[TeamsTable.teamId] = teamId
                    it[name] = "First"
                }
                TeamsTable.insert {
                    it[TeamsTable.orgId] = orgId
                    it[TeamsTable.teamId] = teamId
                    it[name] = "Duplicate"
                }
            }
        }
    }
}
