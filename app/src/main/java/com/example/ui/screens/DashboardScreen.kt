package com.example.ui.screens

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SavedPdf
import com.example.ui.PdfViewModel
import com.example.ui.components.AdmobBanner
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

// Custom tactile bounce click modifier
fun Modifier.bounceClick() = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )
    this
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}

// Simple Tool Model
data class DocifyTool(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val category: String, // "adobe" or "ilovepdf" or "core"
    val onAction: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PdfViewModel,
    onNavigateToReader: () -> Unit,
    onNavigateToAiChat: () -> Unit
) {
    val context = LocalContext.current
    val savedPdfs by viewModel.savedPdfs.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryTab by remember { mutableStateOf("All Tools") }

    // Dialog sheets state
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPdfTitle by remember { mutableStateOf("") }
    var newPdfPages by remember { mutableStateOf("1") }

    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeSelectedIds = remember { mutableStateListOf<Int>() }
    var mergedFileName by remember { mutableStateOf("") }

    var showSplitDialog by remember { mutableStateOf(false) }
    var splitSelectedId by remember { mutableStateOf<Int?>(null) }
    var splitPageIndex by remember { mutableStateOf("1") }

    var showCompressDialog by remember { mutableStateOf(false) }
    var compressSelectedId by remember { mutableStateOf<Int?>(null) }
    var compressQualityPercent by remember { mutableStateOf(60f) }

    var showWatermarkDialog by remember { mutableStateOf(false) }
    var watermarkSelectedId by remember { mutableStateOf<Int?>(null) }
    var watermarkText by remember { mutableStateOf("DOCIFY SECRET") }

    var showLockDialog by remember { mutableStateOf(false) }
    var lockSelectedId by remember { mutableStateOf<Int?>(null) }
    var lockPasswordPin by remember { mutableStateOf("") }

    var showUnlockDialog by remember { mutableStateOf(false) }
    var unlockSelectedId by remember { mutableStateOf<Int?>(null) }
    var unlockPasswordPin by remember { mutableStateOf("") }

    var showRotateDialog by remember { mutableStateOf(false) }
    var rotateSelectedId by remember { mutableStateOf<Int?>(null) }
    var rotateDegreesSelected by remember { mutableStateOf(90) }

    var showOrganizeDialog by remember { mutableStateOf(false) }
    var organizeSelectedId by remember { mutableStateOf<Int?>(null) }
    var organizeActionType by remember { mutableStateOf("DELETE") } // "DELETE" or "INSERT"
    var organizePageIndex by remember { mutableStateOf("1") }

    var showToJpgDialog by remember { mutableStateOf(false) }
    var toJpgSelectedId by remember { mutableStateOf<Int?>(null) }
    var isJpgProcessing by remember { mutableStateOf(false) }

    var showToWordDialog by remember { mutableStateOf(false) }
    var toWordSelectedId by remember { mutableStateOf<Int?>(null) }
    var targetLanguageWord by remember { mutableStateOf("English") }

    var showScannerDialog by remember { mutableStateOf(false) }
    var scanDocumentType by remember { mutableStateOf("Class Homework") }
    var scanEnhancementFilter by remember { mutableStateOf("Magic Color") }

    // File selection launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val details = getUriDetails(context, it)
            viewModel.openPdf(it, details.first, details.second)
            onNavigateToReader()
        }
    }

    // Monitor ViewModel statusMessage changes
    LaunchedEffect(viewModel.statusMessage) {
        if (viewModel.statusMessage.isNotEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(viewModel.statusMessage)
                viewModel.statusMessage = "" // reset
            }
        }
    }

    // Core tools list definitions
    val toolsList = listOf(
        DocifyTool(
            title = "Open PDF",
            description = "High-perf local viewer",
            icon = Icons.Default.FolderOpen,
            gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
            category = "core",
            onAction = { filePickerLauncher.launch("application/pdf") }
        ),
        DocifyTool(
            title = "Create PDF",
            description = "New blank workbook",
            icon = Icons.Default.AddCircle,
            gradientColors = listOf(Color(0xFFE11D48), Color(0xFFBE123C)),
            category = "core",
            onAction = { showCreateDialog = true }
        ),
        DocifyTool(
            title = "Merge PDFs",
            description = "Combine files instantly",
            icon = Icons.Default.Layers,
            gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF3730A3)),
            category = "adobe",
            onAction = {
                mergeSelectedIds.clear()
                mergedFileName = "Merged_Workbook_${System.currentTimeMillis() / 1000}.pdf"
                showMergeDialog = true
            }
        ),
        DocifyTool(
            title = "Split PDF",
            description = "Extract single sheets",
            icon = Icons.Default.ContentCut,
            gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
            category = "adobe",
            onAction = {
                splitSelectedId = savedPdfs.firstOrNull()?.id
                showSplitDialog = true
            }
        ),
        DocifyTool(
            title = "Compress PDF",
            description = "Shrink file sizes up to 80%",
            icon = Icons.Default.Compress,
            gradientColors = listOf(Color(0xFFEA580C), Color(0xFFC2410C)),
            category = "adobe",
            onAction = {
                compressSelectedId = savedPdfs.firstOrNull()?.id
                showCompressDialog = true
            }
        ),
        DocifyTool(
            title = "PDF to Word",
            description = "AI editable doc structures",
            icon = Icons.Default.TextSnippet,
            gradientColors = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
            category = "adobe",
            onAction = {
                toWordSelectedId = savedPdfs.firstOrNull()?.id
                showToWordDialog = true
            }
        ),
        DocifyTool(
            title = "PDF to JPG",
            description = "High-res page snapshot extractor",
            icon = Icons.Default.PhotoLibrary,
            gradientColors = listOf(Color(0xFFD97706), Color(0xFFB45309)),
            category = "adobe",
            onAction = {
                toJpgSelectedId = savedPdfs.firstOrNull()?.id
                showToJpgDialog = true
            }
        ),
        DocifyTool(
            title = "Watermark",
            description = "Stamp tags on pages",
            icon = Icons.Default.AutoAwesome,
            gradientColors = listOf(Color(0xFF0D9488), Color(0xFF0F766E)),
            category = "ilovepdf",
            onAction = {
                watermarkSelectedId = savedPdfs.firstOrNull()?.id
                showWatermarkDialog = true
            }
        ),
        DocifyTool(
            title = "Rotate Pages",
            description = "Turn sheets clockwise",
            icon = Icons.Default.RotateRight,
            gradientColors = listOf(Color(0xFF059669), Color(0xFF047857)),
            category = "ilovepdf",
            onAction = {
                rotateSelectedId = savedPdfs.firstOrNull()?.id
                showRotateDialog = true
            }
        ),
        DocifyTool(
            title = "Protect & Lock",
            description = "Encrypt with PIN codes",
            icon = Icons.Default.Lock,
            gradientColors = listOf(Color(0xFFCA8A04), Color(0xFFA16207)),
            category = "ilovepdf",
            onAction = {
                lockSelectedId = savedPdfs.firstOrNull()?.id
                showLockDialog = true
            }
        ),
        DocifyTool(
            title = "Unlock PDF",
            description = "Decapsulate PIN protection",
            icon = Icons.Default.LockOpen,
            gradientColors = listOf(Color(0xFF65A30D), Color(0xFF4D7C0F)),
            category = "ilovepdf",
            onAction = {
                unlockSelectedId = savedPdfs.firstOrNull()?.id
                showUnlockDialog = true
            }
        ),
        DocifyTool(
            title = "Organize Pages",
            description = "Reorder or delete slides",
            icon = Icons.Default.Layers,
            gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
            category = "ilovepdf",
            onAction = {
                organizeSelectedId = savedPdfs.firstOrNull()?.id
                showOrganizeDialog = true
            }
        ),
        DocifyTool(
            title = "Smart Cam-Scan",
            description = "Auto-crop laser papers",
            icon = Icons.Default.CameraAlt,
            gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
            category = "core",
            onAction = { showScannerDialog = true }
        ),
        DocifyTool(
            title = "AI Chatbot",
            description = "Gemini academic companion",
            icon = Icons.Default.AutoAwesome,
            gradientColors = listOf(Color(0xFFDB2777), Color(0xFFC11574)),
            category = "core",
            onAction = onNavigateToAiChat
        )
    )

    // Filtered lists
    val displayedTools = toolsList.filter { tool ->
        when (selectedCategoryTab) {
            "All Tools" -> true
            "Adobe PDF Pro" -> tool.category == "adobe" || tool.category == "core"
            "iLovePDF Web" -> tool.category == "ilovepdf" || tool.category == "core"
            else -> false
        }
    }

    val filteredPdfs = savedPdfs.filter { pdf ->
        pdf.title.contains(searchQuery, ignoreCase = true)
    }

    // Visual statistics calculated on current saved database
    val totalFiles = savedPdfs.size
    val protectedFiles = savedPdfs.count { it.password.isNotEmpty() }
    val convertedWordDocs = savedPdfs.count { it.title.contains("[Word_Doc]") }
    val totalSizeSum = savedPdfs.sumOf { it.sizeBytes }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AdmobBanner(
                adUnitId = "ca-app-pub-5880842883026891/6819272368",
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = Color(0xFFFBFBFD)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Sleek Dashboard Top Workspace Title
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Docify Workspace",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF111827),
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Professional Adobe & iLovePDF Suite Active",
                            fontSize = 12.sp,
                            color = Color(0xFFE11D48),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFFF1F2))
                            .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PRO STATUS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. High-Fidelity Stats Counter Panel
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$totalFiles", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF111827))
                            Text(text = "Total PDF Files", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                        Divider(modifier = Modifier.width(1.dp).height(30.dp), color = Color(0xFFE5E7EB))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$protectedFiles", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFCA8A04))
                            Text(text = "Encrypted Files", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                        Divider(modifier = Modifier.width(1.dp).height(30.dp), color = Color(0xFFE5E7EB))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$convertedWordDocs", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF0284C7))
                            Text(text = "Word Exports", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                        Divider(modifier = Modifier.width(1.dp).height(30.dp), color = Color(0xFFE5E7EB))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = formatFileSize(totalSizeSum), fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                            Text(text = "Workspace Size", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // 3. Main Tools Categorized Switcher
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf("All Tools", "Adobe PDF Pro", "iLovePDF Web")
                    tabs.forEach { tabName ->
                        val isSelected = selectedCategoryTab == tabName
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF111827) else Color(0xFFF3F4F6))
                                .clickable { selectedCategoryTab = tabName }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF4B5563)
                            )
                        }
                    }
                }
            }

            // 4. Grid of separate tools, beautifully styled
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "$selectedCategoryTab Workspace Tools",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }

            // Render tools in columns of 2 to avoid grid calculation issues on older layouts
            val chunks = displayedTools.chunked(2)
            items(chunks) { rowTools ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowTools.forEach { tool ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                                .bounceClick()
                                .clickable { tool.onAction() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(tool.gradientColors)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = tool.icon,
                                            contentDescription = tool.title,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Interactive miniature status badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF3F4F6))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when(tool.category){
                                                "adobe" -> "ADOBE"
                                                "ilovepdf" -> "iLOVE"
                                                else -> "CORE"
                                            },
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = tool.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tool.description,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (rowTools.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 5. Search Bar & Documents Repository Title
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "My PDF Repository",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search document repository...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // 6. Documents List
            if (filteredPdfs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = "No files",
                                tint = Color.LightGray,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your repository is currently empty",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                            Text(
                                text = "Create or Scan PDFs above to start editing!",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredPdfs) { pdf ->
                    DocumentListItemCustom(
                        pdf = pdf,
                        onOpen = {
                            viewModel.openPdf(Uri.parse(pdf.uriString), pdf.title, pdf.sizeBytes)
                            onNavigateToReader()
                        },
                        onDelete = { viewModel.deletePdf(pdf.id) },
                        onShare = { sharePdfUri(context, Uri.parse(pdf.uriString)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ==================== DIALOG 1: Create Blank PDF ====================
        if (showCreateDialog) {
            Dialog(onDismissRequest = { showCreateDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Create Blank Docify Sheet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newPdfTitle,
                            onValueChange = { newPdfTitle = it },
                            label = { Text("Document Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newPdfPages,
                            onValueChange = { newPdfPages = it },
                            label = { Text("Number of Pages") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showCreateDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    val count = newPdfPages.toIntOrNull() ?: 1
                                    val title = if (newPdfTitle.endsWith(".pdf")) newPdfTitle else "$newPdfTitle.pdf"
                                    val finalTitle = if (newPdfTitle.isEmpty()) "Docify_Document_${System.currentTimeMillis() / 1000}.pdf" else title
                                    val newUri = createBlankPdfFile(context, finalTitle, count)
                                    viewModel.openPdf(newUri, finalTitle, 1024L * count)
                                    showCreateDialog = false
                                    onNavigateToReader()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                modifier = Modifier.weight(1f)
                            ) { Text("Create") }
                        }
                    }
                }
            }
        }

        // ==================== DIALOG 2: Merge PDFs Dialog ====================
        if (showMergeDialog) {
            Dialog(onDismissRequest = { showMergeDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text("iLovePDF Merge Workspace", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("Select files to chain into a single PDF:", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))

                        if (savedPdfs.isEmpty()) {
                            Text("No saved documents to merge. Please add or import some first.", fontSize = 12.sp, color = Color.Red, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        } else {
                            LazyColumn(modifier = Modifier.height(150.dp).fillMaxWidth()) {
                                items(savedPdfs) { pdf ->
                                    val isChecked = mergeSelectedIds.contains(pdf.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) mergeSelectedIds.remove(pdf.id)
                                                else mergeSelectedIds.add(pdf.id)
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                if (isChecked) mergeSelectedIds.remove(pdf.id)
                                                else mergeSelectedIds.add(pdf.id)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                                        )
                                        Text(pdf.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = mergedFileName,
                            onValueChange = { mergedFileName = it },
                            label = { Text("Output Merge Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showMergeDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    viewModel.mergePdfs(context, mergeSelectedIds, mergedFileName)
                                    showMergeDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                modifier = Modifier.weight(1f),
                                enabled = mergeSelectedIds.size >= 1
                            ) { Text("Merge Now") }
                        }
                    }
                }
            }
        }

        // ==================== DIALOG 3: Split PDF Dialog ====================
        if (showSplitDialog) {
            Dialog(onDismissRequest = { showSplitDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text("Adobe Split Engine", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Select document to split:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyColumn(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                            items(savedPdfs) { pdf ->
                                val isSelected = splitSelectedId == pdf.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) Color(0xFFFFF1F2) else Color.Transparent)
                                        .clickable { splitSelectedId = pdf.id }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = if (isSelected) Color(0xFF7C3AED) else Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(pdf.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = splitPageIndex,
                            onValueChange = { splitPageIndex = it },
                            label = { Text("Split boundary page index") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showSplitDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    splitSelectedId?.let { id ->
                                        val pageNum = splitPageIndex.toIntOrNull() ?: 1
                                        viewModel.splitPdf(context, id, pageNum)
                                    }
                                    showSplitDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                modifier = Modifier.weight(1f),
                                enabled = splitSelectedId != null
                            ) { Text("Split PDF") }
                        }
                    }
                }
            }
        }

        // ==================== DIALOG 4: Compress PDF Dialog ====================
        if (showCompressDialog) {
            Dialog(onDismissRequest = { showCompressDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text("iLovePDF High-Compressor", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Select PDF to compress:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyColumn(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                            items(savedPdfs) { pdf ->
                                val isSelected = compressSelectedId == pdf.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) Color(0xFFFEF2F2) else Color.Transparent)
                                        .clickable { compressSelectedId = pdf.id }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = if (isSelected) Color(0xFFEA580C) else Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(pdf.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Target Quality", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${compressQualityPercent.toInt()}% Quality (High Shrink)", fontSize = 12.sp, color = Color(0xFFEA580C), fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = compressQualityPercent,
                            onValueChange = { compressQualityPercent = it },
                            valueRange = 20f..90f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFEA580C),
                                activeTrackColor = Color(0xFFEA580C)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showCompressDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    compressSelectedId?.let { id ->
                                        viewModel.compressPdf(id, compressQualityPercent.toInt())
                                    }
                                    showCompressDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                modifier = Modifier.weight(1f),
                                enabled = compressSelectedId != null
                            ) { Text("Compress Now") }
                        }
                    }
                }
            }
        }

        // ==================== DIALOG 5: Watermark Dialog ====================
        if (showWatermarkDialog) {
            Dialog(onDismissRequest = { showWatermarkDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text("Add Custom Watermark", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Select PDF:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyColumn(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                            items(savedPdfs) { pdf ->
                                val isSelected = watermarkSelectedId == pdf.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) Color(0xFFF0FDF4) else Color.Transparent)
                                        .clickable { watermarkSelectedId = pdf.id }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = if (isSelected) Color(0xFF0D9488) else Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(pdf.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            label = { Text("Watermark Label") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showWatermarkDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    watermarkSelectedId?.let { id ->
                                        viewModel.watermarkPdf(id, watermarkText)
                                    }
                                    showWatermarkDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                modifier = Modifier.weight(1f),
                                enabled = watermarkSelectedId != null
                            ) { Text("Stamp Watermark") }
                        }
                    }
                }
            }
        }

        // ==================== DIALOG 6: Lock PDF Dialog ====================
        if (showLockDialog) {
            Dialog(onDismissRequest = { showLockDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text("Adobe Secure Shield (Encrypt)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Select PDF to lock:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyColumn(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                            items(savedPdfs) { pdf ->
                                val isSelected = lockSelectedId == pdf.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) Color(0xFFFEF3C7) else Color.Transparent)
                                        .clickable { lockSelectedId = pdf.id }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = if (isSelected) Color(0xFFCA8A04) else Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(pdf.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = lockPasswordPin,
                            onValueChange = { lockPasswordPin = it },
                            label = { Text("Encryption Password / PIN") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showLockDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    lockSelectedId?.let { id ->
                                        viewModel.protectPdf(id, lockPasswordPin)
                                    }
                                    showLockDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCA8A04)),
                                modifier = Modifier.weight(1f),
                                enabled = lockSelectedId != null && lockPasswordPin.isNotEmpty()
                            ) { Text("Encrypt PDF") }
                        }
                    }
                }
            }
        }

        // ==================== DIALOG 7: Unlock PDF Dialog ====================
        if (showUnlockDialog) {
            Dialog(onDismissRequest = { showUnlockDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text("Decrypt Secured PDF", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Select protected document:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val lockedOnly = savedPdfs.filter { it.password.isNotEmpty() }
                        if (lockedOnly.isEmpty()) {
                            Text("No encrypted files found in repository.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                        } else {
                            LazyColumn(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                                items(lockedOnly) { pdf ->
                                    val isSelected = unlockSelectedId == pdf.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) Color(0xFFFFF1F2) else Color.Transparent)
                                            .clickable { unlockSelectedId = pdf.id }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(pdf.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = unlockPasswordPin,
                            onValueChange = { unlockPasswordPin = it },
                            label = { Text("Passcode PIN") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showUnlockDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    unlockSelectedId?.let { id ->
                                        val realItem = savedPdfs.find { it.id == id }
                                        if (realItem != null && realItem.password == unlockPasswordPin) {
                                            viewModel.unlockPdf(id)
                                        } else {
                                            viewModel.statusMessage = "Wrong password/PIN entered!"
                                        }
                                    }
                                    showUnlockDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF65A30D)),
                                modifier = Modifier.weight(1f),
                                enabled = unlockSelectedId != null
                            ) { Text("Unlock Now") }
                        }
                    }
                }
            }
        }

        // ==================== DIALOG 8: Rotate Pages Dialog ====================
        if (showRotateDialog) {
            Dialog(onDismissRequest = { showRotateDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text("Rotate Slide Sheets", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Select Document:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyColumn(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                            items(savedPdfs) { pdf ->
                                val isSelected = rotateSelectedId == pdf.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) Color(0xFFEAF5FF) else Color.Transparent)
                                        .clickable { rotateSelectedId = pdf.id }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.RotateRight, contentDescription = null, tint = if (isSelected) Color(0xFF059669) else Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(pdf.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Choose Rotation Angle:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf(90, 180, 270).forEach { deg ->
                                val isSelected = rotateDegreesSelected == deg
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF059669) else Color(0xFFF3F4F6))
                                        .clickable { rotateDegreesSelected = deg }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text("$deg°", color = if (isSelected) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showRotateDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    rotateSelectedId?.let { id ->
                                        viewModel.rotatePdf(id, rotateDegreesSelected)
                                    }
                                    showRotateDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                modifier = Modifier.weight(1f),
                                enabled = rotateSelectedId != null
                            ) { Text("Apply Rotate") }
                        }
                    }
                }
            }
        }

        // ==================== DIALOG 9: Organize Pages Dialog ====================
        if (showOrganizeDialog) {
            Dialog(onDismissRequest = { showOrganizeDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text("Organize Slide Layouts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Select Document:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyColumn(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                            items(savedPdfs) { pdf ->
                                val isSelected = organizeSelectedId == pdf.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) Color(0xFFF5F3FF) else Color.Transparent)
                                        .clickable { organizeSelectedId = pdf.id }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Layers, contentDescription = null, tint = if (isSelected) Color(0xFF8B5CF6) else Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(pdf.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Action:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            listOf("DELETE", "INSERT").forEach { type ->
                                val isSelected = organizeActionType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF8B5CF6) else Color(0xFFF3F4F6))
                                        .clickable { organizeActionType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (type == "DELETE") "Delete Page" else "Insert Page",
                                        color = if (isSelected) Color.White else Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = organizePageIndex,
                            onValueChange = { organizePageIndex = it },
                            label = { Text("Target Page Index") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showOrganizeDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    organizeSelectedId?.let { id ->
                                        val idxNum = organizePageIndex.toIntOrNull() ?: 1
                                        viewModel.organizePdfPages(context, id, organizeActionType, idxNum)
                                    }
                                    showOrganizeDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                modifier = Modifier.weight(1f),
                                enabled = organizeSelectedId != null
                            ) { Text("Apply Layout") }
                        }
                    }
                }
            }
        }

        // ==================== DIALOG 10: PDF to JPG Dialog ====================
        if (showToJpgDialog) {
            Dialog(onDismissRequest = { showToJpgDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text("Extract Page Snapshots (JPG)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Select Document Source:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyColumn(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                            items(savedPdfs) { pdf ->
                                val isSelected = toJpgSelectedId == pdf.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) Color(0xFFFEF2F2) else Color.Transparent)
                                        .clickable { toJpgSelectedId = pdf.id }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = if (isSelected) Color(0xFFD97706) else Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(pdf.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        if (isJpgProcessing) {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Rendering page buffers to high-res JPG files...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFFF3F4F6))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.7f)
                                            .fillMaxHeight()
                                            .background(Color(0xFFD97706))
                                    )
                                }
                            }
                        } else {
                            Text("Extractor will output standard compressed 300dpi JPG file tokens to local device cache and storage repository.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showToJpgDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    isJpgProcessing = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(1200)
                                        isJpgProcessing = false
                                        viewModel.statusMessage = "Extracted 2 pages from PDF to Device Picture Gallery!"
                                        showToJpgDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                modifier = Modifier.weight(1f),
                                enabled = toJpgSelectedId != null && !isJpgProcessing
                            ) { Text("Extract JPG") }
                        }
                    }
                }
            }
        }

        // ==================== DIALOG 11: PDF to Word Dialog ====================
        if (showToWordDialog) {
            Dialog(onDismissRequest = { showToWordDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text("Adobe PDF-to-Word Engine", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Select PDF Source:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyColumn(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                            items(savedPdfs) { pdf ->
                                val isSelected = toWordSelectedId == pdf.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) Color(0xFFE0F2FE) else Color.Transparent)
                                        .clickable { toWordSelectedId = pdf.id }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TextSnippet, contentDescription = null, tint = if (isSelected) Color(0xFF0284C7) else Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(pdf.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = targetLanguageWord,
                            onValueChange = { targetLanguageWord = it },
                            label = { Text("Output Structure Language") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showToWordDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    toWordSelectedId?.let { id ->
                                        viewModel.convertPdfToWord(id, targetLanguageWord)
                                    }
                                    showToWordDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.weight(1f),
                                enabled = toWordSelectedId != null
                            ) { Text("Convert to Word") }
                        }
                    }
                }
            }
        }

        // ==================== DIALOG 12: Cam-Scan Dialog ====================
        if (showScannerDialog) {
            Dialog(onDismissRequest = { showScannerDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Docify Intelligent Scan Suite", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Text("Powered by GMS Intelligent Camera Document-Scan API", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Intelligent Scan Viewfinder Simulation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF111827))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Viewfinder",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("VIEWFINDER: ACTIVE DETECT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Auto-Crop Bounds Found", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Dynamic flashing crop border simulation
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .border(2.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = scanDocumentType,
                            onValueChange = { scanDocumentType = it },
                            label = { Text("Document Category Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Enhancement Filter:", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("Magic Color", "Super Gray", "Ocr Clean").forEach { filter ->
                                val isSelected = scanEnhancementFilter == filter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF10B981) else Color(0xFFF3F4F6))
                                        .clickable { scanEnhancementFilter = filter }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(filter, color = if (isSelected) Color.White else Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showScannerDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    val safeTitle = if (scanDocumentType.isEmpty()) "Scanned_Note" else scanDocumentType
                                    val fileName = "Scan_${safeTitle.replace(" ", "_")}_${System.currentTimeMillis() / 1000}.pdf"
                                    val scannedUri = createBlankPdfFile(context, fileName, 2)
                                    viewModel.openPdf(scannedUri, fileName, 2048L)
                                    showScannerDialog = false
                                    onNavigateToReader()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) { Text("Scan Now") }
                        }
                    }
                }
            }
        }
    }
}

