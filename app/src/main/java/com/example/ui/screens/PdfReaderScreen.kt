package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.AnnotationStroke
import com.example.ui.PdfViewModel
import com.example.ui.TextAnnotation
import com.example.ui.components.AdmobBanner
import com.example.ui.components.SignatureDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    viewModel: PdfViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uri = viewModel.selectedPdfUri ?: return

    var rendererState by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptorState by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var bitmapsList = remember { mutableStateListOf<Bitmap>() }
    var renderError by remember { mutableStateOf<String?>(null) }
    var isRendering by remember { mutableStateOf(true) }

    // Selected drawing tool: "None", "Pen", "Eraser", "Text", "Signature"
    var activeTool by remember { mutableStateOf("None") }
    var activeColorHex by remember { mutableStateOf("#E11D48") } // Adobe Crimson Default
    var activeStrokeWidth by remember { mutableStateOf(6f) }

    // Text typing dialog
    var showTextDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var textTapX by remember { mutableStateOf(0f) }
    var textTapY by remember { mutableStateOf(0f) }
    var textTapPageIndex by remember { mutableStateOf(0) }

    // Signature Pad dialog
    var showSignatureDialog by remember { mutableStateOf(false) }
    var signatureTapPageIndex by remember { mutableStateOf(0) }

    // AI Translation/Assistant Overlay
    var showAiOverlay by remember { mutableStateOf(false) }
    var aiQuery by remember { mutableStateOf("") }
    
    // Tab tracking inside AI overlay
    var aiSelectedTab by remember { mutableStateOf("Insights") }
    
    // Local interactive quiz taking state
    var currentQuizQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }
    var quizScore by remember { mutableStateOf(0) }
    
    // Reset local quiz taking state when a new quiz is generated
    LaunchedEffect(viewModel.aiQuizQuestions.value) {
        currentQuizQuestionIndex = 0
        selectedAnswerIndex = null
        isAnswerSubmitted = false
        quizScore = 0
    }
    
    // Local flashcard interactive state
    var currentCardIndex by remember { mutableStateOf(0) }
    var isCardFlipped by remember { mutableStateOf(false) }
    
    // Reset flashcards local states when a new deck is loaded
    LaunchedEffect(viewModel.aiFlashcards.value) {
        currentCardIndex = 0
        isCardFlipped = false
    }
    
    // Lazy list scrolling state for automatic page index tracking
    val scrollState = rememberLazyListState()
    
    LaunchedEffect(scrollState.firstVisibleItemIndex) {
        viewModel.currentPageIndex = scrollState.firstVisibleItemIndex
    }

    // Load PDF bitmaps using standard high-perf PdfRenderer
    LaunchedEffect(uri) {
        isRendering = true
        withContext(Dispatchers.IO) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    fileDescriptorState = pfd
                    val renderer = PdfRenderer(pfd)
                    rendererState = renderer
                    viewModel.pageCount = renderer.pageCount

                    val tempBitmaps = mutableListOf<Bitmap>()
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        
                        // Scale up rendering width/height for premium paper crispness
                        val width = (page.width * 1.5).toInt()
                        val height = (page.height * 1.5).toInt()
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        
                        // Fill white back layer (for transparent PNG page sources)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        tempBitmaps.add(bitmap)
                    }

                    withContext(Dispatchers.Main) {
                        bitmapsList.clear()
                        bitmapsList.addAll(tempBitmaps)
                        viewModel.pdfPageBitmaps.value = tempBitmaps
                        isRendering = false
                    }
                } else {
                    renderError = "Could not open document File Descriptor."
                    isRendering = false
                }
            } catch (e: Exception) {
                Log.e("PdfReaderScreen", "PdfRenderer initialization error", e)
                renderError = "Failed to load document: ${e.localizedMessage}"
                isRendering = false
            }
        }
    }

    // Clean up file descriptors safely on screen leave
    DisposableEffect(Unit) {
        onDispose {
            try {
                rendererState?.close()
                fileDescriptorState?.close()
            } catch (e: Exception) {
                Log.e("PdfReaderScreen", "Error releasing PdfRenderer resources", e)
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                // Top Custom Header Actions
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1F2937))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = viewModel.selectedPdfTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937),
                                    maxLines = 1,
                                    modifier = Modifier.width(180.dp)
                                )
                                Text(
                                    text = "Academic Reader & Annotator",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Top header action tags
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showAiOverlay = !showAiOverlay }) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Assistant",
                                    tint = if (showAiOverlay) Color(0xFF8B5CF6) else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            IconButton(onClick = { viewModel.saveAnnotations() }) {
                                Icon(Icons.Default.Save, contentDescription = "Save Document", tint = Color(0xFF10B981))
                            }
                        }
                    }
                }

                // 1. Top Banner Ad Integration
                AdmobBanner(
                    adUnitId = "ca-app-pub-5880842883026891/6819272368",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        bottomBar = {
            // 2. Down Banner Ad Integration
            AdmobBanner(
                adUnitId = "ca-app-pub-5880842883026891/6819272368",
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFE5E7EB)) // Standard dark-gray workspace backing
        ) {
            if (isRendering) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFFE11D48))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Rendering pages in high precision...", fontSize = 13.sp, color = Color.Gray)
                }
            } else if (renderError != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unable to render PDF",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = renderError ?: "Unknown error",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                    ) {
                        Text("Return to Repository")
                    }
                }
            } else {
                // Main PDF scroll list
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(bitmapsList) { pageIndex, bitmap ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(560.dp) // Custom fixed visual card sizing to look crisp
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                        ) {
                            // Page Image display
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Page ${pageIndex + 1}",
                                modifier = Modifier.fillMaxSize()
                            )

                            // Vector drawing annotation overlay
                            val drawings = viewModel.drawingsMap.value[pageIndex] ?: emptyList()
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                for (stroke in drawings) {
                                    if (stroke.points.size > 1) {
                                        val path = Path().apply {
                                            val first = stroke.points.first()
                                            moveTo(first.first, first.second)
                                            for (i in 1 until stroke.points.size) {
                                                val pt = stroke.points[i]
                                                lineTo(pt.first, pt.second)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color(android.graphics.Color.parseColor(stroke.colorHex)),
                                            style = Stroke(
                                                width = stroke.strokeWidth,
                                                cap = StrokeCap.Round
                                            )
                                        )
                                    }
                                }
                            }

                            // Render text overlays on top of the page
                            val textAnnos = viewModel.textAnnotationsMap.value[pageIndex] ?: emptyList()
                            textAnnos.forEach { textAnno ->
                                Text(
                                    text = textAnno.text,
                                    color = Color(android.graphics.Color.parseColor(textAnno.colorHex)),
                                    fontSize = textAnno.size.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(textAnno.x.toInt(), textAnno.y.toInt())
                                        }
                                        .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            // Gesture Capture canvas wrapper for drawing
                            var activeStrokePoints = remember { mutableStateListOf<Pair<Float, Float>>() }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(activeTool) {
                                        detectTapGestures(
                                            onDoubleTap = { offset ->
                                                if (activeTool == "Text") {
                                                    textTapX = offset.x
                                                    textTapY = offset.y
                                                    textTapPageIndex = pageIndex
                                                    textInput = ""
                                                    showTextDialog = true
                                                } else if (activeTool == "Signature") {
                                                    signatureTapPageIndex = pageIndex
                                                    showSignatureDialog = true
                                                }
                                            }
                                        )
                                    }
                                    .pointerInput(activeTool) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                if (activeTool == "Pen") {
                                                    activeStrokePoints.clear()
                                                    activeStrokePoints.add(Pair(offset.x, offset.y))
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                if (activeTool == "Pen") {
                                                    activeStrokePoints.add(Pair(change.position.x, change.position.y))
                                                }
                                            },
                                            onDragEnd = {
                                                if (activeTool == "Pen" && activeStrokePoints.isNotEmpty()) {
                                                    viewModel.addStroke(
                                                        pageIndex,
                                                        AnnotationStroke(
                                                            activeStrokePoints.toList(),
                                                            activeColorHex,
                                                            activeStrokeWidth
                                                        )
                                                    )
                                                    activeStrokePoints.clear()
                                                }
                                            }
                                        )
                                    }
                            ) {
                                // Draw temporary active gesture lines in real time
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (activeStrokePoints.size > 1) {
                                        val path = Path().apply {
                                            val first = activeStrokePoints.first()
                                            moveTo(first.first, first.second)
                                            for (i in 1 until activeStrokePoints.size) {
                                                val pt = activeStrokePoints[i]
                                                lineTo(pt.first, pt.second)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color(android.graphics.Color.parseColor(activeColorHex)),
                                            style = Stroke(
                                                width = activeStrokeWidth,
                                                cap = StrokeCap.Round
                                            )
                                        )
                                    }
                                }
                                
                                // Small decorative page tracker number
                                Text(
                                    text = "Page ${pageIndex + 1} of ${viewModel.pageCount}",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Annotation Draw Tools Dock (Right panel drawer floating style)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .width(56.dp)
                    .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(28.dp))
                    .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(28.dp))
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pen Selector
                ToolButton(
                    icon = Icons.Default.Edit,
                    isSelected = activeTool == "Pen",
                    color = Color(0xFFEF4444),
                    onClick = { activeTool = if (activeTool == "Pen") "None" else "Pen" }
                )

                // Text Selector
                ToolButton(
                    icon = Icons.Default.TextFields,
                    isSelected = activeTool == "Text",
                    color = Color(0xFF3B82F6),
                    onClick = { activeTool = if (activeTool == "Text") "None" else "Text" }
                )

                // Signature pad selector
                ToolButton(
                    icon = Icons.Default.Gesture,
                    isSelected = activeTool == "Signature",
                    color = Color(0xFF10B981),
                    onClick = { activeTool = if (activeTool == "Signature") "None" else "Signature" }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Color selectors (for writing/drawing)
                ColorSelector(colorHex = "#E11D48", isSelected = activeColorHex == "#E11D48", onClick = { activeColorHex = "#E11D48" })
                ColorSelector(colorHex = "#1E3A8A", isSelected = activeColorHex == "#1E3A8A", onClick = { activeColorHex = "#1E3A8A" })
                ColorSelector(colorHex = "#10B981", isSelected = activeColorHex == "#10B981", onClick = { activeColorHex = "#10B981" })

                Spacer(modifier = Modifier.height(4.dp))

                // Clear page drawings
                IconButton(
                    onClick = { viewModel.clearAnnotations(viewModel.currentPageIndex) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFEE2E2), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Page", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }

            // Active Tool helpful banner
            if (activeTool != "None") {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
                ) {
                    Text(
                        text = when(activeTool) {
                            "Pen" -> "✏️ Drawing Pen mode active: Drag to draw diagrams."
                            "Text" -> "✍️ Text Fill mode active: Double-tap page to write."
                            "Signature" -> "✒️ Signature Stamp active: Double-tap page to sign."
                            else -> ""
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // --- AI Assistant overlay side drawer ---
            AnimatedVisibility(
                visible = showAiOverlay,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .align(Alignment.CenterStart)
            ) {
                Surface(
                    modifier = Modifier.fillMaxHeight(),
                    color = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color(0xFF8B5CF6))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Study Suite", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            IconButton(onClick = { showAiOverlay = false }) {
                                Icon(Icons.Default.Clear, contentDescription = "Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom High-Fidelity Tab Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val tabs = listOf("Insights", "Quiz", "Flashcards")
                            tabs.forEach { tab ->
                                val isSelected = aiSelectedTab == tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color.White else Color.Transparent)
                                        .clickable { aiSelectedTab = tab }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF8B5CF6) else Color.Gray
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Content of the active tab
                        when (aiSelectedTab) {
                            "Insights" -> {
                                // Fast actions
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { viewModel.translatePage("English", viewModel.currentPageIndex) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Translate, contentDescription = "Translate", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Translate", fontSize = 11.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.askGemini("Provide a bullet-point summary explaining the core concepts, homework keywords, and key definitions on this document page.", viewModel.currentPageIndex) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Summarize", fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = aiQuery,
                                    onValueChange = { aiQuery = it },
                                    placeholder = { Text("Ask anything about this page...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            if (aiQuery.isNotEmpty()) {
                                                viewModel.askGemini(aiQuery, viewModel.currentPageIndex)
                                                aiQuery = ""
                                            }
                                        }) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = "Ask", tint = Color(0xFF8B5CF6))
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("AI INSIGHTS & TRANSLATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                                Spacer(modifier = Modifier.height(6.dp))

                                // AI Output Area
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF9FAFB))
                                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    if (viewModel.aiIsLoading) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(color = Color(0xFF8B5CF6))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Gemini AI thinking...", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            item {
                                                Text(
                                                    text = if (viewModel.aiResponse.isEmpty()) "Select Translate, Summarize, or type a custom question above to let Gemini parse this page." else viewModel.aiResponse,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp,
                                                    color = Color(0xFF374151)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            "Quiz" -> {
                                val quizQuestions = viewModel.aiQuizQuestions.value
                                
                                when {
                                    viewModel.quizLoading.value -> {
                                        Column(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(color = Color(0xFF8B5CF6))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Gemini is composing academic quiz...", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                    viewModel.quizErrorMessage.isNotEmpty() -> {
                                        Column(
                                            modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("Failed to generate quiz", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(viewModel.quizErrorMessage, fontSize = 11.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { viewModel.generateQuiz(viewModel.currentPageIndex) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                                            ) {
                                                Text("Try Again")
                                            }
                                        }
                                    }
                                    quizQuestions.isEmpty() -> {
                                        Column(
                                            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.School,
                                                contentDescription = "Quiz Creator",
                                                tint = Color(0xFF8B5CF6),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Interactive AI Quiz", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Let Gemini analyze the current page and generate custom multiple-choice questions to test your knowledge.",
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(20.dp))
                                            Button(
                                                onClick = { viewModel.generateQuiz(viewModel.currentPageIndex) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Generate Page Quiz", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                    else -> {
                                        // Take the Quiz!
                                        if (currentQuizQuestionIndex >= quizQuestions.size) {
                                            // Finish view
                                            Column(
                                                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("🎉 Quiz Completed!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF10B981))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Your Score: $quizScore / ${quizQuestions.size}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Button(
                                                    onClick = { viewModel.generateQuiz(viewModel.currentPageIndex) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                                                ) {
                                                    Text("Regenerate Quiz")
                                                }
                                            }
                                        } else {
                                            val question = quizQuestions[currentQuizQuestionIndex]
                                            Column(
                                                modifier = Modifier.weight(1f).fillMaxWidth()
                                            ) {
                                                // Progress text
                                                Text("Question ${currentQuizQuestionIndex + 1} of ${quizQuestions.size}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(question.question, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Options List
                                                LazyColumn(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    itemsIndexed(question.options) { optIndex, optionText ->
                                                        val isSelected = selectedAnswerIndex == optIndex
                                                        val isCorrect = optIndex == question.correctAnswerIndex
                                                        
                                                        val backColor = when {
                                                            !isAnswerSubmitted -> if (isSelected) Color(0xFFF3E8FF) else Color(0xFFF9FAFB)
                                                            else -> when {
                                                                isCorrect -> Color(0xFFD1FAE5) // light green
                                                                isSelected && !isCorrect -> Color(0xFFFEE2E2) // light red
                                                                else -> Color(0xFFF9FAFB)
                                                            }
                                                        }
                                                        
                                                        val bColor = when {
                                                            !isAnswerSubmitted -> if (isSelected) Color(0xFF8B5CF6) else Color(0xFFE5E7EB)
                                                            else -> when {
                                                                isCorrect -> Color(0xFF10B981)
                                                                isSelected && !isCorrect -> Color(0xFFEF4444)
                                                                else -> Color(0xFFE5E7EB)
                                                            }
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(backColor)
                                                                .border(1.dp, bColor, RoundedCornerShape(8.dp))
                                                                .clickable(enabled = !isAnswerSubmitted) {
                                                                    selectedAnswerIndex = optIndex
                                                                }
                                                                .padding(10.dp)
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                val optLabel = when(optIndex) {
                                                                    0 -> "A"
                                                                    1 -> "B"
                                                                    2 -> "C"
                                                                    else -> "D"
                                                                }
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(20.dp)
                                                                        .clip(CircleShape)
                                                                        .background(if (isSelected) Color(0xFF8B5CF6) else Color.LightGray),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(optLabel, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                                }
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(optionText, fontSize = 12.sp, color = Color(0xFF374151))
                                                            }
                                                        }
                                                    }

                                                    if (isAnswerSubmitted) {
                                                        item {
                                                            Spacer(modifier = Modifier.height(12.dp))
                                                            Card(
                                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047)),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Column(modifier = Modifier.padding(10.dp)) {
                                                                    Text("Explanation:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF854D0E))
                                                                    Spacer(modifier = Modifier.height(2.dp))
                                                                    Text(question.explanation, fontSize = 11.sp, color = Color(0xFF713F12))
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                // Bottom controls
                                                Spacer(modifier = Modifier.height(8.dp))
                                                if (!isAnswerSubmitted) {
                                                    Button(
                                                        onClick = {
                                                            isAnswerSubmitted = true
                                                            if (selectedAnswerIndex == question.correctAnswerIndex) {
                                                                quizScore++
                                                            }
                                                        },
                                                        enabled = selectedAnswerIndex != null,
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("Submit Answer")
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            currentQuizQuestionIndex++
                                                            selectedAnswerIndex = null
                                                            isAnswerSubmitted = false
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("Next Question")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "Flashcards" -> {
                                val flashcards = viewModel.aiFlashcards.value
                                
                                when {
                                    viewModel.flashcardsLoading.value -> {
                                        Column(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(color = Color(0xFF8B5CF6))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Gemini is forging card deck...", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                    viewModel.flashcardsErrorMessage.isNotEmpty() -> {
                                        Column(
                                            modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("Failed to generate flashcards", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(viewModel.flashcardsErrorMessage, fontSize = 11.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { viewModel.generateFlashcards(viewModel.currentPageIndex) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                                            ) {
                                                Text("Try Again")
                                            }
                                        }
                                    }
                                    flashcards.isEmpty() -> {
                                        Column(
                                            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = "Flashcard Creator",
                                                tint = Color(0xFFFFB000),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("AI Study Flashcards", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Automatically extract key terms, formulas, and academic definitions into visual study flashcards.",
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(20.dp))
                                            Button(
                                                onClick = { viewModel.generateFlashcards(viewModel.currentPageIndex) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Forge Card Decks", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                    else -> {
                                        // Show Flashcard deck
                                        val card = flashcards[currentCardIndex]
                                        Column(
                                            modifier = Modifier.weight(1f).fillMaxWidth()
                                        ) {
                                            Text("Card ${currentCardIndex + 1} of ${flashcards.size}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Card body with beautiful flip state theme
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .clickable { isCardFlipped = !isCardFlipped },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isCardFlipped) Color(0xFFECFDF5) else Color(0xFFF5F3FF)
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isCardFlipped) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF8B5CF6).copy(alpha = 0.5f)
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(16.dp),
                                                    verticalArrangement = Arrangement.Center,
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    // Card Side Tag
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                if (isCardFlipped) Color(0xFF10B981) else Color(0xFF8B5CF6),
                                                                RoundedCornerShape(12.dp)
                                                            )
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isCardFlipped) "BACK / DEFINITION" else "FRONT / TERM",
                                                            color = Color.White,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(24.dp))

                                                    // Text
                                                    Text(
                                                        text = if (isCardFlipped) card.back else card.front,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1F2937),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Text(
                                                        text = "🔄 Tap card to flip",
                                                        fontSize = 9.sp,
                                                        color = Color.LightGray
                                                    )
                                                }
                                            }

                                            // Footer slider deck buttons
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = {
                                                        if (currentCardIndex > 0) {
                                                            currentCardIndex--
                                                            isCardFlipped = false
                                                        }
                                                    },
                                                    enabled = currentCardIndex > 0,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Prev", fontSize = 11.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        if (currentCardIndex < flashcards.size - 1) {
                                                            currentCardIndex++
                                                            isCardFlipped = false
                                                        }
                                                    },
                                                    enabled = currentCardIndex < flashcards.size - 1,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Next", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Caution Notice: Gemini may make mistakes. Verify critical textbook notes.",
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        // --- Text insertion Dialog ---
        if (showTextDialog) {
            Dialog(onDismissRequest = { showTextDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Add Text Note / Fill Form", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Type annotations or comments...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = { showTextDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (textInput.isNotEmpty()) {
                                        viewModel.addTextAnnotation(
                                            textTapPageIndex,
                                            TextAnnotation(
                                                text = textInput,
                                                x = textTapX,
                                                y = textTapY,
                                                colorHex = activeColorHex,
                                                size = 14f
                                            )
                                        )
                                        showTextDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                            ) {
                                Text("Apply", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // --- Signature Stamp Dialog ---
        if (showSignatureDialog) {
            SignatureDialog(
                onDismiss = { showSignatureDialog = false },
                onSaveSignature = { points ->
                    // Convert points list to annotation stroke
                    viewModel.addStroke(
                        signatureTapPageIndex,
                        AnnotationStroke(
                            points,
                            "#1E3A8A", // Formal ink blue
                            4f
                        )
                    )
                    showSignatureDialog = false
                }
            )
        }
    }
}

// Side Draw Tool icon button
@Composable
fun ToolButton(
    icon: ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
            .border(1.dp, if (isSelected) color else Color.Transparent, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Tool",
            tint = if (isSelected) color else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Draw Color selector dot
@Composable
fun ColorSelector(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(Color(android.graphics.Color.parseColor(colorHex)), CircleShape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
