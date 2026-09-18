package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.ExpenseViewModel
import java.util.Calendar

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, dateMillis: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val context = LocalContext.current
    val dateCalendar = remember(selectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedDateMillis = newCal.timeInMillis
            },
            dateCalendar.get(Calendar.YEAR),
            dateCalendar.get(Calendar.MONTH),
            dateCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_expense_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleError && it.isNotBlank()) titleError = false
                    },
                    label = { Text(stringResource(R.string.expense_title_label)) },
                    placeholder = { Text("e.g. Lunch, Grocery, Bus") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null
                        )
                    },
                    isError = titleError,
                    supportingText = {
                        if (titleError) {
                            Text("Title cannot be empty")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        // Allow only valid decimal number input
                        if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                            amountText = it
                            if (amountError && (it.toDoubleOrNull() ?: 0.0) > 0) {
                                amountError = false
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.expense_amount_label)) },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null
                        )
                    },
                    isError = amountError,
                    supportingText = {
                        if (amountError) {
                            Text("Enter a valid cost amount greater than 0")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                            val validTitle = title.isNotBlank()
                            val validAmount = parsedAmount > 0.0

                            titleError = !validTitle
                            amountError = !validAmount

                            if (validTitle && validAmount) {
                                onConfirm(title.trim(), parsedAmount, selectedDateMillis)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                // Date Selector
                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("expense_date_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Select Date"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Date: ${ExpenseViewModel.formatDate(selectedDateMillis)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                    val validTitle = title.isNotBlank()
                    val validAmount = parsedAmount > 0.0

                    titleError = !validTitle
                    amountError = !validAmount

                    if (validTitle && validAmount) {
                        onConfirm(title.trim(), parsedAmount, selectedDateMillis)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text(stringResource(R.string.save_expense_button))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("cancel_expense_button")
            ) {
                Text(stringResource(R.string.cancel_button))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