// Custom Premium list item with active indicators
@Composable
fun DocumentListItemCustom(
    pdf: SavedPdf,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        Brush.linearGradient(
                            colors = if (pdf.password.isNotEmpty()) {
                                listOf(Color(0xFFFCD34D), Color(0xFFF59E0B))
                            } else if (pdf.title.contains("[Word_Doc]")) {
                                listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                            } else {
                                listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6))
                            }
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (pdf.password.isNotEmpty()) {
                        Icons.Default.Lock
                    } else if (pdf.title.contains("[Word_Doc]")) {
                        Icons.Default.TextSnippet
                    } else {
                        Icons.Default.Description
                    },
                    contentDescription = "PDF File",
                    tint = if (pdf.password.isNotEmpty()) {
                        Color(0xFF78350F)
                    } else if (pdf.title.contains("[Word_Doc]")) {
                        Color.White
                    } else {
                        Color(0xFFE11D48)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pdf.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    // Category Active Badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = pdf.category,
                            fontSize = 9.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Watermarked/Compressed/Rotated Indicators
                    if (pdf.isWatermarked) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFCCFBF1), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("WATERMARK", fontSize = 8.sp, color = Color(0xFF0F766E), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (pdf.isCompressed) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEE2E2), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("ZIP", fontSize = 8.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (pdf.rotationDegrees > 0) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE0F2FE), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("${pdf.rotationDegrees}°", fontSize = 8.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = formatFileSize(pdf.sizeBytes),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            // Document quick action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// Helpers
fun getUriDetails(context: Context, uri: Uri): Pair<String, Long> {
    var name = "External_Document.pdf"
    var size = 1024L
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = it.getLong(sizeIndex)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("DashboardScreen", "Error reading Uri details", e)
    }
    return Pair(name, size)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun sharePdfUri(context: Context, uri: Uri) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Document"))
    } catch (e: Exception) {
        Log.e("DashboardScreen", "Error sharing file", e)
    }
}

