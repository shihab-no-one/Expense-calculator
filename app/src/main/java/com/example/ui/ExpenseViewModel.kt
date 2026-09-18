package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Expense
import com.example.data.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository
) : AndroidViewModel(application) {

    // All logged expenses
    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selected month and year for Monthly Expense view
    private val calendar = Calendar.getInstance()
    private val _selectedYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear

    private val _selectedMonth = MutableStateFlow(calendar.get(Calendar.MONTH)) // 0-based (0 = Jan)
    val selectedMonth: StateFlow<Int> = _selectedMonth

    // Expenses filtered for the selected month
    val selectedMonthExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        _selectedYear,
        _selectedMonth
    ) { expenses, year, month ->
        val cal = Calendar.getInstance()
        expenses.filter { expense ->
            cal.timeInMillis = expense.dateMillis
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Total for today
    val todayTotal: StateFlow<Double> = allExpenses.combine(_selectedYear) { expenses, _ ->
        val todayCal = Calendar.getInstance()
        val expenseCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)

        expenses.filter {
            expenseCal.timeInMillis = it.dateMillis
            expenseCal.get(Calendar.YEAR) == todayYear &&
                expenseCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
        }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Total for the selected month
    val selectedMonthTotal: StateFlow<Double> = selectedMonthExpenses.combine(_selectedYear) { list, _ ->
        list.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Total for the actual current ongoing month
    val currentMonthTotal: StateFlow<Double> = allExpenses.combine(_selectedYear) { expenses, _ ->
        val now = Calendar.getInstance()
        val currYear = now.get(Calendar.YEAR)
        val currMonth = now.get(Calendar.MONTH)
        val cal = Calendar.getInstance()

        expenses.filter {
            cal.timeInMillis = it.dateMillis
            cal.get(Calendar.YEAR) == currYear && cal.get(Calendar.MONTH) == currMonth
        }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun addExpense(title: String, amount: Double, dateMillis: Long = System.currentTimeMillis()) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty() || amount <= 0) return

        viewModelScope.launch {
            repository.insert(
                Expense(
                    title = trimmedTitle,
                    amount = amount,
                    dateMillis = dateMillis
                )
            )
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.delete(expense)
        }
    }

    fun deleteExpenseById(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun previousMonth() {
        if (_selectedMonth.value == 0) {
            _selectedMonth.value = 11
            _selectedYear.value -= 1
        } else {
            _selectedMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (_selectedMonth.value == 11) {
            _selectedMonth.value = 0
            _selectedYear.value += 1
        } else {
            _selectedMonth.value += 1
        }
    }

    fun resetToCurrentMonth() {
        val now = Calendar.getInstance()
        _selectedYear.value = now.get(Calendar.YEAR)
        _selectedMonth.value = now.get(Calendar.MONTH)
    }

    companion object {
        fun formatCurrency(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            return format.format(amount)
        }

        fun formatDate(millis: Long): String {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return formatter.format(millis)
        }

        fun formatTime(millis: Long): String {
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return formatter.format(millis)
        }

        fun formatMonthYear(year: Int, month: Int): String {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            return formatter.format(cal.time)
        }

        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val repo = ExpenseRepository(db.expenseDao())
                    return ExpenseViewModel(application, repo) as T
                }
            }
    }
}
