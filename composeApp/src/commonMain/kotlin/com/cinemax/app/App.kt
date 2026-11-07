package com.cinemax.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinemax.app.addons.InstalledAddon
import com.cinemax.app.addons.AddonRepository
import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.addons.StremioStream
import com.cinemax.app.addons.createCinemaxHttpClient
import kotlinx.coroutines.launch

private val Ink = Color(0xFF090B12)
private val Panel = Color(0xFF171B28)
private val Accent = Color(0xFF8B5CF6)

@Composable
fun App() {
    val http = remember { createCinemaxHttpClient() }
    val repository = remember { AddonRepository(http) }
    val addonStore = remember { AddonSettingsStore() }
    val scope = rememberCoroutineScope()
    val addons = remember { mutableStateListOf<InstalledAddon>().apply { addAll(addonStore.load()) } }
    val catalogItems = remember { mutableStateListOf<StremioMetaPreview>() }
    val streams = remember { mutableStateListOf<StremioStream>() }
    var activeAddon by remember { mutableStateOf(addons.lastOrNull()) }
    var selectedItem by remember { mutableStateOf<StremioMetaPreview?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addonUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("أضف إضافة Stremio متوافقة للبدء") }
    var loading by remember { mutableStateOf(false) }
    var loadingStreams by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Accent,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Column(Modifier.fillMaxSize().background(Ink).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CINEMAX", fontSize = 25.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.weight(1f))
                Text("AR  /  EN", color = Color.LightGray, fontSize = 12.sp)
            }
            Spacer(Modifier.height(26.dp))
            Text("ليلتك تبدأ هنا", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("مكتبتك السينمائية من مصادرك المفضلة", color = Color.LightGray)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = true, onClick = {}, label = { Text("الكل") })
                FilterChip(selected = false, onClick = {}, label = { Text("أفلام") })
                FilterChip(selected = false, onClick = {}, label = { Text("مسلسلات") })
            }
            Spacer(Modifier.height(18.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("مصادرك وإضافاتك", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { showAddDialog = true }) { Text("＋ إضافة") }
                    }
                    Text(status, color = Color.LightGray, fontSize = 13.sp)
                    if (loading) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    addons.forEach { addon ->
                        Spacer(Modifier.height(8.dp))
                        Text("✓ ${addon.name}  •  ${addon.version}", color = Color(0xFFB8A2FF))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("اكتشف", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (catalogItems.isEmpty()) {
                Text("ستظهر عناصر الكتالوج هنا بعد تثبيت إضافة توفر كتالوجًا.", color = Color.LightGray, fontSize = 13.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(catalogItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedItem = item
                                streams.clear()
                                val addon = activeAddon
                                if (addon != null) {
                                    loadingStreams = true
                                    scope.launch {
                                        try {
                                            streams.addAll(repository.loadStreams(addon.manifestUrl, item.type, item.id))
                                        } catch (error: Throwable) {
                                            status = "تعذر جلب المصادر: ${error.message ?: "خطأ غير معروف"}"
                                        } finally {
                                            loadingStreams = false
                                        }
                                    }
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = Panel)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                item.releaseInfo?.let { Text(it, color = Color.LightGray, fontSize = 12.sp) }
                                item.description?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(Modifier.height(4.dp))
                                    Text(it, color = Color.LightGray, fontSize = 12.sp, maxLines = 3)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("اضغط لعرض المصادر", color = Accent, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("إضافة مصدر") },
                text = {
                    Column {
                        Text("الصق رابط الإضافة أو رابط manifest.json", fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = addonUrl, onValueChange = { addonUrl = it },
                            singleLine = true, placeholder = { Text("https://example.com/manifest.json") })
                    }
                },
                confirmButton = {
                    Button(enabled = addonUrl.isNotBlank() && !loading, onClick = {
                        val input = addonUrl.trim()
                        showAddDialog = false
                        loading = true
                        status = "جارٍ التحقق من الإضافة..."
                        scope.launch {
                            try {
                                val manifest = repository.loadManifest(input)
                                val normalized = com.cinemax.app.addons.StremioAddonClient.manifestUrl(input)!!
                                val installed = InstalledAddon(
                                    manifestUrl = normalized, id = manifest.id, name = manifest.name,
                                    version = manifest.version, description = manifest.description,
                                    installedAtEpochMillis = 0L
                                )
                                addons.removeAll { it.id == installed.id }
                                addons.add(installed)
                                addonStore.save(addons.toList())
                                activeAddon = installed
                                catalogItems.clear()
                                val catalog = manifest.catalogs.firstOrNull()
                                if (catalog != null) {
                                    val loaded = repository.loadCatalog(normalized, catalog.type, catalog.id)
                                    catalogItems.addAll(loaded)
                                    status = "تم تثبيت ${manifest.name} وجلب ${loaded.size} عنصرًا"
                                } else {
                                    status = "تم تثبيت ${manifest.name}، ولا تعلن الإضافة عن كتالوج."
                                }
                                addonUrl = ""
                            } catch (error: Throwable) {
                                status = "تعذر تثبيت الإضافة: ${error.message ?: "خطأ غير معروف"}"
                            } finally {
                                loading = false
                            }
                        }
                    }) { Text("تحقق وأضف") }
                },
                dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
            )
        }

        if (selectedItem != null) {
            AlertDialog(
                onDismissRequest = { selectedItem = null },
                title = { Text(selectedItem?.name ?: "مصادر المشاهدة") },
                text = {
                    Column {
                        if (loadingStreams) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text("جارٍ جلب المصادر...", modifier = Modifier.padding(top = 8.dp))
                        } else if (streams.isEmpty()) {
                            Text("لم تُرجع الإضافة مصادر لهذا العنصر.")
                        } else {
                            Text("المصادر التي أعادتها الإضافة:", fontSize = 13.sp)
                            streams.forEachIndexed { index, stream ->
                                Spacer(Modifier.height(8.dp))
                                Text("${index + 1}. ${stream.name ?: stream.title ?: "مصدر"}", fontWeight = FontWeight.SemiBold)
                                stream.title?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
                                stream.url?.let { Text(it, fontSize = 10.sp, color = Color.Gray, maxLines = 2) }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { selectedItem = null }) { Text("إغلاق") } }
            )
        }
    }
}
