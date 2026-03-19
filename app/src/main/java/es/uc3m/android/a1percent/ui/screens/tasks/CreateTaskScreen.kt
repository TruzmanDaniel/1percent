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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.TaskCategoryRepository
import es.uc3m.android.a1percent.data.model.TaskDeadline
import es.uc3m.android.a1percent.data.model.enums.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class DeadlineOption {
    THIS_WEEK,
    EXACT_DATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(navController: NavController) {
    var taskName by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.AUTOMATIC) }
    var selectedCustomCategoryName by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var hasDeadline by rememberSaveable { mutableStateOf(false) }
    var deadlineOption by rememberSaveable { mutableStateOf(DeadlineOption.THIS_WEEK) }
    var selectedEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePickerDialog by rememberSaveable { mutableStateOf(false) }

    val customCategories by TaskCategoryRepository.customCategories.collectAsState()
    val predefinedCategories = remember { TaskCategoryRepository.predefinedCategories }
    val selectedCategoryLabel = selectedCustomCategoryName ?: selectedCategory.displayName

    if (showDatePickerDialog) {
        val millisPerDay = 86_400_000L
        val initialMillis = (selectedEpochDay ?: (System.currentTimeMillis() / millisPerDay)) * millisPerDay
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            selectedEpochDay = selectedMillis / millisPerDay
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showCreateCategoryDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateCategoryDialog = false
                newCategoryName = ""
            },
            title = { Text("Create a Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val savedName = TaskCategoryRepository.addCustomCategory(newCategoryName)
                        if (savedName != null) {
                            selectedCustomCategoryName = savedName
                            selectedCategory = Category.AUTOMATIC
                        }
                        showCreateCategoryDialog = false
                        newCategoryName = ""
                    },
                    enabled = newCategoryName.trim().isNotEmpty()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateCategoryDialog = false
                        newCategoryName = ""
                    }
                ) { Text("Cancel") }
            }
        )
    }

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
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text("Task Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = taskDescription,
                onValueChange = { taskDescription = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategoryLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Category.AUTOMATIC.displayName) },
                        onClick = {
                            selectedCategory = Category.AUTOMATIC
                            selectedCustomCategoryName = null
                            categoryExpanded = false
                        }
                    )

                    HorizontalDivider()

                    predefinedCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                selectedCategory = category
                                selectedCustomCategoryName = null
                                categoryExpanded = false
                            }
                        )
                    }

                    if (customCategories.isNotEmpty()) {
                        HorizontalDivider()
                        customCategories.forEach { customCategory ->
                            DropdownMenuItem(
                                text = { Text(customCategory) },
                                onClick = {
                                    selectedCustomCategoryName = customCategory
                                    selectedCategory = Category.AUTOMATIC
                                    categoryExpanded = false
                                }
                            )
                        }
                    }

                    HorizontalDivider()

                    DropdownMenuItem(
                        text = { Text("Create a Category") },
                        onClick = {
                            categoryExpanded = false
                            showCreateCategoryDialog = true
                        }
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasDeadline,
                        onCheckedChange = { checked ->
                            hasDeadline = checked
                            if (!checked) {
                                selectedEpochDay = null
                            }
                        }
                    )
                    Text("Set deadline")
                }

                if (hasDeadline) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = deadlineOption == DeadlineOption.THIS_WEEK,
                            onClick = { deadlineOption = DeadlineOption.THIS_WEEK }
                        )
                        Text("This Week")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = deadlineOption == DeadlineOption.EXACT_DATE,
                            onClick = {
                                deadlineOption = DeadlineOption.EXACT_DATE
                                showDatePickerDialog = true
                            }
                        )
                        Text("Specific date")
                    }

                    if (deadlineOption == DeadlineOption.EXACT_DATE) {
                        val formattedDate = selectedEpochDay?.let { epochDay ->
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

                        TextButton(onClick = { showDatePickerDialog = true }) {
                            Text(if (selectedEpochDay == null) "Select date" else "Change date")
                        }
                    }
                }
            }

            val selectedDeadline: TaskDeadline? = when {
                !hasDeadline -> null
                deadlineOption == DeadlineOption.THIS_WEEK -> TaskDeadline.ThisWeek
                else -> selectedEpochDay?.let { TaskDeadline.OnDate(it) }
            }

            val canCreateTask = !hasDeadline || selectedDeadline != null

            Button(
                onClick = {
                    // TODO: if selectedCategory == AUTOMATIC and no custom category is selected:
                    // infer/predict the best predefined enum category in a future iteration.
                    // Custom categories are only for manual selection of category
                    // TODO: persist task with selectedCategory, selectedCustomCategoryName and selectedDeadline.
                    navController.popBackStack()
                },
                enabled = canCreateTask,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Task")
            }
        }
    }
}
