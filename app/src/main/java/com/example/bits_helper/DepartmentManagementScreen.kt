package com.example.bits_helper

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bits_helper.data.DepartmentEntity
import com.example.bits_helper.ui.CartridgeViewModel
import com.example.bits_helper.utils.RoomRangeParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentManagementScreen(
    vm: CartridgeViewModel,
    onBack: () -> Unit
) {
    var departments by remember { mutableStateOf<List<DepartmentEntity>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingDepartment by remember { mutableStateOf<DepartmentEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<DepartmentEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Обработка системной кнопки "Назад"
    BackHandler {
        onBack()
    }

    // Загружаем подразделения при инициализации
    LaunchedEffect(Unit) {
        vm.getAllDepartmentEntities { deptList ->
            departments = deptList
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Заголовок
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "🏢 Управление подразделениями",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Кнопка добавления
            FilledTonalButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Добавить")
                Spacer(Modifier.width(8.dp))
                Text("Добавить подразделение")
            }

            Spacer(Modifier.height(16.dp))

            // Список подразделений
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(departments) { department ->
                    DepartmentCard(
                        department = department,
                        onEdit = {
                            editingDepartment = department
                            showEditDialog = true
                        },
                        onDelete = {
                            showDeleteDialog = department
                        }
                    )
                }
            }
        }

        // Snackbar для уведомлений
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Диалог добавления подразделения
    if (showAddDialog) {
        AddDepartmentDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, rooms ->
                vm.addDepartment(name, rooms) { success ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (success) {
                            snackbarHostState.showSnackbar("Подразделение добавлено")
                            vm.getAllDepartmentEntities { deptList ->
                                departments = deptList
                            }
                        } else {
                            snackbarHostState.showSnackbar("Ошибка при добавлении подразделения")
                        }
                    }
                }
                showAddDialog = false
            }
        )
    }

    // Диалог редактирования подразделения
    if (showEditDialog && editingDepartment != null) {
        EditDepartmentDialog(
            department = editingDepartment!!,
            onDismiss = {
                showEditDialog = false
                editingDepartment = null
            },
            onSave = { name, rooms ->
                vm.updateDepartment(name, rooms) { success ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (success) {
                            snackbarHostState.showSnackbar("Подразделение обновлено")
                            vm.getAllDepartmentEntities { deptList ->
                                departments = deptList
                            }
                        } else {
                            snackbarHostState.showSnackbar("Ошибка при обновлении подразделения")
                        }
                    }
                }
                showEditDialog = false
                editingDepartment = null
            }
        )
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog != null) {
        val departmentToDelete = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Удалить подразделение", fontWeight = FontWeight.SemiBold) },
            text = { 
                Text("Вы действительно хотите удалить подразделение \"${departmentToDelete.name}\"?\n\nЭто действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteDepartment(departmentToDelete.name) { success ->
                            CoroutineScope(Dispatchers.Main).launch {
                                if (success) {
                                    snackbarHostState.showSnackbar("Подразделение удалено")
                                    vm.getAllDepartmentEntities { deptList ->
                                        departments = deptList
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("Ошибка при удалении подразделения")
                                }
                            }
                        }
                        showDeleteDialog = null
                    }
                ) { 
                    Text("Удалить", color = MaterialTheme.colorScheme.error) 
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { 
                    Text("Отмена") 
                }
            }
        )
    }
}

@Composable
fun DepartmentCard(
    department: DepartmentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = department.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            val parsedRooms = RoomRangeParser.parseRooms(department.rooms)
            val displayRooms = if (parsedRooms.size <= 10) {
                parsedRooms.joinToString(", ")
            } else {
                "${parsedRooms.take(10).joinToString(", ")} и еще ${parsedRooms.size - 10}"
            }
            
            Text(
                text = "Кабинеты: $displayRooms",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AddDepartmentDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, rooms: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rooms by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf("") }
    var roomsError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить подразделение", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = ""
                    },
                    label = { Text("Название подразделения") },
                    isError = nameError.isNotEmpty(),
                    supportingText = if (nameError.isNotEmpty()) { { Text(nameError) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rooms,
                    onValueChange = { 
                        rooms = it
                        roomsError = ""
                    },
                    label = { Text("Кабинеты") },
                    placeholder = { Text("101,102,103 или 201-205") },
                    isError = roomsError.isNotEmpty(),
                    supportingText = if (roomsError.isNotEmpty()) { { Text(roomsError) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Предварительный просмотр кабинетов
                if (rooms.isNotBlank()) {
                    val validation = RoomRangeParser.validateRoomsString(rooms)
                    if (validation.isValid) {
                        val parsedRooms = RoomRangeParser.parseRooms(rooms)
                        Text(
                            text = "Кабинеты: ${parsedRooms.joinToString(", ")}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = validation.errorMessage,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                Text(
                    text = "Примеры: 101,102,103 или 201-205 или 301,302,303,304,305",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    var hasError = false
                    
                    if (name.isBlank()) {
                        nameError = "Название не может быть пустым"
                        hasError = true
                    }
                    
                    if (rooms.isBlank()) {
                        roomsError = "Список кабинетов не может быть пустым"
                        hasError = true
                    } else {
                        val validation = RoomRangeParser.validateRoomsString(rooms)
                        if (!validation.isValid) {
                            roomsError = validation.errorMessage
                            hasError = true
                        }
                    }
                    
                    if (!hasError) {
                        onSave(name.trim(), rooms.trim())
                    }
                }
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun EditDepartmentDialog(
    department: DepartmentEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, rooms: String) -> Unit
) {
    var name by remember { mutableStateOf(department.name) }
    var rooms by remember { mutableStateOf(department.rooms) }
    var nameError by remember { mutableStateOf("") }
    var roomsError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать подразделение", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = ""
                    },
                    label = { Text("Название подразделения") },
                    isError = nameError.isNotEmpty(),
                    supportingText = if (nameError.isNotEmpty()) { { Text(nameError) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rooms,
                    onValueChange = { 
                        rooms = it
                        roomsError = ""
                    },
                    label = { Text("Кабинеты") },
                    placeholder = { Text("101,102,103 или 201-205") },
                    isError = roomsError.isNotEmpty(),
                    supportingText = if (roomsError.isNotEmpty()) { { Text(roomsError) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Предварительный просмотр кабинетов
                if (rooms.isNotBlank()) {
                    val validation = RoomRangeParser.validateRoomsString(rooms)
                    if (validation.isValid) {
                        val parsedRooms = RoomRangeParser.parseRooms(rooms)
                        Text(
                            text = "Кабинеты: ${parsedRooms.joinToString(", ")}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = validation.errorMessage,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                Text(
                    text = "Примеры: 101,102,103 или 201-205 или 301,302,303,304,305",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    var hasError = false
                    
                    if (name.isBlank()) {
                        nameError = "Название не может быть пустым"
                        hasError = true
                    }
                    
                    if (rooms.isBlank()) {
                        roomsError = "Список кабинетов не может быть пустым"
                        hasError = true
                    } else {
                        val validation = RoomRangeParser.validateRoomsString(rooms)
                        if (!validation.isValid) {
                            roomsError = validation.errorMessage
                            hasError = true
                        }
                    }
                    
                    if (!hasError) {
                        onSave(name.trim(), rooms.trim())
                    }
                }
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
