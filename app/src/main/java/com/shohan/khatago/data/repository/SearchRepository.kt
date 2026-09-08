package com.shohan.khatago.data.repository

import com.shohan.khatago.data.local.db.dao.SearchDao
import com.shohan.khatago.domain.model.AccountKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** One grouped result from the global search. */
data class SearchResultItem(
    val kind: AccountKind,
    val id: Long,
    val title: String,
    val subtitle: String,
    val amount: Long?,
    val date: LocalDate?
)

data class SearchResults(
    val query: String,
    val accounts: List<SearchResultItem> = emptyList(),
    val transactions: List<SearchResultItem> = emptyList()
) {
    val isEmpty: Boolean get() = accounts.isEmpty() && transactions.isEmpty()
    val total: Int get() = accounts.size + transactions.size
}

/**
 * Global search across shops, loans, EMIs, people, categories, descriptions and
 * notes. Results are grouped logically (accounts first, then history) and every
 * query is bounded so search stays instant.
 */
class SearchRepository(private val dao: SearchDao) {

    suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext SearchResults(trimmed)

        val shops = dao.searchShops(trimmed, LIMIT).map {
            SearchResultItem(AccountKind.SHOP_CREDIT, it.id, it.title, it.subtitle, null, null)
        }
        val loans = dao.searchLoans(trimmed, LIMIT).map {
            SearchResultItem(AccountKind.LOAN, it.id, it.title, it.subtitle, null, null)
        }
        val emis = dao.searchEmis(trimmed, LIMIT).map {
            SearchResultItem(AccountKind.EMI, it.id, it.title, it.subtitle, null, null)
        }
        val people = dao.searchPeople(trimmed, LIMIT).map {
            SearchResultItem(AccountKind.PERSONAL_DEBT, it.id, it.title, it.subtitle, null, null)
        }

        val transactions = dao.searchTransactions(trimmed, LIMIT).map {
            SearchResultItem(
                kind = AccountKind.PERSONAL_DEBT,
                id = it.id,
                title = it.title.ifBlank { it.subtitle },
                subtitle = it.detail,
                amount = it.amount,
                date = LocalDate.ofEpochDay(it.dateEpochDay)
            )
        }
        val income = dao.searchIncome(trimmed, LIMIT).map {
            SearchResultItem(
                kind = AccountKind.PERSONAL_DEBT,
                id = it.id,
                title = it.title.ifBlank { it.subtitle },
                subtitle = "Income",
                amount = it.amount,
                date = LocalDate.ofEpochDay(it.dateEpochDay)
            )
        }
        val expenses = dao.searchExpenses(trimmed, LIMIT).map {
            SearchResultItem(
                kind = AccountKind.PERSONAL_DEBT,
                id = it.id,
                title = it.title.ifBlank { it.subtitle },
                subtitle = "Expense",
                amount = it.amount,
                date = LocalDate.ofEpochDay(it.dateEpochDay)
            )
        }

        SearchResults(
            query = trimmed,
            accounts = shops + loans + emis + people,
            transactions = (transactions + income + expenses)
                .sortedByDescending { it.date?.toEpochDay() ?: 0L }
                .take(LIMIT * 2)
        )
    }

    private companion object {
        const val LIMIT = 20
    }
}
