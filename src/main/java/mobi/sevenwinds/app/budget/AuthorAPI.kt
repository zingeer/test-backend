package mobi.sevenwinds.app.budget

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.type.number.integer.min.Min
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import org.joda.time.DateTime

fun NormalOpenAPIRoute.author() {
    route("/author") {
        route("/add").post<Unit, AuthorRecord, AuthorRecord>(info("Добавить автора")) { param, body ->
            if (body.fio == null) {
                throw IllegalArgumentException("Fio is required")
            }
            if (body.fio.split(" ").size != 3) {
                throw IllegalArgumentException("Fio must contain 3 parts")
            }
            respond(AuthorService.addRecord(body))
        }

        route("/list").get<Unit, List<AuthorRecord>>(info("Получить список авторов")) {
            respond(AuthorService.getList())
        }
        route("/{id}").get<AuthorParam, AuthorRecord>(info("Получить автора по айди")) { param ->
            respond(AuthorService.getRecord(param))
        }
    }
}

data class AuthorRecord(
    @Min(1) val id: Int? = null,
    val fio: String? = null,
    val createdAt: Long? = null
)

data class AuthorParam(
    @PathParam("Айди автора") val id: Int
)