package com.example.bits_helper

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.offset
import kotlin.math.abs
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
    var roomNumberFilter by remember { mutableStateOf("") }
    var cartridgeNumberFilter by remember { mutableStateOf("") }
    
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
        status = selectedStatus,
        roomNumber = roomNumberFilter.takeIf { it.isNotBlank() },
        cartridgeNumber = cartridgeNumberFilter.takeIf { it.isNotBlank() }
    ).collectAsState(initial = emptyList()).value
    
    // Обработчик системной кнопки "Назад"
    BackHandler {
        onBack()
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
            // Современный заголовок
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Анализ данных по картриджам",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Современный переключатель видов
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { viewType = StatisticsViewType.CARDS },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewType == StatisticsViewType.CARDS) 
                                    MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Карточки",
                                color = if (viewType == StatisticsViewType.CARDS) 
                                    MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                        Button(
                            onClick = { viewType = StatisticsViewType.MODELS },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewType == StatisticsViewType.MODELS) 
                                    MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Модели",
                                color = if (viewType == StatisticsViewType.MODELS) 
                                    MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                roomNumberFilter = roomNumberFilter,
                cartridgeNumberFilter = cartridgeNumberFilter,
                departments = departments,
                formatter = formatter,
                onDateFromChange = { dateFrom = it },
                onDateToChange = { dateTo = it },
                onDepartmentChange = { selectedDepartment = it },
                onStatusChange = { selectedStatus = it },
                onRoomNumberChange = { roomNumberFilter = it },
                onCartridgeNumberChange = { cartridgeNumberFilter = it },
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            // Дата и филиал справа
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = date,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!department.isNullOrBlank()) {
                    Text(
                        text = department,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    roomNumberFilter: String,
    cartridgeNumberFilter: String,
    departments: List<String>,
    formatter: DateTimeFormatter,
    onDateFromChange: (String) -> Unit,
    onDateToChange: (String) -> Unit,
    onDepartmentChange: (String?) -> Unit,
    onStatusChange: (Status?) -> Unit,
    onRoomNumberChange: (String) -> Unit,
    onCartridgeNumberChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showDateFromPicker by remember { mutableStateOf(false) }
    var showDateToPicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Фильтры статистики", fontWeight = FontWeight.SemiBold) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Выбор даты "от"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Дата от",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Дата до",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
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
                
                // Фильтр по номеру кабинета
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Номер кабинета",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = roomNumberFilter,
                            onValueChange = onRoomNumberChange,
                            placeholder = { Text("Все", fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                        )
                    }
                }
                
                // Фильтр по номеру картриджа
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Номер картриджа",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = cartridgeNumberFilter,
                            onValueChange = onCartridgeNumberChange,
                            placeholder = { Text("Все", fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                        )
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

@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDay by remember { mutableStateOf(initialDate.dayOfMonth) }
    var selectedMonth by remember { mutableStateOf(initialDate.monthValue) }
    var selectedYear by remember { mutableStateOf(initialDate.year) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите дату", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Анимированный выбор даты
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // День
                    AnimatedDateWheel(
                        label = "День",
                        items = (1..31).toList(),
                        selectedValue = selectedDay,
                        onValueChanged = { selectedDay = it },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Месяц
                    AnimatedDateWheel(
                        label = "Месяц",
                        items = (1..12).toList(),
                        selectedValue = selectedMonth,
                        onValueChanged = { selectedMonth = it },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Год
                    AnimatedDateWheel(
                        label = "Год",
                        items = (2020..2030).toList(),
                        selectedValue = selectedYear,
                        onValueChanged = { selectedYear = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Быстрый выбор дат
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Быстрый выбор:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val today = LocalDate.now()
                                    selectedDay = today.dayOfMonth
                                    selectedMonth = today.monthValue
                                    selectedYear = today.year
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                Text("Сегодня", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val yesterday = LocalDate.now().minusDays(1)
                                    selectedDay = yesterday.dayOfMonth
                                    selectedMonth = yesterday.monthValue
                                    selectedYear = yesterday.year
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                Text("Вчера", fontSize = 12.sp)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val weekAgo = LocalDate.now().minusWeeks(1)
                                    selectedDay = weekAgo.dayOfMonth
                                    selectedMonth = weekAgo.monthValue
                                    selectedYear = weekAgo.year
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                Text("Неделю назад", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val monthAgo = LocalDate.now().minusMonths(1)
                                    selectedDay = monthAgo.dayOfMonth
                                    selectedMonth = monthAgo.monthValue
                                    selectedYear = monthAgo.year
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                Text("Месяц назад", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    try {
                        val selectedDate = LocalDate.of(selectedYear, selectedMonth, selectedDay)
                        onDateSelected(selectedDate)
                    } catch (e: Exception) {
                        // Обработка ошибок валидации даты
                    }
                }
            ) { 
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

@Composable
fun AnimatedDateWheel(
    label: String,
    items: List<Int>,
    selectedValue: Int,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var offset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val itemHeight = 50.dp
    val visibleItems = 3
    val centerIndex = visibleItems / 2
    
    // Вычисляем начальное смещение для выбранного элемента
    val initialOffset = remember(selectedValue) {
        val selectedIndex = items.indexOf(selectedValue)
        if (selectedIndex != -1) {
            -(selectedIndex - centerIndex) * with(density) { itemHeight.toPx() }
        } else 0f
    }
    
    // Обновляем offset при изменении selectedValue
    LaunchedEffect(selectedValue) {
        val selectedIndex = items.indexOf(selectedValue)
        if (selectedIndex != -1) {
            val targetOffset = -(selectedIndex - centerIndex) * with(density) { itemHeight.toPx() }
            offset = targetOffset
        }
    }
    
    // Анимируем к ближайшему элементу при отпускании
    LaunchedEffect(isDragging) {
        if (!isDragging) {
            val newIndex = (-offset / with(density) { itemHeight.toPx() } + centerIndex).toInt()
            val clampedIndex = newIndex.coerceIn(0, items.size - 1)
            val targetOffset = -(clampedIndex - centerIndex) * with(density) { itemHeight.toPx() }
            
            // Плавная анимация к целевому элементу
            offset = targetOffset
            
            if (items[clampedIndex] != selectedValue) {
                onValueChanged(items[clampedIndex])
            }
        }
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .height(itemHeight * visibleItems)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false }
                    ) { _, dragAmount ->
                        offset += dragAmount.y
                    }
                }
        ) {
            // Рендерим элементы
            items.forEachIndexed { index, item ->
                val itemOffset = index * with(density) { itemHeight.toPx() } + offset
                val centerY = with(density) { (itemHeight * centerIndex).toPx() }
                val distanceFromCenter = kotlin.math.abs(itemOffset - centerY)
                val maxDistance = with(density) { itemHeight.toPx() }
                
                // Вычисляем прозрачность и масштаб на основе расстояния от центра
                val alpha = (1f - (distanceFromCenter / maxDistance)).coerceIn(0.3f, 1f)
                val scale = (1f - (distanceFromCenter / maxDistance) * 0.3f).coerceIn(0.7f, 1f)
                
                // Анимация для плавного перехода
                val animatedAlpha by animateFloatAsState(
                    targetValue = alpha,
                    animationSpec = tween(150),
                    label = "alpha"
                )
                val animatedScale by animateFloatAsState(
                    targetValue = scale,
                    animationSpec = tween(150),
                    label = "scale"
                )
                
                // Определяем, является ли элемент центральным
                val isCenter = distanceFromCenter < with(density) { itemHeight.toPx() / 2 }
                
                Box(
                    modifier = Modifier
                        .offset(y = with(density) { itemOffset.toDp() })
                        .fillMaxWidth()
                        .height(itemHeight)
                        .alpha(animatedAlpha)
                        .scale(animatedScale)
                        .clickable { 
                            // Клик по элементу - переходим к нему
                            val targetOffset = -(index - centerIndex) * with(density) { itemHeight.toPx() }
                            offset = targetOffset
                            onValueChanged(item)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.toString().padStart(2, '0'),
                        fontSize = if (isCenter) 18.sp else 16.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCenter) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Центральная линия выделения
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .offset(y = itemHeight * centerIndex)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

