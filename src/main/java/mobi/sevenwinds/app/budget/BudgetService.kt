package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorEntity
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): ResponseBudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = body.authorId?.let { AuthorEntity.findById(it) }
            }
            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val baseQuery = run {
                if (param.author_full_name != null) {
                    return@run BudgetTable.join(AuthorTable, JoinType.INNER, BudgetTable.authorId, AuthorTable.id)
                } else return@run BudgetTable
            }

            val itemsQuery = baseQuery
                .select { BudgetTable.year eq param.year }
                .apply {
                    param.author_full_name?.let {
                        andWhere {
                            AuthorTable.fullName.lowerCase().like("%${it.lowercase()}%")
                        }
                    }
                }
                .orderBy(
                    *(listOf(
                        Pair(BudgetTable.month, SortOrder.ASC),
                        Pair(BudgetTable.amount, SortOrder.DESC)
                    ).toTypedArray())
                )

            val sumByTypeQuery = baseQuery
                .slice(BudgetTable.type, BudgetTable.amount.sum())
                .select { BudgetTable.year eq param.year }
                .apply {
                    param.author_full_name?.let {
                        andWhere {
                            AuthorTable.fullName.lowerCase().like("%${it.lowercase()}%")
                        }
                    }
                }
                .groupBy(BudgetTable.type)

            val total = itemsQuery.count()
            val data = BudgetEntity.wrapRows(itemsQuery.limit(param.limit, param.offset)).map { it.toResponse() }
            val sumByType = sumByTypeQuery
                .associate { row ->
                    val type = row[BudgetTable.type].name
                    val sum = row[BudgetTable.amount.sum()] ?: 0
                    type to sum
                }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data,
            )
        }
    }
}