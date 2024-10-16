package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = (body.author?.id?.let { AuthorEntity.findById(it) } ?: body.author?.fio?.let {
                    AuthorEntity.find { AuthorTable.fio.lowerCase() like "%${it.toLowerCase()}%" }.firstOrNull()
                })
            }
            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val query = BudgetTable
                .let { if (param.author != null) it.leftJoin(AuthorTable) else it }
                .select {
                    (BudgetTable.year eq param.year).let {
                        if (param.author != null) it.and(AuthorTable.fio.lowerCase() like "%${param.author.toLowerCase()}%") else it
                    }
                }
                .orderBy(BudgetTable.month, SortOrder.ASC)
                .orderBy(BudgetTable.amount, SortOrder.DESC)

            val total = query.count()

            val sumByType =
                BudgetEntity.wrapRows(query).groupBy { it.type.name }.mapValues { it.value.sumOf { v -> v.amount } }

            val data = BudgetEntity.wrapRows(query.limit(param.limit, param.offset)).map { it.toResponse() }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data
            )
        }
    }
}