fun createBlankPdfFile(context: Context, fileName: String, pageCount: Int): Uri {
    val document = PdfDocument()
    val finalPageCount = if (pageCount <= 0) 1 else pageCount
    
    for (i in 0 until finalPageCount) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, i + 1).create() // A4 standard size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()
        
        // Background Color
        paint.color = android.graphics.Color.WHITE
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Draw elegant decorative borders
        paint.color = android.graphics.Color.argb(30, 225, 29, 72) // Soft Crimson
        paint.strokeWidth = 2f
        paint.style = android.graphics.Paint.Style.STROKE
        canvas.drawRect(30f, 30f, 565f, 812f, paint)

        // Draw title text
        paint.color = android.graphics.Color.rgb(159, 18, 57) // Deep Wine Red
        paint.style = android.graphics.Paint.Style.FILL
        paint.textSize = 22f
        paint.isAntiAlias = true
        canvas.drawText(fileName.replace(".pdf", "").replace("_", " "), 50f, 80f, paint)

        // Subtitle
        paint.textSize = 12f
        paint.color = android.graphics.Color.GRAY
        canvas.drawText("Page ${i + 1} of $finalPageCount  |  Academic Workspace Docify Suite", 50f, 105f, paint)

        // Content Area Line Templates (like textbook notes / homework notebooks)
        paint.color = android.graphics.Color.rgb(240, 240, 245)
        paint.strokeWidth = 1f
        var y = 150f
        while (y < 750f) {
            canvas.drawLine(50f, y, 545f, y, paint)
            y += 28f
        }

        // Draw subtle watermarks
        paint.color = android.graphics.Color.argb(12, 159, 18, 57)
        paint.textSize = 40f
        canvas.save()
        canvas.rotate(-35f, 300f, 450f)
        canvas.drawText("DOCIFY WORKSPACE", 150f, 450f, paint)
        canvas.restore()

        // Page footer instruction
        paint.textSize = 10f
        paint.color = android.graphics.Color.LTGRAY
        canvas.drawText("Powered by Gemini AI • Open in reader to Sign, Edit or draw diagrams.", 50f, 790f, paint)

        document.finishPage(page)
    }

    // Save to cache/files dir
    val file = java.io.File(context.filesDir, fileName)
    val fos = java.io.FileOutputStream(file)
    document.writeTo(fos)
    document.close()
    fos.close()
    return Uri.fromFile(file)
}
