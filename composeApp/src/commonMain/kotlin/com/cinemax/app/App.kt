package com.cinemax.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF090B12)
private val Panel = Color(0xFF171B28)
private val Accent = Color(0xFF8B5CF6)

@Composable
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink, surface = Panel, primary = Accent,
            onBackground = Color.White, onSurface = Color.White
        )
    ) {
        Column(Modifier.fillMaxSize().background(Ink).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CINEMAX", fontSize = 25.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.weight(1f))
                Text("AR  /  EN", color = Color.LightGray, fontSize = 12.sp)
            }
            Spacer(Modifier.height(28.dp))
            Text("ليلتك تبدأ هنا", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("اكتشف أفلامك ومسلسلاتك من إضافاتك المفضلة", color = Color.LightGray)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = true, onClick = {}, label = { Text("الكل") })
                FilterChip(selected = false, onClick = {}, label = { Text("أفلام") })
                FilterChip(selected = false, onClick = {}, label = { Text("مسلسلات") })
            }
            Spacer(Modifier.height(18.dp))
            Text("ابدأ بإضافة مصادرك", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("مدير الإضافات", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("أضف رابط manifest لإضافة متوافقة مع بروتوكول Stremio.", color = Color.LightGray)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("إضافة مصدر") }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("قريبًا في CINEMAX", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(3) { i ->
                    Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(listOf("كتالوجات موحّدة", "متابعة المشاهدة", "مشغل وترجمات")[i], color = Color.White)
                            Spacer(Modifier.weight(1f))
                            Text("قيد التطوير", color = Accent, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
