package es.uc3m.android.a1percent.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.model.enums.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    navController: NavController,
    viewModel: CreateTaskViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Task") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = uiState.taskName,
                onValueChange = viewModel::onTaskNameChange,
                label = { Text("Task Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.taskDescription,
                onValueChange = viewModel::onTaskDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

        // CATEGORY DROPDOWN
            ExposedDropdownMenuBox(
                expanded = uiState.isCategoryDropdownExpanded,
                onExpandedChange = viewModel::onCategoryDropdownExpandedChange
            ) {
                OutlinedTextField(
                    value = uiState.selectedCategoryLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isCategoryDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = uiState.isCategoryDropdownExpanded,
                    onDismissRequest = { viewModel.onCategoryDropdownExpandedChange(false) }
                ) {
                    // AUTOMATIC
                    DropdownMenuItem(
                        text = { Text(Category.AUTOMATIC.displayName) },
                        onClick = { viewModel.onCategorySelected(Category.AUTOMATIC) }
                    )

                    HorizontalDivider()

                    // PREDEFINED
                    uiState.predefinedCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = { viewModel.onCategorySelected(category) }
                        )
                    }

                    // CUSTOM
                    if (uiState.customCategories.isNotEmpty()) {
                        HorizontalDivider()

                        uiState.customCategories.forEach { customCategory ->
                            DropdownMenuItem(
                                text = { Text(customCategory) },
                                onClick = { viewModel.onCustomCategorySelected(customCategory) }
                            )
                        }
                    }

                    HorizontalDivider()

                    // CREATE A CUSTOM
                    DropdownMenuItem(
                        text = { Text("Create a Category") },
                        onClick = viewModel::onCreateCategoryClicked
                    )
                }
            }


        // DEADLINE SELECTION
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.hasDeadline,
                        onCheckedChange = viewModel::onDeadlineEnabledChange
                    )
                    Text("Set deadline")
                }

                if (uiState.hasDeadline) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.deadlineOption == DeadlineOption.THIS_WEEK,
                            onClick = { viewModel.onDeadlineOptionSelected(DeadlineOption.THIS_WEEK) }
                        )
                        Text("This Week")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.deadlineOption == DeadlineOption.EXACT_DATE,
                            onClick = {
                                viewModel.onDeadlineOptionSelected(DeadlineOption.EXACT_DATE)
                            }
                        )
                        Text("Specific date")
                    }

                    if (uiState.deadlineOption == DeadlineOption.EXACT_DATE) {
                        val formattedDate = uiState.selectedEpochDay?.let { epochDay ->
                            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH)
                            formatter.format(Date(epochDay * 86_400_000L))
                        } ?: "No date selected"

                        OutlinedTextField(
                            value = formattedDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Deadline date") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        TextButton(onClick = { viewModel.onDeadlineOptionSelected(DeadlineOption.EXACT_DATE) }) {
                            Text(if (uiState.selectedEpochDay == null) "Select date" else "Change date")
                        }
                    }
                }
            }


            // CREATE TASK BUTTON
            Button(
                onClick = {
                    // TODO: if selectedCategory == AUTOMATIC and no custom category is selected:
                    // infer/predict the best predefined enum category in a future iteration.
                    // Custom categories are only for manual selection of category
                    // TODO: persist task with selectedCategory, selectedCustomCategoryName and selectedDeadline.
                    navController.popBackStack()
                },
                enabled = uiState.canCreateTask,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Task")
            }
        }
    }

        // Dialog to select a date in the calendar
    if (uiState.isDatePickerVisible) {
        val millisPerDay = 86_400_000L
        val initialMillis = (uiState.selectedEpochDay ?: (System.currentTimeMillis() / millisPerDay)) * millisPerDay
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = viewModel::onDatePickerDismissed,
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            viewModel.onDateSelected(selectedMillis / millisPerDay)
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDatePickerDismissed) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Dialog to create a custom category
    if (uiState.isCreateCategoryDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onCreateCategoryDismissed()
            },
            title = { Text("Create a Category") },
            text = {
                OutlinedTextField(
                    value = uiState.newCategoryName,
                    onValueChange = viewModel::onNewCategoryNameChange,
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onCreateCategoryConfirmed()
                    },
                    enabled = uiState.newCategoryName.trim().isNotEmpty()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onCreateCategoryDismissed()
                    }
                ) { Text("Cancel") }
            }
        )
    }

}

