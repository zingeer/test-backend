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

class BudgetAPIKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction {
            BudgetTable.deleteAll()
            AuthorTable.deleteAll()
        }
    }

    @Test
    fun testBudgetPagination() {
        addRecord(BudgetRecord(2020, 5, 10, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 20, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 40, BudgetType.Приход))
        addRecord(BudgetRecord(2030, 1, 1, BudgetType.Расход))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assertions.assertEquals(5, response.total)
                Assertions.assertEquals(3, response.items.size)
                Assertions.assertEquals(105, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetRecord(2020, 5, 100, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 400, BudgetType.Приход))

        // expected sort order - month ascending, amount descending

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assertions.assertEquals(30, response.items[0].amount)
                Assertions.assertEquals(5, response.items[1].amount)
                Assertions.assertEquals(400, response.items[2].amount)
                Assertions.assertEquals(100, response.items[3].amount)
                Assertions.assertEquals(50, response.items[4].amount)
            }
    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetRecord(2020, -5, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetRecord(2020, 15, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)
    }

    @Test
    fun testCreateBudgetWithAuthor() {
        addRecord(AuthorRecord(fio = "Петров Петр Петрович"))
        addRecord(BudgetRecord(2020, 5, 10, BudgetType.Приход, AuthorRecord(fio = "Петров Петр Петрович")))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход, AuthorRecord(fio = "Петров Петр")))
        addRecord(BudgetRecord(2020, 5, 20, BudgetType.Приход, AuthorRecord(fio = "Петров")))

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assertions.assertEquals(3, response.total)
                Assertions.assertEquals(35, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testBudgetStatsByAuthor() {
        val author1 = addRecord(AuthorRecord(fio = "Петров Петр Петрович"))
        val author2 = addRecord(AuthorRecord(fio = "Андреев Андрей Андреевич"))
        val author3 = addRecord(AuthorRecord(fio = "Сидоров Сидор Сидорович"))

        addRecord(BudgetRecord(2020, 5, 10, BudgetType.Приход, author1))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход, author2))
        addRecord(BudgetRecord(2020, 5, 20, BudgetType.Приход, author3))
        addRecord(BudgetRecord(2020, 5, 30, BudgetType.Приход, author1))
        addRecord(BudgetRecord(2020, 5, 40, BudgetType.Приход, author2))
        addRecord(BudgetRecord(2020, 5, 50, BudgetType.Приход, author3))

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0&author=Петров")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assertions.assertEquals(2, response.total)
                Assertions.assertEquals(40, response.totalByType[BudgetType.Приход.name])
            }

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0&author=Андреев")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assertions.assertEquals(2, response.total)
                Assertions.assertEquals(45, response.totalByType[BudgetType.Приход.name])
            }

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0&author=Сидоров")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assertions.assertEquals(2, response.total)
                Assertions.assertEquals(70, response.totalByType[BudgetType.Приход.name])
            }
    }



    private fun addRecord(record: BudgetRecord) {
        RestAssured.given()
            .jsonBody(record)
            .post("/budget/add")
            .toResponse<BudgetRecord>().let { response ->
                Assertions.assertEquals(record, response.copy(author = record.author))
                if (record.author != null) {
                    Assertions.assertNotNull(response.author)
                }
            }
    }

    private fun addRecord(record: AuthorRecord): AuthorRecord {
        RestAssured.given()
            .jsonBody(record)
            .post("/author/add")
            .toResponse<AuthorRecord>().let { response ->
                Assertions.assertEquals(record.fio, response.fio)
                return response
            }
    }
}