package mobi.sevenwinds.app.author

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object AuthorService {
    suspend fun addRecord(body: AuthorRecord): ResponseAuthorRecord = withContext(Dispatchers.IO) {
        transaction {
            val authorRecord = AuthorEntity.new {
                this.fullName = body.fullName
                this.createdAt = DateTime.now()
            }
            return@transaction authorRecord.toResponse()
        }
    }
}