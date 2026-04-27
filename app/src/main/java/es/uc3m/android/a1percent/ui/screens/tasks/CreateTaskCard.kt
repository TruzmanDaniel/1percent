package es.uc3m.android.a1percent.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.uc3m.android.a1percent.data.model.enums.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * CreateTaskCard renderiza el formulario en una card modal overlay.
 * La logica de estado vive en el ViewModel para mantener la UI simple.
 */



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskCard(
    onDismiss: () -> Unit,  // onDismiss is the function used for closing this card
    viewModel: CreateTaskViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var createErrorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Values for glassmorphism style
        val glassShape = RoundedCornerShape(24.dp)
        val glassGradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
            )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = glassShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = glassGradient)
            ) {

                // Header Section - Fixed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create Task", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = {
                        viewModel.resetState() // Clear form state when closing
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Content Section - Scrollable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
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
                                .fillMaxWidth(),
                                // outlinedtextfield por defecto son transparentes, por lo que se ve el fondo glass de la column
//                            colors = OutlinedTextFieldDefaults.colors(
//                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
//                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
//                            )
                        )

                        ExposedDropdownMenu(
                            expanded = uiState.isCategoryDropdownExpanded,
                            onDismissRequest = { viewModel.onCategoryDropdownExpandedChange(false) },

                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            tonalElevation = 0.dp,
                            shadowElevation = 10.dp
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            // Select THIS WEEK
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

                            // Select Specific Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.deadlineOption == DeadlineOption.EXACT_DATE,
                                    onClick = { viewModel.onDeadlineOptionSelected(DeadlineOption.EXACT_DATE) }
                                )
                                Text("Specific date")
                            }

                            if (uiState.deadlineOption == DeadlineOption.EXACT_DATE) {
                                val formattedDate = uiState.selectedEpochDay?.let { epochDay ->
                                    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH)
                                    formatter.format(Date(epochDay * 86_400_000L))
                                } ?: "No date selected"

                                // Clickable box for changing/selecting and displaying Date
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onDeadlineOptionSelected(DeadlineOption.EXACT_DATE)
                                        }
                                ) {
                                    OutlinedTextField(
                                        value = formattedDate,
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        label = { Text("Deadline date") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.primary,
                                            disabledLabelColor = MaterialTheme.colorScheme.primary,
                                            disabledContainerColor = Color.Transparent
                                        )
                                    )
                                }
                            }
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
                        DatePicker(
                            state = datePickerState,
                            colors = DatePickerDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                            )
                        )
                    }
                }


                // Button Section - Fixed

                // HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.16f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        createErrorMessage?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                createErrorMessage = null
                                viewModel.createTask(
                                    onSuccess = {
                                        viewModel.resetState()
                                        onDismiss()
                                    },
                                    onError = { error -> createErrorMessage = error }
                                )
                            },
                            enabled = uiState.canCreateTask,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (uiState.isLoading) "Creating..." else "Create Task")
                        }
                    }
                }
            }
        }
    }


    // Dialog to create a custom category (Overlay component)
    if (uiState.isCreateCategoryDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onCreateCategoryDismissed()
            },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
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