package com.cinemax.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cinemax.app.addons.AddonSettingsStore
import com.cinemax.app.addons.InstalledAddon
import com.cinemax.app.addons.AddonRepository
import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.addons.StremioStream
import com.cinemax.app.addons.createCinemaxHttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val Ink = Color(0xFF090B12)
private val Panel = Color(0xFF171B28)
private val Accent = Color(0xFF8B5CF6)

@Composable
fun App() {
    val http = remember { createCinemaxHttpClient() }
    val repository = remember { AddonRepository(http) }
    val addonStore = remember { AddonSettingsStore() }
    val preferencesStore = remember { CinemaxPreferencesStore() }
    val favoritesStore = remember { FavoritesStore() }
    val favoriteItems = remember { mutableStateListOf<com.cinemax.app.addons.StremioMetaPreview>().apply { addAll(favoritesStore.load()) } }
    val favoriteIds = favoriteItems.map { it.id }.toSet()
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var preferences by remember { mutableStateOf(preferencesStore.load()) }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val addons = remember { mutableStateListOf<InstalledAddon>().apply { addAll(addonStore.load()) } }
    val catalogItems = remember { mutableStateListOf<StremioMetaPreview>() }
    val availableCatalogs = remember { mutableStateListOf<com.cinemax.app.addons.StremioCatalog>() }
    val streams = remember { mutableStateListOf<StremioStream>() }
    var activeAddon by remember { mutableStateOf(addons.lastOrNull()) }
    var selectedItem by remember { mutableStateOf<StremioMetaPreview?>(null) }
    var selectedMeta by remember { mutableStateOf<com.cinemax.app.addons.StremioMeta?>(null) }
    var activeCatalogType by remember { mutableStateOf<String?>(null) }
    var activeCatalogId by remember { mutableStateOf<String?>(null) }
    var activeCatalogSupportsSearch by remember { mutableStateOf(false) }
    var activeCatalogSupportsSkip by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addonUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("أضف إضافة Stremio متوافقة للبدء") }
    var loading by remember { mutableStateOf(false) }
    var loadingStreams by remember { mutableStateOf(false) }
    var playbackUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedItem?.id, activeAddon?.id) {
        val item = selectedItem
        val addon = activeAddon
        if (item != null && addon != null) {
            loadingStreams = true
            streams.clear()
            selectedMeta = null
            try {
                selectedMeta = repository.loadMeta(addon.manifestUrl, item.type, item.id)
                selectedMeta?.let { meta ->
                    val index = favoriteItems.indexOfFirst { it.id == item.id }
                    if (index >= 0) {
                        favoriteItems[index] = item.copy(
                            name = meta.name, poster = meta.poster ?: item.poster,
                            background = meta.background ?: item.background,
                            description = meta.description ?: item.description,
                            releaseInfo = meta.releaseInfo ?: item.releaseInfo,
                            imdbRating = meta.imdbRating ?: item.imdbRating
                        )
                        favoritesStore.save(favoriteItems.toList())
                    }
                }
                streams.addAll(repository.loadStreams(addon.manifestUrl, item.type, item.id))
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                status = "تعذر جلب تفاصيل العنصر أو مصادره: ${error.message ?: "خطأ غير معروف"}"
            } finally {
                loadingStreams = false
            }
        } else {
            streams.clear()
            selectedMeta = null
            loadingStreams = false
        }
    }

    LaunchedEffect(Unit) {
        val restoredAddon = activeAddon
        if (restoredAddon != null) {
            loading = true
            try {
                val manifest = repository.loadManifest(restoredAddon.manifestUrl)
                availableCatalogs.clear()
                availableCatalogs.addAll(manifest.catalogs)
                val catalog = manifest.catalogs.firstOrNull()
                if (catalog != null) {
                    activeCatalogType = catalog.type
                    activeCatalogId = catalog.id
                    activeCatalogSupportsSearch = catalog.extra.any { it.name == "search" }
                    activeCatalogSupportsSkip = catalog.extra.any { it.name == "skip" }
                    catalogItems.addAll(repository.loadCatalog(restoredAddon.manifestUrl, catalog.type, catalog.id))
                    status = "تم استعادة ${restoredAddon.name} • ${catalogItems.size} عنصرًا"
                } else {
                    status = "تم استعادة ${restoredAddon.name}؛ لا يوجد كتالوج معلن."
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                status = "تعذر استعادة الكتالوج: ${error.message ?: "خطأ غير معروف"}"
            } finally {
                loading = false
            }
        }
    }

    fun selectAddon(addon: InstalledAddon) {
        if (activeAddon?.id == addon.id) return
        activeAddon = addon
        catalogItems.clear()
        selectedItem = null
        playbackUrl = null
        streams.clear()
        searchQuery = ""
        loading = true
        scope.launch {
            try {
                val manifest = repository.loadManifest(addon.manifestUrl)
                availableCatalogs.clear()
                availableCatalogs.addAll(manifest.catalogs)
                val catalog = manifest.catalogs.firstOrNull()
                if (catalog == null) {
                    activeCatalogType = null
                    activeCatalogId = null
                    activeCatalogSupportsSearch = false
                    activeCatalogSupportsSkip = false
                    status = "تم اختيار ${addon.name}؛ لا يوجد كتالوج معلن."
                } else {
                    activeCatalogType = catalog.type
                    activeCatalogId = catalog.id
                    activeCatalogSupportsSearch = catalog.extra.any { it.name == "search" }
                    activeCatalogSupportsSkip = catalog.extra.any { it.name == "skip" }
                    val loaded = repository.loadCatalog(addon.manifestUrl, catalog.type, catalog.id)
                    catalogItems.addAll(loaded)
                    status = "المصدر النشط: ${addon.name} • ${loaded.size} عنصرًا"
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                status = "تعذر فتح ${addon.name}: ${error.message ?: "خطأ غير معروف"}"
            } finally {
                loading = false
            }
        }
    }

    val appAccent = when (preferences.accent) {
        "Blue" -> Color(0xFF22A7F0)
        "Rose" -> Color(0xFFEC4899)
        "Emerald" -> Color(0xFF10B981)
        "Amber" -> Color(0xFFF59E0B)
        else -> Accent
    }
    val appBackground = if (preferences.oledBlack) Color.Black else Ink
    MaterialTheme(colorScheme = darkColorScheme(
        background = appBackground, surface = Panel, primary = appAccent,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Column(Modifier.fillMaxSize().background(appBackground).padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CINEMAX", fontSize = 25.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.weight(1f))
                Text("AR  /  EN", color = Color.LightGray, fontSize = 12.sp)
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = { showSettings = true }) { Text("⚙", color = appAccent, fontSize = 24.sp) }
            }
            Spacer(Modifier.height(26.dp))
            Text("ليلتك تبدأ هنا", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("مكتبتك السينمائية من مصادرك المفضلة", color = Color.LightGray)
            Spacer(Modifier.height(10.dp))
            Text("أفلام  •  مسلسلات  •  اكتشف", color = appAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(20.dp))
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = activeAddon?.id == addon.id,
                                onClick = { selectAddon(addon) },
                                label = { Text("${addon.name} • ${addon.version}") }
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                addons.remove(addon)
                                addonStore.save(addons.toList())
                                if (activeAddon?.id == addon.id) {
                                    activeAddon = null
                                    catalogItems.clear()
                                    selectedItem = null
                                    activeCatalogType = null
                                    activeCatalogId = null
                                    activeCatalogSupportsSearch = false
                                    activeCatalogSupportsSkip = false
                                    addons.lastOrNull()?.let { selectAddon(it) }
                                }
                                status = "تم تحديث قائمة الإضافات"
                            }) { Text("إزالة") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("اكتشف", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                FilterChip(selected = !showFavoritesOnly, onClick = { showFavoritesOnly = false }, label = { Text("الكل") })
                Spacer(Modifier.width(6.dp))
                FilterChip(selected = showFavoritesOnly, onClick = { showFavoritesOnly = true }, label = { Text("قائمتي ★") })
            }
            if (availableCatalogs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCatalogs.forEach { catalog ->
                        FilterChip(
                            selected = activeCatalogType == catalog.type && activeCatalogId == catalog.id,
                            onClick = {
                                val addon = activeAddon ?: return@FilterChip
                                selectedItem = null
                                playbackUrl = null
                                activeCatalogType = catalog.type
                                activeCatalogId = catalog.id
                                activeCatalogSupportsSearch = catalog.extra.any { it.name == "search" }
                                activeCatalogSupportsSkip = catalog.extra.any { it.name == "skip" }
                                searchQuery = ""
                                catalogItems.clear()
                                loading = true
                                scope.launch {
                                    try {
                                        val items = repository.loadCatalog(addon.manifestUrl, catalog.type, catalog.id)
                                        catalogItems.addAll(items)
                                        status = "تم تحميل ${catalog.name ?: catalog.id} • ${items.size} عنصرًا"
                                    } catch (error: Throwable) {
                                        if (error is CancellationException) throw error
                                        status = "تعذر تحميل الكتالوج: ${error.message ?: "خطأ غير معروف"}"
                                    } finally {
                                        loading = false
                                    }
                                }
                            },
                            label = { Text(catalog.name ?: catalog.id) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (activeAddon != null && activeCatalogType != null && activeCatalogId != null && activeCatalogSupportsSearch) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("ابحث في هذا المصدر") }
                    )
                    Button(
                        enabled = searchQuery.isNotBlank() && !searching && !loading,
                        onClick = {
                            val addon = activeAddon ?: return@Button
                            val type = activeCatalogType ?: return@Button
                            val catalogId = activeCatalogId ?: return@Button
                            searching = true
                            scope.launch {
                                try {
                                    val results = repository.searchCatalog(
                                        addon.manifestUrl, type, catalogId, searchQuery
                                    )
                                    catalogItems.clear()
                                    catalogItems.addAll(results)
                                    status = "نتائج البحث: ${results.size}"
                                } catch (error: Throwable) {
                                    status = "تعذر البحث: ${error.message ?: "خطأ غير معروف"}"
                                } finally {
                                    searching = false
                                }
                            }
                        }
                    ) { Text("بحث") }
                }
                if (searching) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
            }
            val visibleItems = if (showFavoritesOnly) favoriteItems.toList() else catalogItems.toList()
            if (visibleItems.isEmpty()) {
                Text(if (showFavoritesOnly) "قائمتك فارغة حاليًا. افتح تفاصيل أي عنوان وأضفه إلى قائمتك." else "ستظهر عناصر الكتالوج هنا بعد تثبيت إضافة توفر كتالوجًا.", color = Color.LightGray, fontSize = 13.sp)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedItem = item
                            },
                            colors = CardDefaults.cardColors(containerColor = Panel)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(if (preferences.compactCards) 8.dp else 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AsyncImage(
                                    model = item.poster,
                                    contentDescription = "ملصق ${item.name}",
                                    modifier = Modifier.width(if (preferences.compactCards) 64.dp else 88.dp)
                                        .height(if (preferences.compactCards) 94.dp else 128.dp)
                                        .background(Color(0xFF25283A), RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(Modifier.weight(1f).align(Alignment.CenterVertically)) {
                                    Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    item.releaseInfo?.let { Text(it, color = Color.LightGray, fontSize = 12.sp) }
                                    item.imdbRating?.let { Text("★ $it", color = Color(0xFFFFC857), fontSize = 12.sp) }
                                    item.description?.takeIf { it.isNotBlank() }?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text(it, color = Color.LightGray, fontSize = 12.sp, maxLines = 3)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text("اضغط لعرض التفاصيل والمصادر", color = appAccent, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    if (activeCatalogSupportsSkip) {
                        item {
                            Button(
                                enabled = !loadingMore && !loading && catalogItems.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val addon = activeAddon ?: return@Button
                                    val type = activeCatalogType ?: return@Button
                                    val catalogId = activeCatalogId ?: return@Button
                                    loadingMore = true
                                    scope.launch {
                                        try {
                                            val nextPage = if (searchQuery.isNotBlank() && activeCatalogSupportsSearch) {
                                                repository.searchCatalog(
                                                    addon.manifestUrl, type, catalogId,
                                                    searchQuery, skip = catalogItems.size
                                                )
                                            } else {
                                                repository.loadCatalog(
                                                    addon.manifestUrl, type, catalogId,
                                                    extra = mapOf("skip" to catalogItems.size.toString())
                                                )
                                            }
                                            catalogItems.addAll(nextPage)
                                            status = "تم تحميل ${nextPage.size} عنصر إضافي"
                                        } catch (error: Throwable) {
                                            status = "تعذر تحميل المزيد: ${error.message ?: "خطأ غير معروف"}"
                                        } finally {
                                            loadingMore = false
                                        }
                                    }
                                }
                            ) {
                                if (loadingMore) CircularProgressIndicator()
                                else Text("تحميل المزيد")
                            }
                        }
                    }
                }
            }
        }


        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Column { Text("إعدادات CINEMAX", fontWeight = FontWeight.Bold); Text("خصص تجربة المشاهدة كما تحب", fontSize = 12.sp, color = Color.Gray) } },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("المظهر", color = appAccent, fontWeight = FontWeight.Bold)
                        Text("لون التطبيق", fontSize = 13.sp)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Violet", "Blue", "Rose", "Emerald", "Amber").forEach { tone ->
                                FilterChip(selected = preferences.accent == tone, onClick = { preferences = preferences.copy(accent = tone); preferencesStore.save(preferences) },
                                    label = { Text(when(tone) { "Violet" -> "بنفسجي"; "Blue" -> "أزرق"; "Rose" -> "وردي"; "Emerald" -> "أخضر"; else -> "ذهبي" }) })
                            }
                        }
                        PreferenceSwitch("خلفية سوداء OLED", preferences.oledBlack) { preferences = preferences.copy(oledBlack = it); preferencesStore.save(preferences) }
                        PreferenceSwitch("بطاقات أكثر اختصارًا", preferences.compactCards) { preferences = preferences.copy(compactCards = it); preferencesStore.save(preferences) }
                        Divider()
                        Text("التشغيل", color = appAccent, fontWeight = FontWeight.Bold)
                        PreferenceSwitch("تذكر موضع التوقف", preferences.rememberPlaybackPosition) { preferences = preferences.copy(rememberPlaybackPosition = it); preferencesStore.save(preferences) }
                        PreferenceSwitch("تشغيل الحلقة التالية تلقائيًا", preferences.autoplayNextEpisode) { preferences = preferences.copy(autoplayNextEpisode = it); preferencesStore.save(preferences) }
                        Divider()
                        Text("الترجمة والصوت", color = appAccent, fontWeight = FontWeight.Bold)
                        Text("لغة الترجمة المفضلة", fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = preferences.preferredSubtitleLanguage == "ar", onClick = { preferences = preferences.copy(preferredSubtitleLanguage = "ar"); preferencesStore.save(preferences) }, label = { Text("العربية") })
                            FilterChip(selected = preferences.preferredSubtitleLanguage == "en", onClick = { preferences = preferences.copy(preferredSubtitleLanguage = "en"); preferencesStore.save(preferences) }, label = { Text("English") })
                            FilterChip(selected = preferences.preferredSubtitleLanguage == "off", onClick = { preferences = preferences.copy(preferredSubtitleLanguage = "off"); preferencesStore.save(preferences) }, label = { Text("إيقاف") })
                        }
                        Text("لغة الصوت المفضلة", fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = preferences.preferredAudioLanguage == "device", onClick = { preferences = preferences.copy(preferredAudioLanguage = "device"); preferencesStore.save(preferences) }, label = { Text("لغة الجهاز") })
                            FilterChip(selected = preferences.preferredAudioLanguage == "ar", onClick = { preferences = preferences.copy(preferredAudioLanguage = "ar"); preferencesStore.save(preferences) }, label = { Text("العربية") })
                            FilterChip(selected = preferences.preferredAudioLanguage == "en", onClick = { preferences = preferences.copy(preferredAudioLanguage = "en"); preferencesStore.save(preferences) }, label = { Text("English") })
                        }
                        Divider()
                        Text("الإضافات", color = appAccent, fontWeight = FontWeight.Bold)
                        Text("إدارة المصادر من بطاقة «مصادرك وإضافاتك» في الرئيسية. تشغيل شيفرات الإضافات غير مفعّل حفاظًا على الأمان.", fontSize = 12.sp, color = Color.Gray)
                        Text("CINEMAX • إصدار تجريبي", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                confirmButton = { TextButton(onClick = { showSettings = false }) { Text("تم") } }
            )
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
                                availableCatalogs.clear()
                                availableCatalogs.addAll(manifest.catalogs)
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
                                selectedItem = null
                                playbackUrl = null
                                streams.clear()
                                searchQuery = ""
                                catalogItems.clear()
                                val catalog = manifest.catalogs.firstOrNull()
                                if (catalog != null) {
                                    activeCatalogType = catalog.type
                                    activeCatalogId = catalog.id
                                    activeCatalogSupportsSearch = catalog.extra.any { it.name == "search" }
                                    activeCatalogSupportsSkip = catalog.extra.any { it.name == "skip" }
                                    val loaded = repository.loadCatalog(normalized, catalog.type, catalog.id)
                                    catalogItems.addAll(loaded)
                                    status = "تم تثبيت ${manifest.name} وجلب ${loaded.size} عنصرًا"
                                } else {
                                    activeCatalogType = null
                                    activeCatalogId = null
                                    activeCatalogSupportsSearch = false
                                    activeCatalogSupportsSkip = false
                                    status = "تم تثبيت ${manifest.name}، ولا تعلن الإضافة عن كتالوج."
                                }
                                addonUrl = ""
                            } catch (error: Throwable) {
                                if (error is CancellationException) throw error
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

        selectedItem?.let { selected ->
            Dialog(onDismissRequest = { selectedItem = null }) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 680.dp)
                        .background(Color(0xFF10131D), RoundedCornerShape(22.dp))
                        .verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = selectedMeta?.background ?: selected.background ?: selected.poster,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(190.dp)
                            .background(Panel, RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(selectedMeta?.name ?: selected.name, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        (selectedMeta?.releaseInfo ?: selected.releaseInfo)?.let { Text(it, color = Color.LightGray, fontSize = 12.sp) }
                        (selectedMeta?.imdbRating ?: selected.imdbRating)?.let { Text("★ $it", color = Color(0xFFFFC857), fontSize = 12.sp) }
                        selectedMeta?.runtime?.let { Text(it, color = Color.LightGray, fontSize = 12.sp) }
                    }
                    val isFavorite = selected.id in favoriteIds
                    OutlinedButton(onClick = {
                        if (isFavorite) {
                            favoriteItems.removeAll { it.id == selected.id }
                        } else {
                            val meta = selectedMeta
                            val saved = selected.copy(
                                name = meta?.name ?: selected.name,
                                poster = meta?.poster ?: selected.poster,
                                background = meta?.background ?: selected.background,
                                description = meta?.description ?: selected.description,
                                releaseInfo = meta?.releaseInfo ?: selected.releaseInfo,
                                imdbRating = meta?.imdbRating ?: selected.imdbRating
                            )
                            favoriteItems.removeAll { it.id == selected.id }
                            favoriteItems.add(saved)
                        }
                        favoritesStore.save(favoriteItems.toList())
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isFavorite) "✓ تمت الإضافة إلى قائمتي — إزالة" else "＋ أضف إلى قائمتي")
                    }
                    selectedMeta?.genres?.takeIf { it.isNotEmpty() }?.let {
                        Text(it.joinToString("  •  "), color = appAccent, fontSize = 12.sp)
                    }
                    (selectedMeta?.description ?: selected.description)?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color(0xFFE0E3EB), fontSize = 14.sp, lineHeight = 21.sp)
                    }
                    selectedMeta?.director?.takeIf { it.isNotEmpty() }?.let {
                        Text("إخراج: ${it.joinToString("، ")}", color = Color.LightGray, fontSize = 12.sp)
                    }
                    selectedMeta?.cast?.takeIf { it.isNotEmpty() }?.let {
                        Text("طاقم التمثيل: ${it.take(8).joinToString("، ")}", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Divider(color = Color.DarkGray)
                    Text("مصادر المشاهدة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    if (loadingStreams) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("جارٍ جلب المصادر...", color = Color.LightGray)
                    } else if (streams.isEmpty()) {
                        Text("لم تُرجع الإضافة مصادر لهذا العنوان.", color = Color.LightGray, fontSize = 13.sp)
                    } else {
                        streams.forEachIndexed { index, stream ->
                            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("${index + 1}. ${stream.name ?: stream.title ?: "مصدر"}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    stream.title?.let { Text(it, color = Color.LightGray, fontSize = 12.sp) }
                                    stream.url?.let { mediaUrl ->
                                        Button(onClick = {
                                            if (com.cinemax.app.addons.StremioAddonClient.isSafeMediaUrl(mediaUrl)) playbackUrl = mediaUrl
                                            else status = "تم رفض رابط تشغيل غير آمن. يجب أن يستخدم HTTPS."
                                        }, modifier = Modifier.fillMaxWidth()) { Text("▶ تشغيل") }
                                    } ?: Text("هذا المصدر لا يحتوي على رابط مباشر قابل للتشغيل هنا.", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    TextButton(onClick = { selectedItem = null }, modifier = Modifier.align(Alignment.End)) { Text("إغلاق") }
                }
            }
        }
        playbackUrl?.let { mediaUrl ->
            Dialog(onDismissRequest = { playbackUrl = null }) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("مشغل CINEMAX", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { playbackUrl = null }) { Text("إغلاق") }
                    }
                    NativeVideoPlayer(
                        url = mediaUrl,
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
