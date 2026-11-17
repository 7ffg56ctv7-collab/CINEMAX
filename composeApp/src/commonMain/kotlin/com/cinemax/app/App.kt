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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import com.cinemax.app.core.MediaIdentity
import com.cinemax.app.core.CatalogSort
import com.cinemax.app.core.DiscoveryPresentation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

private val Ink = Color(0xFF090B12)
private val Panel = Color(0xFF171B28)
private val Accent = Color(0xFF8B5CF6)

@Composable
fun App() {
    val http = remember { createCinemaxHttpClient() }
    DisposableEffect(http) { onDispose { http.close() } }
    val repository = remember { AddonRepository(http) }
    val addonStore = remember { AddonSettingsStore() }
    val preferencesStore = remember { CinemaxPreferencesStore() }
    val favoritesStore = remember { FavoritesStore() }
    val sourcePreferenceStore = remember { SourcePreferenceStore() }
    val favoriteItems = remember { mutableStateListOf<com.cinemax.app.addons.StremioMetaPreview>().apply { addAll(favoritesStore.load()) } }
    val favoriteIds = favoriteItems.map { MediaIdentity.storageKey(it.type, it.id, it.name, it.releaseInfo) }.toSet()
    val watchProgressStore = remember { WatchProgressStore() }
    val watchProgressItems = remember { mutableStateListOf<WatchProgress>().apply { addAll(watchProgressStore.load()) } }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var localFilterQuery by remember { mutableStateOf("") }
    var mediaTypeFilter by remember { mutableStateOf<String?>(null) }
    var catalogSort by remember { mutableStateOf(CatalogSort.Source) }
    var playableSourcesOnly by remember { mutableStateOf(false) }
    var preferences by remember { mutableStateOf(preferencesStore.load()) }
    var showSettings by remember { mutableStateOf(false) }
    var showAddonManager by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
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
    var moreCatalogPages by remember { mutableStateOf(true) }
    var nextPageOffset by remember { mutableIntStateOf(0) }
    var catalogRequestVersion by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addonUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("أضف إضافة Stremio متوافقة للبدء") }
    var loading by remember { mutableStateOf(false) }
    var loadingStreams by remember { mutableStateOf(false) }
    var streamsError by remember { mutableStateOf<String?>(null) }
    var streamsRefreshVersion by remember { mutableIntStateOf(0) }
    var playbackUrl by remember { mutableStateOf<String?>(null) }
    var playingItem by remember { mutableStateOf<StremioMetaPreview?>(null) }

    LaunchedEffect(selectedItem?.id, selectedItem?.type, selectedItem?.name, selectedItem?.releaseInfo, activeAddon?.id, activeAddon?.manifestUrl, addons.toList(), streamsRefreshVersion) {
        val item = selectedItem
        val addon = activeAddon
        if (item != null && addon != null) {
            loadingStreams = true
            streamsError = null
            streams.clear()
            selectedMeta = null
            try {
                selectedMeta = try {
                    repository.loadMeta(addon.manifestUrl, item.type, item.id)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    status = "تعذر تحميل بيانات العنوان؛ سأحاول جلب مصادر المشاهدة."
                    null
                }
                selectedMeta?.let { meta ->
                    val itemKey = MediaIdentity.storageKey(item.type, item.id, item.name, item.releaseInfo)
                    val index = favoriteItems.indexOfFirst { MediaIdentity.storageKey(it.type, it.id, it.name, it.releaseInfo) == itemKey }
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
                streams.addAll(repository.loadStreamsAcrossAddons(addons.toList(), item, addon.id))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                streamsError = "تعذر جلب مصادر المشاهدة: ${error.message ?: "خطأ غير معروف"}"
            } finally {
                loadingStreams = false
            }
        } else {
            streams.clear()
            selectedMeta = null
            streamsError = null
            loadingStreams = false
        }
    }

    LaunchedEffect(Unit) {
        val requestVersion = catalogRequestVersion
        val restoredAddon = activeAddon
        if (restoredAddon != null) {
            loading = true
            try {
                val manifest = repository.loadManifest(restoredAddon.manifestUrl)
                if (requestVersion != catalogRequestVersion) return@LaunchedEffect
                availableCatalogs.clear()
                availableCatalogs.addAll(manifest.catalogs)
                val catalog = manifest.catalogs.firstOrNull()
                if (catalog != null) {
                    activeCatalogType = catalog.type
                    activeCatalogId = catalog.id
                    activeCatalogSupportsSearch = catalog.extra.any { it.name == "search" }
                    activeCatalogSupportsSkip = catalog.extra.any { it.name == "skip" }
                    val restoredItems = repository.loadCatalog(restoredAddon.manifestUrl, catalog.type, catalog.id)
                    if (requestVersion != catalogRequestVersion) return@LaunchedEffect
                    catalogItems.clear()
                    catalogItems.addAll(restoredItems)
                    nextPageOffset = restoredItems.size
                    moreCatalogPages = restoredItems.isNotEmpty()
                    status = "تم استعادة ${restoredAddon.name} • ${catalogItems.size} عنصرًا"
                } else {
                    status = "تم استعادة ${restoredAddon.name}؛ لا يوجد كتالوج معلن."
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (requestVersion == catalogRequestVersion) status = "تعذر استعادة الكتالوج: ${error.message ?: "خطأ غير معروف"}"
            } finally {
                if (requestVersion == catalogRequestVersion) loading = false
            }
        }
    }

    fun selectAddon(addon: InstalledAddon) {
        if (activeAddon?.id == addon.id && activeAddon?.manifestUrl == addon.manifestUrl) return
        catalogRequestVersion++
        val requestVersion = catalogRequestVersion
        searching = false
        loadingMore = false
        activeAddon = addon
        activeCatalogType = null
        activeCatalogId = null
        activeCatalogSupportsSearch = false
        activeCatalogSupportsSkip = false
        selectedMeta = null
        availableCatalogs.clear()
        catalogItems.clear()
        moreCatalogPages = true
        nextPageOffset = 0
        selectedItem = null
        playbackUrl = null
        playingItem = null
        streams.clear()
        searchQuery = ""
        loading = true
        scope.launch {
            try {
                val manifest = repository.loadManifest(addon.manifestUrl)
                if (requestVersion != catalogRequestVersion) return@launch
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
                    if (requestVersion != catalogRequestVersion) return@launch
                    catalogItems.addAll(loaded)
                    nextPageOffset = loaded.size
                    moreCatalogPages = loaded.isNotEmpty()
                    status = "المصدر النشط: ${addon.name} • ${loaded.size} عنصرًا"
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (requestVersion == catalogRequestVersion) status = "تعذر فتح ${addon.name}: ${error.message ?: "خطأ غير معروف"}"
            } finally {
                if (requestVersion == catalogRequestVersion) loading = false
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
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    MaterialTheme(colorScheme = darkColorScheme(
        background = appBackground, surface = Panel, primary = appAccent,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Column(Modifier.fillMaxSize().background(appBackground).padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("RAIF", fontSize = 25.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.weight(1f))
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
                        TextButton(onClick = { showAddonManager = !showAddonManager }) { Text(if (showAddonManager) "إخفاء" else "إدارة (${addons.size})") }
                        TextButton(onClick = { showAddDialog = true }) { Text("＋ إضافة") }
                    }
                    Text(status, color = Color.LightGray, fontSize = 13.sp)
                    if (loading) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    if (showAddonManager) addons.forEach { addon ->
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = activeAddon?.id == addon.id,
                                onClick = { selectAddon(addon) },
                                label = { Text("${addon.name} • ${addon.version}") }
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                val removingActiveAddon = activeAddon?.id == addon.id && activeAddon?.manifestUrl == addon.manifestUrl
                                if (removingActiveAddon) {
                                    catalogRequestVersion++
                                    searching = false
                                    loadingMore = false
                                }
                                addons.remove(addon)
                                addonStore.save(addons.toList())
                                if (removingActiveAddon) {
                                    activeAddon = null
                                    catalogItems.clear()
                                    moreCatalogPages = false
                                    nextPageOffset = 0
                                    selectedItem = null
                                    selectedMeta = null
                                    playbackUrl = null
                                    playingItem = null
                                    availableCatalogs.clear()
                                    streams.clear()
                                    loading = false
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
                FilterChip(selected = showFavoritesOnly, onClick = { showFavoritesOnly = true }, label = { Text("قائمتي ★ (${favoriteItems.size})") })
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = mediaTypeFilter == null, onClick = { mediaTypeFilter = null }, label = { Text("كل الأنواع") })
                FilterChip(selected = mediaTypeFilter == "movie", onClick = { mediaTypeFilter = "movie" }, label = { Text("أفلام") })
                FilterChip(selected = mediaTypeFilter == "series", onClick = { mediaTypeFilter = "series" }, label = { Text("مسلسلات") })
            }
            OutlinedTextField(value = localFilterQuery, onValueChange = { localFilterQuery = it.take(100) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(if (showFavoritesOnly) "بحث داخل قائمتي" else "تصفية العناوين المعروضة") })
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = catalogSort == CatalogSort.Source, onClick = { catalogSort = CatalogSort.Source }, label = { Text("ترتيب المصدر") })
                FilterChip(selected = catalogSort == CatalogSort.Title, onClick = { catalogSort = CatalogSort.Title }, label = { Text("حسب الاسم") })
                FilterChip(selected = catalogSort == CatalogSort.Newest, onClick = { catalogSort = CatalogSort.Newest }, label = { Text("الأحدث") })
                FilterChip(selected = catalogSort == CatalogSort.Oldest, onClick = { catalogSort = CatalogSort.Oldest }, label = { Text("الأقدم") })
                FilterChip(selected = catalogSort == CatalogSort.Rating, onClick = { catalogSort = CatalogSort.Rating }, label = { Text("الأعلى تقييمًا") })
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(enabled = mediaTypeFilter != null || localFilterQuery.isNotEmpty() || catalogSort != CatalogSort.Source,
                    onClick = { mediaTypeFilter = null; localFilterQuery = ""; catalogSort = CatalogSort.Source }) { Text("مسح التصفية") }
                Spacer(Modifier.weight(1f))
                Text("النتائج معروضة أدناه", color = Color.Gray, fontSize = 11.sp)
            }
            if (watchProgressItems.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("تابع المشاهدة • ${watchProgressItems.size}", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    watchProgressItems.forEach { progress ->
                        Card(modifier = Modifier.width(142.dp).clickable { selectedItem = progress.item }, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AsyncImage(model = progress.item.poster, contentDescription = progress.item.name, modifier = Modifier.fillMaxWidth().height(82.dp).background(Color(0xFF25283A), RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(progress.item.name, color = Color.White, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            watchProgressStore.remove(progress.item)
                                            watchProgressItems.removeAll { MediaIdentity.storageKey(it.item.type, it.item.id, it.item.name, it.item.releaseInfo) == MediaIdentity.storageKey(progress.item.type, progress.item.id, progress.item.name, progress.item.releaseInfo) }
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) { Text("×", color = Color.LightGray, fontSize = 16.sp) }
                                }
                                LinearProgressIndicator(progress = { progress.progressFraction }, modifier = Modifier.fillMaxWidth())
                                Text("متبقي " + ((progress.durationMillis - progress.positionMillis).coerceAtLeast(0L) / 60_000L) + " دقيقة تقريبًا", color = Color.LightGray, fontSize = 10.sp)
                            }
                        }
                    }
                }
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
                                playingItem = null
                                streams.clear()
                                selectedMeta = null
                                activeCatalogType = catalog.type
                                activeCatalogId = catalog.id
                                activeCatalogSupportsSearch = catalog.extra.any { it.name == "search" }
                                activeCatalogSupportsSkip = catalog.extra.any { it.name == "skip" }
                                searchQuery = ""
                                catalogRequestVersion++
                                val requestVersion = catalogRequestVersion
                                searching = false
                                loadingMore = false
                                catalogItems.clear()
                                moreCatalogPages = true
                                nextPageOffset = 0
                                loading = true
                                scope.launch {
                                    try {
                                        val items = repository.loadCatalog(addon.manifestUrl, catalog.type, catalog.id)
                                        if (requestVersion != catalogRequestVersion) return@launch
                                        catalogItems.addAll(items)
                                        nextPageOffset = items.size
                                        moreCatalogPages = items.isNotEmpty()
                                        status = "تم تحميل ${catalog.name ?: catalog.id} • ${items.size} عنصرًا"
                                    } catch (error: Exception) {
                                        if (error is CancellationException) throw error
                                        if (requestVersion == catalogRequestVersion) status = "تعذر تحميل الكتالوج: ${error.message ?: "خطأ غير معروف"}"
                                    } finally {
                                        if (requestVersion == catalogRequestVersion) loading = false
                                    }
                                }
                            },
                            label = { Text(catalog.name ?: catalog.id) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (activeAddon != null && activeCatalogType != null && activeCatalogId != null && !showFavoritesOnly) {
                TextButton(enabled = !loading && !searching && !loadingMore, onClick = {
                    val addon = activeAddon ?: return@TextButton
                    val type = activeCatalogType ?: return@TextButton
                    val id = activeCatalogId ?: return@TextButton
                    catalogRequestVersion++
                    val requestVersion = catalogRequestVersion
                    searching = false
                    loadingMore = false
                    searchQuery = ""
                    loading = true
                    scope.launch {
                        try {
                            val items = repository.loadCatalog(addon.manifestUrl, type, id)
                            if (requestVersion != catalogRequestVersion) return@launch
                            catalogItems.clear()
                            catalogItems.addAll(items)
                            nextPageOffset = items.size
                            moreCatalogPages = items.isNotEmpty()
                            status = "تم تحديث الكتالوج • " + items.size + " عنصرًا"
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            if (requestVersion == catalogRequestVersion) status = "تعذر تحديث الكتالوج: " + (error.message ?: "خطأ غير معروف")
                        } finally {
                            if (requestVersion == catalogRequestVersion) loading = false
                        }
                    }
                }) { Text("↻ تحديث الكتالوج") }
            }
            if (activeAddon != null && activeCatalogType != null && activeCatalogId != null && activeCatalogSupportsSearch && !showFavoritesOnly) {
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
                            catalogRequestVersion++
                            val requestVersion = catalogRequestVersion
                            val submittedQuery = searchQuery.trim()
                            loadingMore = false
                            moreCatalogPages = true
                            nextPageOffset = 0
                            searching = true
                            scope.launch {
                                try {
                                    val results = repository.searchCatalog(
                                        addon.manifestUrl, type, catalogId, submittedQuery
                                    )
                                    if (requestVersion != catalogRequestVersion) return@launch
                                    catalogItems.clear()
                                    catalogItems.addAll(results)
                                    nextPageOffset = results.size
                                    moreCatalogPages = results.isNotEmpty()
                                    status = "نتائج البحث: ${results.size}"
                                } catch (error: Exception) {
                                    if (error is CancellationException) throw error
                                    if (requestVersion == catalogRequestVersion) status = "تعذر البحث: ${error.message ?: "خطأ غير معروف"}"
                                } finally {
                                    if (requestVersion == catalogRequestVersion) searching = false
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
            val visibleItems = DiscoveryPresentation.visible(
                if (showFavoritesOnly) favoriteItems.toList() else catalogItems.toList(),
                mediaTypeFilter, localFilterQuery, catalogSort
            )
            Text("نتائج العرض: ${visibleItems.size}", color = Color.LightGray, fontSize = 12.sp)
            if (visibleItems.isEmpty()) {
                val hasUnfilteredItems = if (showFavoritesOnly) favoriteItems.isNotEmpty() else catalogItems.isNotEmpty()
                Text(if (hasUnfilteredItems) "لا توجد نتائج تطابق التصفية الحالية." else if (showFavoritesOnly) "قائمتك فارغة حاليًا. افتح تفاصيل أي عنوان وأضفه إلى قائمتك." else "ستظهر عناصر الكتالوج هنا بعد تثبيت إضافة توفر كتالوجًا.", color = Color.LightGray, fontSize = 13.sp)
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
                    if (activeCatalogSupportsSkip && !showFavoritesOnly && moreCatalogPages) {
                        item {
                            Button(
                                enabled = !loadingMore && !loading && !searching && catalogItems.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val addon = activeAddon ?: return@Button
                                    val type = activeCatalogType ?: return@Button
                                    val catalogId = activeCatalogId ?: return@Button
                                    val requestVersion = catalogRequestVersion
                                    val nextOffset = nextPageOffset
                                    val submittedQuery = searchQuery.trim()
                                    val supportsSearch = activeCatalogSupportsSearch
                                    loadingMore = true
                                    scope.launch {
                                        try {
                                            val nextPage = if (submittedQuery.isNotBlank() && supportsSearch) {
                                                repository.searchCatalog(
                                                    addon.manifestUrl, type, catalogId,
                                                    submittedQuery, skip = nextOffset
                                                )
                                            } else {
                                                repository.loadCatalog(
                                                    addon.manifestUrl, type, catalogId,
                                                    extra = mapOf("skip" to nextOffset.toString())
                                                )
                                            }
                                            if (requestVersion != catalogRequestVersion) return@launch
                                            val existingIds = catalogItems.map { "${it.type}:${it.id}" }.toSet()
                                            val newItems = nextPage.distinctBy { "${it.type}:${it.id}" }.filterNot { "${it.type}:${it.id}" in existingIds }
                                            catalogItems.addAll(newItems)
                                            nextPageOffset = nextOffset + nextPage.size
                                            moreCatalogPages = nextPage.isNotEmpty() && newItems.isNotEmpty()
                                            status = "تم تحميل ${newItems.size} عنصر إضافي"
                                        } catch (error: Exception) {
                                            if (error is CancellationException) throw error
                                            if (requestVersion == catalogRequestVersion) status = "تعذر تحميل المزيد: ${error.message ?: "خطأ غير معروف"}"
                                        } finally {
                                            if (requestVersion == catalogRequestVersion) loadingMore = false
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
                title = { Column { Text("إعدادات RAIF", fontWeight = FontWeight.Bold); Text("خصص تجربة المشاهدة كما تحب", fontSize = 12.sp, color = Color.Gray) } },
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
                        Divider()
                        Text("الصوت والترجمة", color = appAccent, fontWeight = FontWeight.Bold)
                        Text("اختر مسار الصوت أو الترجمة من عناصر التحكم داخل المشغّل عند توفر مسارات متعددة في المصدر. لا يفرض التطبيق لغة تلقائيًا حاليًا.", color = Color.LightGray, fontSize = 12.sp, lineHeight = 18.sp)
                        Divider()
                        Text("الإضافات", color = appAccent, fontWeight = FontWeight.Bold)
                        Text("إدارة المصادر من بطاقة «مصادرك وإضافاتك» في الرئيسية. تشغيل شيفرات الإضافات غير مفعّل حفاظًا على الأمان.", fontSize = 12.sp, color = Color.Gray)
                        if (watchProgressItems.isNotEmpty()) {
                            TextButton(onClick = { showSettings = false; showClearHistoryDialog = true }) { Text("مسح سجل متابعة المشاهدة") }
                        }
                        Text("RAIF • إصدار تجريبي", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                confirmButton = { TextButton(onClick = { showSettings = false }) { Text("تم") } }
            )
        }

        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("مسح متابعة المشاهدة؟") },
                text = { Text("سيُحذف تقدم المشاهدة المحفوظ على هذا الجهاز. لن تُحذف قائمة المفضلة.") },
                confirmButton = { TextButton(onClick = {
                    watchProgressStore.clear()
                    watchProgressItems.clear()
                    showClearHistoryDialog = false
                }) { Text("حذف السجل") } },
                dismissButton = { TextButton(onClick = { showClearHistoryDialog = false }) { Text("إلغاء") } }
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
                        catalogRequestVersion++
                        val installRequestVersion = catalogRequestVersion
                        showAddDialog = false
                        loading = true
                        status = "جارٍ التحقق من الإضافة..."
                        scope.launch {
                            try {
                                val manifest = repository.loadManifest(input)
                                if (installRequestVersion != catalogRequestVersion) return@launch
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
                                searching = false
                                loadingMore = false
                                activeAddon = installed
                                selectedItem = null
                                selectedMeta = null
                                playbackUrl = null
                                playingItem = null
                                streams.clear()
                                searchQuery = ""
                                catalogItems.clear()
                                nextPageOffset = 0
                                moreCatalogPages = false
                                val catalog = manifest.catalogs.firstOrNull()
                                if (catalog != null) {
                                    activeCatalogType = catalog.type
                                    activeCatalogId = catalog.id
                                    activeCatalogSupportsSearch = catalog.extra.any { it.name == "search" }
                                    activeCatalogSupportsSkip = catalog.extra.any { it.name == "skip" }
                                    try {
                                        val loaded = repository.loadCatalog(normalized, catalog.type, catalog.id)
                                        if (installRequestVersion != catalogRequestVersion) return@launch
                                        catalogItems.addAll(loaded)
                                        nextPageOffset = loaded.size
                                        moreCatalogPages = loaded.isNotEmpty()
                                        status = "تم تثبيت المصدر وجلب " + loaded.size + " عنصرًا"
                                    } catch (error: Exception) {
                                        if (error is CancellationException) throw error
                                        if (installRequestVersion == catalogRequestVersion) status = "تم تثبيت المصدر لكن تعذر جلب الكتالوج: " + (error.message ?: "خطأ غير معروف")
                                    }
                                } else {
                                    activeCatalogType = null
                                    activeCatalogId = null
                                    activeCatalogSupportsSearch = false
                                    activeCatalogSupportsSkip = false
                                    status = "تم تثبيت ${manifest.name}، ولا تعلن الإضافة عن كتالوج."
                                }
                                if (installRequestVersion == catalogRequestVersion) addonUrl = ""
                            } catch (error: Exception) {
                                if (error is CancellationException) throw error
                                if (installRequestVersion == catalogRequestVersion) status = "تعذر تحميل الإضافة: ${error.message ?: "خطأ غير معروف"}"
                            } finally {
                                if (installRequestVersion == catalogRequestVersion) loading = false
                            }
                        }
                    }) { Text("تحقق وأضف") }
                },
                dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
            )
        }

        selectedItem?.let { selected ->
            var preferredSourceName by remember(selected.type, selected.id, selected.name, selected.releaseInfo) {
                mutableStateOf(sourcePreferenceStore.load(selected))
            }
            var streamQualityFilter by remember(selected.type, selected.id) { mutableStateOf<String?>(null) }
            var streamProviderFilter by remember(selected.type, selected.id) { mutableStateOf<String?>(null) }
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
                    val selectedKey = MediaIdentity.storageKey(selected.type, selected.id, selected.name, selected.releaseInfo)
                    val isFavorite = selectedKey in favoriteIds
                    OutlinedButton(onClick = {
                        if (isFavorite) {
                            favoriteItems.removeAll { MediaIdentity.storageKey(it.type, it.id, it.name, it.releaseInfo) == selectedKey }
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
                            favoriteItems.removeAll { MediaIdentity.storageKey(it.type, it.id, it.name, it.releaseInfo) == selectedKey }
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
                    if (streamsError != null) {
                        Text(streamsError.orEmpty(), color = Color(0xFFFFB4AB), fontSize = 12.sp)
                        TextButton(onClick = { streamsRefreshVersion++ }) { Text("↻ إعادة جلب المصادر") }
                    }
                    if (loadingStreams) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("جارٍ جلب المصادر...", color = Color.LightGray)
                    } else if (streams.isEmpty()) {
                        Text("لم تُرجع الإضافة مصادر لهذا العنوان.", color = Color.LightGray, fontSize = 13.sp)
                    } else {
                        FilterChip(selected = playableSourcesOnly, onClick = { playableSourcesOnly = !playableSourcesOnly },
                            label = { Text("المصادر القابلة للتشغيل فقط") })
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf<String?>(null, "4K", "1440p", "1080p", "720p", "SD").forEach { quality ->
                                FilterChip(selected = streamQualityFilter == quality, onClick = { streamQualityFilter = quality },
                                    label = { Text(quality ?: "جميع الجودات") })
                            }
                        }
                        val providers = DiscoveryPresentation.availableProviders(streams.toList())
                        if (providers.size > 1) {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = streamProviderFilter == null, onClick = { streamProviderFilter = null }, label = { Text("جميع المزوّدين") })
                                providers.forEach { provider ->
                                    FilterChip(selected = streamProviderFilter == provider, onClick = { streamProviderFilter = provider }, label = { Text(provider) })
                                }
                            }
                        }
                        if (streamQualityFilter != null || streamProviderFilter != null) {
                            TextButton(onClick = { streamQualityFilter = null; streamProviderFilter = null }) { Text("مسح تصفية المصادر") }
                        }
                        preferredSourceName?.let { preference ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("آخر مصدر اخترته: " + preference, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 2)
                                TextButton(onClick = { sourcePreferenceStore.clear(selected); preferredSourceName = null }) { Text("مسح") }
                            }
                        }
                        val visibleStreams = DiscoveryPresentation.sortedStreams(streams.toList(), playableSourcesOnly, preferredSourceName, streamQualityFilter, streamProviderFilter)
                        Text("المصادر المعروضة: " + visibleStreams.size, color = Color.LightGray, fontSize = 11.sp)
                        if (visibleStreams.isEmpty()) {
                            Text("لا تتوفر روابط تشغيل مباشرة آمنة حاليًا.", color = Color.LightGray, fontSize = 12.sp)
                        }
                        visibleStreams.forEachIndexed { index, stream ->
                            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("${index + 1}. ${stream.name ?: stream.title ?: "مصدر"}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    if (stream.name == preferredSourceName) Text("★ آخر مصدر اخترته", color = appAccent, fontSize = 11.sp)
                                    val sourceBadges = listOfNotNull(
                                        DiscoveryPresentation.qualityLabel(stream),
                                        DiscoveryPresentation.codecLabel(stream),
                                        DiscoveryPresentation.hdrLabel(stream),
                                        DiscoveryPresentation.audioLabel(stream)?.let { "الصوت: $it" },
                                        DiscoveryPresentation.subtitleLabel(stream)?.let { "الترجمة: $it" }
                                    )
                                    if (sourceBadges.isNotEmpty()) {
                                        Text(sourceBadges.joinToString(" • "), color = appAccent, fontSize = 12.sp)
                                    }
                                    stream.title?.let { Text(it, color = Color.LightGray, fontSize = 12.sp) }
                                    stream.url?.let { mediaUrl ->
                                        Button(enabled = DiscoveryPresentation.isPlayable(stream), onClick = {
                                            if (com.cinemax.app.addons.StremioAddonClient.isSafeMediaUrl(mediaUrl)) {
                                                val meta = selectedMeta
                                                playingItem = selected.copy(
                                                    name = meta?.name ?: selected.name,
                                                    poster = meta?.poster ?: selected.poster,
                                                    background = meta?.background ?: selected.background,
                                                    description = meta?.description ?: selected.description,
                                                    releaseInfo = meta?.releaseInfo ?: selected.releaseInfo,
                                                    imdbRating = meta?.imdbRating ?: selected.imdbRating
                                                )
                                                stream.name?.let { sourceName ->
                                                    sourcePreferenceStore.save(selected, sourceName)
                                                    preferredSourceName = sourceName
                                                }
                                                playbackUrl = mediaUrl
                                            } else status = "تم رفض رابط تشغيل غير آمن. يجب أن يستخدم HTTPS."
                                        }, modifier = Modifier.fillMaxWidth()) { Text("▶ تشغيل") }
                                        if (!DiscoveryPresentation.isPlayable(stream)) Text("رابط التشغيل غير آمن أو غير مدعوم.", color = Color.Gray, fontSize = 11.sp)
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
            val playbackItem = playingItem
            Dialog(onDismissRequest = { playbackUrl = null; playingItem = null }) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("مشغل RAIF", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { playbackUrl = null; playingItem = null }) { Text("إغلاق") }
                    }
                    key(mediaUrl, playbackItem?.type, playbackItem?.id) {
                    NativeVideoPlayer(
                        url = mediaUrl,
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        initialPositionMillis = playbackItem?.let { item ->
                            watchProgressItems.firstOrNull { MediaIdentity.storageKey(it.item.type, it.item.id, it.item.name, it.item.releaseInfo) == MediaIdentity.storageKey(item.type, item.id, item.name, item.releaseInfo) }?.positionMillis ?: 0L
                        } ?: 0L,
                        onProgress = { positionMillis, durationMillis ->
                            val item = playbackItem
                            if (item != null && preferences.rememberPlaybackPosition) {
                                val progress = WatchProgress(
                                    item = item,
                                    positionMillis = positionMillis,
                                    durationMillis = durationMillis,
                                    updatedAtEpochMillis = Clock.System.now().toEpochMilliseconds()
                                )
                                watchProgressStore.save(progress)
                                watchProgressItems.removeAll { MediaIdentity.storageKey(it.item.type, it.item.id, it.item.name, it.item.releaseInfo) == MediaIdentity.storageKey(item.type, item.id, item.name, item.releaseInfo) }
                                if (!progress.isComplete) watchProgressItems.add(0, progress)
                            }
                        }
                    )
                    }
                }
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
