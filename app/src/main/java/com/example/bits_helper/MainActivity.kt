package com.example.bits_helper   // ← замени на свой namespace

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
        Scaffold(
            containerColor = Color(0xFFF5F6F7),
            topBar = { HeaderBar() },      // закреплённая шапка
            bottomBar = { BottomBar() }    // две одинаковые по стилю кнопки
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 12.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(demoItems()) { item ->
                    CartridgeCard(item, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/* =================== HEADER (закреплённый) =================== */

@Composable
fun HeaderBar() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F6F7))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillStat("В работе: 9", bg = 0xFFE5F8E9, dot = 0xFF16A34A)
            Spacer(Modifier.width(10.dp))
            PillNeutral("Роздан: 2")
            Spacer(Modifier.weight(1f))
            SummaryPill("Всего: 20")
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillStat("Собран на заправку: 3", bg = 0xFFFFF5CC, dot = 0xFFEAB308)
            Spacer(Modifier.width(10.dp))
            PillStat("На заправке: 1", bg = 0xFFFFE4E6, dot = 0xFFEF4444)
        }
    }
}

/* =================== КНОПКИ СНИЗУ =================== */

@Composable
fun BottomBar() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .systemBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filled = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFFEDE9FE)   // мягкая сиреневая заливка
        )

        // «+»
        FilledTonalButton(
            onClick = { /* TODO */ },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.weight(1f).height(56.dp),
            contentPadding = PaddingValues(0.dp),
            colors = filled
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Добавить", modifier = Modifier.size(28.dp))
        }

        // «Штрихкод» — тот же стиль, только с текстом
        FilledTonalButton(
            onClick = { /* TODO */ },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.weight(1f).height(56.dp),
            colors = filled
        ) {
            Text("Штрихкод", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/* =================== ЭЛЕМЕНТЫ UI =================== */

data class Cartridge(val number: String, val room: String, val model: String, val date: String)

@Composable
fun PillStat(text: String, bg: Long, dot: Long) {
    Row(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(bg))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(dot)))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = Color(0xFF0F172A))
    }
}

@Composable
fun PillNeutral(text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFEFF4FB))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF6B7280)))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = Color(0xFF334155))
    }
}

@Composable
fun SummaryPill(text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF0F172A)) }
}

@Composable
fun CartridgeCard(item: Cartridge, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE5F8E9))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF16A34A)))
                    Spacer(Modifier.width(8.dp))
                    Text("В работе", fontSize = 14.sp, color = Color(0xFF0F172A))
                }
                Spacer(Modifier.width(12.dp))
                Text(item.number, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF0F172A))
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
                Text("Кабинет: ", color = Color(0xFF64748B), fontSize = 16.sp)
                Text(
                    item.room,
                    color = Color(0xFF0F172A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            InfoRow("Модель:", item.model)
            Spacer(Modifier.height(6.dp))
            InfoRow("Дата:", item.date)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFE2E8F0)))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color(0xFF64748B), fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(value, color = Color(0xFF0F172A), fontSize = 16.sp)
    }
}

/* =================== DEMO DATA =================== */

private fun demoItems(): List<Cartridge> = List(12) {
    Cartridge(number = "11817", room = "5", model = "6115", date = "2024-09-25")
}
