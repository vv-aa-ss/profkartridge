package com.example.bits_helper

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bits_helper.data.Status
import com.example.bits_helper.ui.CartridgeUi
import com.example.bits_helper.ui.CartridgeViewModel
import kotlinx.coroutines.flow.StateFlow

/* =================== ЭКРАН СТАТИСТИКИ =================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    vm: CartridgeViewModel,
    onBack: () -> Unit
) {
    var showFilters by remember { mutableStateOf(false) }
    var viewType by remember { mutableStateOf(StatisticsViewType.CARDS) }
    var showContextMenu by remember { mutableStateOf<Long?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingCartridge by remember { mutableStateOf<CartridgeUi?>(null) }
    
    // Инициализация дат по умолчанию
    val today = LocalDate.now()
    val oneMonthAgo = today.minusMonths(1)
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val internalFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    var dateFrom by remember { mutableStateOf(oneMonthAgo.format(formatter)) }
    var dateTo by remember { mutableStateOf(today.format(formatter)) }
    var selectedDepartment by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<Status?>(null) }
    
    // Список филиалов из базы данных
    var departments by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Загружаем филиалы при инициализации
    LaunchedEffect(Unit) {
        vm.getAllDepartments { deptList ->
            departments = deptList
        }
    }
    
    // Отображение данных
    val filteredData = vm.getFilteredStatistics(
        dateFrom = LocalDate.parse(dateFrom, formatter).format(internalFormatter),
        dateTo = LocalDate.parse(dateTo, formatter).format(internalFormatter),
        department = selectedDepartment,
        status = selectedStatus
    ).collectAsState(initial = emptyList()).value
    
    // Обработчик системной кнопки "Назад"
    BackHandler {
        onBack()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Современный заголовок
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFilters = true }
                    ) {
                        Text(
                            text = "📊 Статистика",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = "Анализ данных по картриджам",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Современный переключатель видов
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3F4F6))
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { viewType = StatisticsViewType.CARDS },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewType == StatisticsViewType.CARDS) 
                                    Color(0xFF0078D4) else Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Карточки",
                                color = if (viewType == StatisticsViewType.CARDS) 
                                    Color.White else Color(0xFF6B7280),
                                fontSize = 14.sp
                            )
                        }
                        Button(
                            onClick = { viewType = StatisticsViewType.MODELS },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewType == StatisticsViewType.MODELS) 
                                    Color(0xFF0078D4) else Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Модели",
                                color = if (viewType == StatisticsViewType.MODELS) 
                                    Color.White else Color(0xFF6B7280),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            when (viewType) {
                StatisticsViewType.CARDS -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredData) { item ->
                            CartridgeCard(
                                item = item,
                                modifier = Modifier.fillMaxWidth(),
                                onStatusClick = { },
                                onLongClick = { 
                                    showContextMenu = item.id
                                }
                            )
                        }
                    }
                }
                StatisticsViewType.MODELS -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredData) { item ->
                            ModelCard(
                                status = item.status,
                                model = item.model,
                                department = item.department,
                                date = item.date
                            )
                        }
                    }
                }
            }
        }
        
        // Диалог фильтров
        if (showFilters) {
            StatisticsFiltersDialog(
                dateFrom = dateFrom,
                dateTo = dateTo,
                selectedDepartment = selectedDepartment,
                selectedStatus = selectedStatus,
                departments = departments,
                formatter = formatter,
                onDateFromChange = { dateFrom = it },
                onDateToChange = { dateTo = it },
                onDepartmentChange = { selectedDepartment = it },
                onStatusChange = { selectedStatus = it },
                onDismiss = { showFilters = false }
            )
        }
        
        // Контекстное меню для картриджа
        if (showContextMenu != null) {
            val cartridgeId = showContextMenu!!
            val cartridge = filteredData.find { it.id == cartridgeId }
            if (cartridge != null) {
                ContextMenuDialog(
                    cartridge = cartridge,
                    onDismiss = { showContextMenu = null },
                    onEdit = {
                        editingCartridge = cartridge
                        showContextMenu = null
                        showEditDialog = true
                    },
                    onDelete = {
                        vm.deleteById(cartridgeId)
                        showContextMenu = null
                    }
                )
            }
        }
        
        // Диалог редактирования картриджа
        if (showEditDialog && editingCartridge != null) {
            EditCartridgeDialog(
                cartridge = editingCartridge!!,
                onDismiss = { showEditDialog = false; editingCartridge = null },
                onSave = { number, room, model, date, status, notes ->
                    vm.updateCartridge(editingCartridge!!.id, number, room, model, date, status, notes)
                    showEditDialog = false
                    editingCartridge = null
                }
            )
        }
    }
}

enum class StatisticsViewType {
    CARDS, MODELS
}

@Composable
fun ModelCard(
    status: Status,
    model: String,
    department: String?,
    date: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Статус слева
            StatusBadge(status)
            Spacer(Modifier.width(16.dp))
            
            // Модель по центру
            Text(
                text = model,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1F2937),
                modifier = Modifier.weight(1f)
            )
            
            // Дата и филиал справа
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = date,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
                if (!department.isNullOrBlank()) {
                    Text(
                        text = department,
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsFiltersDialog(
    dateFrom: String,
    dateTo: String,
    selectedDepartment: String?,
    selectedStatus: Status?,
    departments: List<String>,
    formatter: DateTimeFormatter,
    onDateFromChange: (String) -> Unit,
    onDateToChange: (String) -> Unit,
    onDepartmentChange: (String?) -> Unit,
    onStatusChange: (Status?) -> Unit,
    onDismiss: () -> Unit
) {
    var showDateFromPicker by remember { mutableStateOf(false) }
    var showDateToPicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Фильтры статистики", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Выбор даты "от"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Дата от",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDateFromPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(dateFrom, fontSize = 16.sp)
                        }
                    }
                }
                
                // Выбор даты "до"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Дата до",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDateToPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(dateTo, fontSize = 16.sp)
                        }
                    }
                }
                
                // Филиал
                var departmentExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = departmentExpanded,
                    onExpandedChange = { departmentExpanded = !departmentExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDepartment ?: "Все филиалы",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Филиал") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = departmentExpanded,
                        onDismissRequest = { departmentExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Все филиалы") },
                            onClick = { onDepartmentChange(null); departmentExpanded = false }
                        )
                        departments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept) },
                                onClick = { onDepartmentChange(dept); departmentExpanded = false }
                            )
                        }
                    }
                }
                
                // Статус
                var statusExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedStatus?.let { 
                            when (it) {
                                Status.ISSUED -> "Роздан"
                                Status.IN_REFILL -> "На заправке"
                                Status.COLLECTED -> "Собран"
                                Status.RECEIVED -> "Принят"
                                Status.LOST -> "Потерян"
                                Status.WRITTEN_OFF -> "Списан"
                            }
                        } ?: "Все статусы",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Статус") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Все статусы") },
                            onClick = { onStatusChange(null); statusExpanded = false }
                        )
                        Status.values().forEach { status ->
                            DropdownMenuItem(
                                text = { 
                                    Text(when (status) {
                                        Status.ISSUED -> "Роздан"
                                        Status.IN_REFILL -> "На заправке"
                                        Status.COLLECTED -> "Собран"
                                        Status.RECEIVED -> "Принят"
                                        Status.LOST -> "Потерян"
                                        Status.WRITTEN_OFF -> "Списан"
                                    })
                                },
                                onClick = { onStatusChange(status); statusExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Применить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
    
    // DatePicker для даты "от"
    if (showDateFromPicker) {
        DatePickerDialog(
            initialDate = LocalDate.parse(dateFrom, formatter),
            onDateSelected = { date ->
                onDateFromChange(date.format(formatter))
                showDateFromPicker = false
            },
            onDismiss = { showDateFromPicker = false }
        )
    }
    
    // DatePicker для даты "до"
    if (showDateToPicker) {
        DatePickerDialog(
            initialDate = LocalDate.parse(dateTo, formatter),
            onDateSelected = { date ->
                onDateToChange(date.format(formatter))
                showDateToPicker = false
            },
            onDismiss = { showDateToPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите дату", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    onDateSelected(selectedDate)
                }
            }) { 
                Text("Выбрать") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Отмена") 
            }
        }
    )
}




