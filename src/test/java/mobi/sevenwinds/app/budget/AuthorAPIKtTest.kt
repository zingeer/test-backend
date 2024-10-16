package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthorAPIKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction {
            BudgetTable.deleteAll()
            AuthorTable.deleteAll()
        }
    }

    @Test
    fun testCreateAuthor() {
        addRecord("Петров Петр Петрович")
        addRecord("Андреев Андрей Андреевич")
        addRecord("Сидоров Сидор Сидорович")
    }

    @Test
    fun testGetAuthor() {
        val author = addRecord("Петров Петр Петрович")
        RestAssured.given()
            .get("/author/${author.id}")
            .toResponse<AuthorRecord>().let {
                Assertions.assertEquals(author.id, it.id)
                Assertions.assertEquals(author.fio, it.fio)
            }
    }

    @Test
    fun testGetListAuthors() {
        addRecord("Петров Петр Петрович")
        addRecord("Андреев Андрей Андреевич")
        addRecord("Сидоров Сидор Сидорович")

        RestAssured.given()
            .get("/author/list")
            .toResponse<List<AuthorRecord>>().let {
                Assertions.assertEquals(3, it.size)
            }
    }

    fun addRecord(fio: String): AuthorRecord {
        RestAssured.given()
            .jsonBody(AuthorRecord(fio = fio))
            .post("/author/add")
            .toResponse<AuthorRecord>().let {
                Assertions.assertEquals(fio, it.fio)
                return it
            }
    }



}