package es.uc3m.android.a1percent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.TaskCategoryRepository
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.enums.Category
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskCard(
    task: Task,
    onSave: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val userId = SessionRepository.currentUser.collectAsStateWithLifecycle().value?.id

    val customCategories by TaskCategoryRepository.customCategories.collectAsStateWithLifecycle()
    val predefinedCategories = TaskCategoryRepository.predefinedCategories

    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }

    var selectedCategory by remember { mutableStateOf(task.category) }
    var selectedCustomCategoryName by remember { mutableStateOf(task.customCategoryName) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isCreateCategoryDialogVisible by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val categoryLabel = selectedCustomCategoryName ?: selectedCategory.displayName

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val glassShape = RoundedCornerShape(24.dp)
            val glassGradient = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                )
            )

            Card(
                modifier = Modifier.fillMaxWidth(0.95f),
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
                        .fillMaxWidth()
                        .background(brush = glassGradient)
                ) {
                    // Header
                    Text(
                        text = "Edit ${if (task.goalId != null) "Mission" else "Task"}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                    )

                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )

                        // Category dropdown
                        ExposedDropdownMenuBox(
                            expanded = isCategoryDropdownExpanded,
                            onExpandedChange = { isCategoryDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = categoryLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = isCategoryDropdownExpanded,
                                onDismissRequest = { isCategoryDropdownExpanded = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                tonalElevation = 0.dp,
                                shadowElevation = 10.dp
                            ) {
                                predefinedCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.displayName) },
                                        onClick = {
                                            selectedCategory = category
                                            selectedCustomCategoryName = null
                                            isCategoryDropdownExpanded = false
                                        }
                                    )
                                }

                                if (customCategories.isNotEmpty()) {
                                    HorizontalDivider()
                                    customCategories.forEach { custom ->
                                        DropdownMenuItem(
                                            text = { Text(custom) },
                                            onClick = {
                                                selectedCustomCategoryName = custom
                                                isCategoryDropdownExpanded = false
                                            },
                                            trailingIcon = {
                                                IconButton(onClick = {
                                                    scope.launch {
                                                        userId?.let {
                                                            TaskCategoryRepository.deleteCustomCategory(it, custom)
                                                        }
                                                        if (selectedCustomCategoryName == custom) {
                                                            selectedCustomCategoryName = null
                                                            selectedCategory = Category.PERSONAL
                                                        }
                                                    }
                                                }) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete category",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("Create a Category") },
                                    onClick = {
                                        isCategoryDropdownExpanded = false
                                        newCategoryName = ""
                                        isCreateCategoryDialogVisible = true
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "XP: ${task.xp}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Difficulty: ${task.difficulty}/5",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Footer buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onSave(
                                    task.copy(
                                        title = title,
                                        description = description,
                                        category = selectedCategory,
                                        customCategoryName = selectedCustomCategoryName
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = title.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // Create category dialog
    if (isCreateCategoryDialogVisible) {
        AlertDialog(
            onDismissRequest = { isCreateCategoryDialogVisible = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
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
                        val name = newCategoryName.trim()
                        scope.launch {
                            val saved = userId?.let {
                                TaskCategoryRepository.addCustomCategory(it, name)
                            }
                            if (saved != null) {
                                selectedCustomCategoryName = saved
                                selectedCategory = Category.PERSONAL
                            }
                            isCreateCategoryDialogVisible = false
                            newCategoryName = ""
                        }
                    },
                    enabled = newCategoryName.trim().isNotEmpty()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = {
                    isCreateCategoryDialogVisible = false
                    newCategoryName = ""
                }) { Text("Cancel") }
            }
        )
    }
}
