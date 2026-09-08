package com.shohan.khatago.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohan.khatago.data.local.db.entity.PersonEntity
import com.shohan.khatago.data.local.db.entity.PersonalDebtEntity
import com.shohan.khatago.data.local.db.entity.PersonalLendingEntity
import com.shohan.khatago.data.local.db.entity.PersonalRepaymentEntity
import com.shohan.khatago.data.local.db.entity.PersonalReturnEntity
import com.shohan.khatago.data.local.db.rows.BorrowedRow
import com.shohan.khatago.data.local.db.rows.LentRow
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalDao {

    // ------------------------------------------------------------------ people

    @Query("SELECT * FROM people WHERE archived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observePeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE id = :personId")
    fun observePerson(personId: Long): Flow<PersonEntity?>

    @Query("SELECT * FROM people WHERE id = :personId")
    suspend fun getPerson(personId: Long): PersonEntity?

    @Query("SELECT * FROM people ORDER BY id ASC")
    suspend fun getAllPeople(): List<PersonEntity>

    @Insert
    suspend fun insertPerson(person: PersonEntity): Long

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    // ------------------------------------------------- money I borrowed (payable)

    @Query(
        """
        SELECT d.id AS id,
               d.personId AS personId,
               COALESCE(p.name, '') AS personName,
               COALESCE(p.relationship, '') AS relationship,
               d.amount AS amount,
               d.borrowedDateEpochDay AS borrowedDateEpochDay,
               d.expectedReturnDateEpochDay AS expectedReturnDateEpochDay,
               d.notes AS notes,
               COALESCE((SELECT SUM(r.amount) FROM personal_repayments r WHERE r.debtId = d.id), 0) AS paidAmount,
               d.archived AS archived
        FROM personal_debts d
        LEFT JOIN people p ON p.id = d.personId
        ORDER BY d.archived ASC, d.borrowedDateEpochDay DESC, d.id DESC
        """
    )
    fun observeBorrowed(): Flow<List<BorrowedRow>>

    @Query("SELECT * FROM personal_debts WHERE id = :debtId")
    fun observeDebt(debtId: Long): Flow<PersonalDebtEntity?>

    @Query("SELECT * FROM personal_debts WHERE id = :debtId")
    suspend fun getDebt(debtId: Long): PersonalDebtEntity?

    @Query("SELECT * FROM personal_debts WHERE personId = :personId ORDER BY borrowedDateEpochDay DESC")
    fun observeDebtsForPerson(personId: Long): Flow<List<PersonalDebtEntity>>

    @Query("SELECT * FROM personal_debts ORDER BY id ASC")
    suspend fun getAllDebts(): List<PersonalDebtEntity>

    @Insert
    suspend fun insertDebt(debt: PersonalDebtEntity): Long

    @Update
    suspend fun updateDebt(debt: PersonalDebtEntity)

    @Delete
    suspend fun deleteDebt(debt: PersonalDebtEntity)

    // ---------------------------------------------------------------- repayments

    @Query("SELECT * FROM personal_repayments WHERE debtId = :debtId ORDER BY dateEpochDay DESC, id DESC")
    fun observeRepayments(debtId: Long): Flow<List<PersonalRepaymentEntity>>

    @Query("SELECT * FROM personal_repayments WHERE debtId = :debtId ORDER BY dateEpochDay ASC, id ASC")
    suspend fun getRepayments(debtId: Long): List<PersonalRepaymentEntity>

    @Query("SELECT * FROM personal_repayments WHERE id = :repaymentId")
    suspend fun getRepayment(repaymentId: Long): PersonalRepaymentEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM personal_repayments WHERE dateEpochDay = :epochDay")
    suspend fun repaymentsOnDay(epochDay: Long): Long

    @Insert
    suspend fun insertRepayment(repayment: PersonalRepaymentEntity): Long

    @Update
    suspend fun updateRepayment(repayment: PersonalRepaymentEntity)

    @Delete
    suspend fun deleteRepayment(repayment: PersonalRepaymentEntity)

    // --------------------------------------------------- money I lent (receivable)

    @Query(
        """
        SELECT l.id AS id,
               l.personId AS personId,
               COALESCE(p.name, '') AS personName,
               COALESCE(p.relationship, '') AS relationship,
               l.amount AS amount,
               l.lentDateEpochDay AS lentDateEpochDay,
               l.expectedReturnDateEpochDay AS expectedReturnDateEpochDay,
               l.notes AS notes,
               COALESCE((SELECT SUM(r.amount) FROM personal_returns r WHERE r.lendingId = l.id), 0) AS receivedAmount,
               l.archived AS archived
        FROM personal_lending l
        LEFT JOIN people p ON p.id = l.personId
        ORDER BY l.archived ASC, l.lentDateEpochDay DESC, l.id DESC
        """
    )
    fun observeLent(): Flow<List<LentRow>>

    @Query("SELECT * FROM personal_lending WHERE id = :lendingId")
    fun observeLending(lendingId: Long): Flow<PersonalLendingEntity?>

    @Query("SELECT * FROM personal_lending WHERE id = :lendingId")
    suspend fun getLending(lendingId: Long): PersonalLendingEntity?

    @Query("SELECT * FROM personal_lending WHERE personId = :personId ORDER BY lentDateEpochDay DESC")
    fun observeLendingForPerson(personId: Long): Flow<List<PersonalLendingEntity>>

    @Query("SELECT * FROM personal_lending ORDER BY id ASC")
    suspend fun getAllLending(): List<PersonalLendingEntity>

    @Insert
    suspend fun insertLending(lending: PersonalLendingEntity): Long

    @Update
    suspend fun updateLending(lending: PersonalLendingEntity)

    @Delete
    suspend fun deleteLending(lending: PersonalLendingEntity)

    // ------------------------------------------------------------------ returns

    @Query("SELECT * FROM personal_returns WHERE lendingId = :lendingId ORDER BY dateEpochDay DESC, id DESC")
    fun observeReturns(lendingId: Long): Flow<List<PersonalReturnEntity>>

    @Query("SELECT * FROM personal_returns WHERE lendingId = :lendingId ORDER BY dateEpochDay ASC, id ASC")
    suspend fun getReturns(lendingId: Long): List<PersonalReturnEntity>

    @Query("SELECT * FROM personal_returns WHERE id = :returnId")
    suspend fun getReturn(returnId: Long): PersonalReturnEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM personal_returns WHERE dateEpochDay = :epochDay")
    suspend fun returnsOnDay(epochDay: Long): Long

    @Insert
    suspend fun insertReturn(entry: PersonalReturnEntity): Long

    @Update
    suspend fun updateReturn(entry: PersonalReturnEntity)

    @Delete
    suspend fun deleteReturn(entry: PersonalReturnEntity)

    // ------------------------------------------------- obligations / reminders

    @Query(
        """
        SELECT p.name AS title,
               d.notes AS subtitle,
               d.id AS refId,
               d.expectedReturnDateEpochDay AS dueDateEpochDay,
               d.amount AS scheduledAmount,
               (d.amount - COALESCE((SELECT SUM(r.amount) FROM personal_repayments r WHERE r.debtId = d.id), 0)) AS remainingAmount
        FROM personal_debts d
        LEFT JOIN people p ON p.id = d.personId
        WHERE d.archived = 0
          AND d.amount > COALESCE((SELECT SUM(r.amount) FROM personal_repayments r WHERE r.debtId = d.id), 0)
          AND d.expectedReturnDateEpochDay IS NOT NULL
          AND d.expectedReturnDateEpochDay <= :horizonEpochDay
        ORDER BY d.expectedReturnDateEpochDay ASC
        LIMIT :limit
        """
    )
    suspend fun upcomingBorrowed(horizonEpochDay: Long, limit: Int): List<UpcomingPersonalRow>

    @Query(
        """
        SELECT p.name AS title,
               d.notes AS subtitle,
               d.id AS refId,
               d.expectedReturnDateEpochDay AS dueDateEpochDay,
               d.amount AS scheduledAmount,
               (d.amount - COALESCE((SELECT SUM(r.amount) FROM personal_repayments r WHERE r.debtId = d.id), 0)) AS remainingAmount
        FROM personal_debts d
        LEFT JOIN people p ON p.id = d.personId
        WHERE d.archived = 0
          AND d.amount > COALESCE((SELECT SUM(r.amount) FROM personal_repayments r WHERE r.debtId = d.id), 0)
          AND d.expectedReturnDateEpochDay IS NOT NULL
          AND d.expectedReturnDateEpochDay <= :horizonEpochDay
        ORDER BY d.expectedReturnDateEpochDay ASC
        LIMIT :limit
        """
    )
    fun observeUpcomingBorrowed(horizonEpochDay: Long, limit: Int): kotlinx.coroutines.flow.Flow<List<UpcomingPersonalRow>>

    @Query(
        """
        SELECT p.name AS title,
               l.notes AS subtitle,
               l.id AS refId,
               l.expectedReturnDateEpochDay AS dueDateEpochDay,
               l.amount AS scheduledAmount,
               (l.amount - COALESCE((SELECT SUM(r.amount) FROM personal_returns r WHERE r.lendingId = l.id), 0)) AS remainingAmount
        FROM personal_lending l
        LEFT JOIN people p ON p.id = l.personId
        WHERE l.archived = 0
          AND l.amount > COALESCE((SELECT SUM(r.amount) FROM personal_returns r WHERE r.lendingId = l.id), 0)
          AND l.expectedReturnDateEpochDay IS NOT NULL
          AND l.expectedReturnDateEpochDay <= :horizonEpochDay
        ORDER BY l.expectedReturnDateEpochDay ASC
        LIMIT :limit
        """
    )
    suspend fun upcomingLent(horizonEpochDay: Long, limit: Int): List<UpcomingPersonalRow>

    @Query(
        """
        SELECT p.name AS title,
               l.notes AS subtitle,
               l.id AS refId,
               l.expectedReturnDateEpochDay AS dueDateEpochDay,
               l.amount AS scheduledAmount,
               (l.amount - COALESCE((SELECT SUM(r.amount) FROM personal_returns r WHERE r.lendingId = l.id), 0)) AS remainingAmount
        FROM personal_lending l
        LEFT JOIN people p ON p.id = l.personId
        WHERE l.archived = 0
          AND l.amount > COALESCE((SELECT SUM(r.amount) FROM personal_returns r WHERE r.lendingId = l.id), 0)
          AND l.expectedReturnDateEpochDay IS NOT NULL
          AND l.expectedReturnDateEpochDay <= :horizonEpochDay
        ORDER BY l.expectedReturnDateEpochDay ASC
        LIMIT :limit
        """
    )
    fun observeUpcomingLent(horizonEpochDay: Long, limit: Int): kotlinx.coroutines.flow.Flow<List<UpcomingPersonalRow>>
}

data class UpcomingPersonalRow(
    val title: String,
    val subtitle: String,
    val refId: Long,
    val dueDateEpochDay: Long,
    val scheduledAmount: Long,
    val remainingAmount: Long
)
