package com.example.bits_helper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F6F7)) {
            CartridgesScreen(
                header = HeaderStats(
                    inWork = 9,
                    given = 2,
                    gathered = 3,
                    filling = 1,
                    total = 20
                ),
                items = demoItems(),
                onAdd = { /* TODO: открыть форму добавления */ },
                onScan = { /* TODO: открыть сканер штрихкода */ }
            )
        }
    }
}

// ---------- UI ----------

data class HeaderStats(
    val inWork: Int,
    val given: Int,
    val gathered: Int,
    val filling: Int,
    val total: Int
)

data class Cartridge(
    val status: Status,
    val number: String,
    val room: String,
    val model: String,
    val date: String
)

enum class Status { InWork, Given, Gathered, Filling }

@Composable
fun CartridgesScreen(
    header: HeaderStats,
    items: List<Cartridge>,
    onAdd: () -> Unit,
    onScan: () -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomActionBar(onAdd = onAdd, onScan = onScan)
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Spacer(Modifier.height(8.dp))
            // верхние чипы
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillStat(text = "В работе: ${header.inWork}", bg = Color(0xFFE5F8E9), dot = Color(0xFF16A34A))
                Spacer(Modifier.width(12.dp))
                PillDotStat(text = "Роздан: ${header.given}", dot = Color(0xFF6B7280))
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillStat(text = "Собран на заправку: ${header.gathered}", bg = Color(0xFFFFF5CC), dot = Color(0xFFEAB308))
                Spacer(Modifier.width(12.dp))
                PillStat(text = "На заправке: ${header.filling}", bg = Color(0xFFFFE4E6), dot = Color(0xFFEF4444))
                Spacer(Modifier.weight(1f))
                SummaryPill(text = "Всего: ${header.total}")
            }
            Spacer(Modifier.height(8.dp))
            // список карточек
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
            ) {
                items(items) { item ->
                    CartridgeCard(item = item)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun PillStat(text: String, bg: Color, dot: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 15.sp, color = Color(0xFF0F172A))
    }
}

@Composable
fun PillDotStat(text: String, dot: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 15.sp, color = Color(0xFF334155))
    }
}

@Composable
fun SummaryPill(text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF0F172A))
    }
}

@Composable
fun CartridgeCard(item: Cartridge) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            // строка статуса + номер + кабинет
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillStat(
                    text = when (item.status) {
                        Status.InWork -> "В работе"
                        Status.Given -> "Роздан"
                        Status.Gathered -> "Собран"
                        Status.Filling -> "На заправке"
                    },
                    bg = Color(0xFFE5F8E9),
                    dot = Color(0xFF16A34A)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    item.number,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color(0xFF0F172A)
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF64748B)
                )
                Text(
                    "Кабинет: ",
                    color = Color(0xFF64748B),
                    fontSize = 16.sp
                )
                Text(
                    item.room,
                    color = Color(0xFF0F172A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            InfoRow(label = "Модель:", value = item.model)
            Spacer(Modifier.height(6.dp))
            InfoRow(label = "Дата:", value = item.date)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // маленький икон-placeholder слева (как на скриншоте)
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE2E8F0))
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color(0xFF64748B), fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(value, color = Color(0xFF0F172A), fontSize = 16.sp)
    }
}

@Composable
fun BottomActionBar(onAdd: () -> Unit, onScan: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedButton(
            onClick = onAdd,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Добавить картридж", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onScan,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Штрихкод", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------- demo data ----------

private fun demoItems(): List<Cartridge> = List(5) {
    Cartridge(
        status = Status.InWork,
        number = "11817",
        room = "5",
        model = "6115",
        date = "2024-09-25"
    )
}
