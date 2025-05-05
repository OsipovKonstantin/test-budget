package mobi.sevenwinds.app.author


import com.fasterxml.jackson.annotation.JsonFormat
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import org.jetbrains.annotations.NotNull
import org.joda.time.DateTime
import java.time.LocalDateTime

fun NormalOpenAPIRoute.author() {
    route("/author") {
        route("/add").post<Unit, ResponseAuthorRecord, AuthorRecord>(info("Добавить автора")) { param, body ->
            respond(AuthorService.addRecord(body))
        }
    }
}

data class AuthorRecord(
    @field:NotNull val fullName: String
)

data class ResponseAuthorRecord(
    val authorId: Int,
    val fullName: String,
    val createdAt: DateTime
)

data class BudgetAuthorRecord(
    val fullName: String,
    val createdAt: DateTime
)