package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction

object AuthorService {
    suspend fun addRecord(body: AuthorRecord): AuthorRecord = withContext(Dispatchers.IO) {
        transaction {
            val fio = body.fio!!

            AuthorEntity.find { AuthorTable.fio like "%$fio%" }.firstOrNull()?.let {
                return@transaction it.toResponse()
            }

            val entity = AuthorEntity.new {
                this.fio = fio
            }

            return@transaction entity.toResponse()
        }
    }


    suspend fun getList(): List<AuthorRecord> = withContext(Dispatchers.IO) {
        transaction {
            return@transaction AuthorEntity.all().map { it.toResponse() }
        }
    }

    suspend fun getRecord(param: AuthorParam): AuthorRecord = withContext(Dispatchers.IO) {
        transaction {
            return@transaction AuthorEntity.findById(param.id)?.toResponse() ?: throw IllegalArgumentException("Author not found")
        }
    }
}