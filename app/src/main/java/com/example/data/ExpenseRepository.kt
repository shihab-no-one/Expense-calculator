package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesBetween(startMillis: Long, endMillis: Long): Flow<List<Expense>> =
        expenseDao.getExpensesBetween(startMillis, endMillis)

    suspend fun insert(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun update(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun delete(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun deleteById(id: Long) = expenseDao.deleteExpenseById(id)
}
