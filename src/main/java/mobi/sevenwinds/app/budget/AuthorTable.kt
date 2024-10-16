package mobi.sevenwinds.app.budget

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.CurrentDateTime

object AuthorTable : IntIdTable("author") {
    val fio = varchar("fio", 255)
    val time = datetime("created_at").defaultExpression(CurrentDateTime())
}

class AuthorEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AuthorEntity>(AuthorTable)

    var fio by AuthorTable.fio
    var time by AuthorTable.time

    fun toResponse(): AuthorRecord {
        return AuthorRecord(id.value, fio, time.millis)
    }
}