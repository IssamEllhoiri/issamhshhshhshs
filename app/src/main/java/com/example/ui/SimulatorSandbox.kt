package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SimulatorSandbox(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    // Settings flows
    val cursorSizePref by viewModel.cursorSize.collectAsState()
    val cursorColorPref by viewModel.cursorColor.collectAsState()
    val cursorSensitivityPref by viewModel.cursorSensitivity.collectAsState()
    val clickVfxPref by viewModel.cursorClickVfx.collectAsState()
    val clickVfxColorPref by viewModel.cursorClickVfxColor.collectAsState()

    // Simulator positions
    var simCursorX by remember { mutableFloatStateOf(screenWidthPx / 2f) }
    var simCursorY by remember { mutableFloatStateOf(screenHeightPx / 3f) }

    var panelX by remember { mutableFloatStateOf(screenWidthPx - with(density) { 60.dp.toPx() }) }
    var panelY by remember { mutableFloatStateOf(screenHeightPx / 2f) }
    var isPanelExpanded by remember { mutableStateOf(false) }

    var touchpadX by remember { mutableFloatStateOf(screenWidthPx / 2f - with(density) { 90.dp.toPx() }) }
    var touchpadY by remember { mutableFloatStateOf(screenHeightPx - with(density) { 260.dp.toPx() }) }

    // Action mode
    var activeMode by remember { mutableStateOf("POINTER") } // "POINTER", "DRAG", "SCROLL"
    var showMockKeyboard by remember { mutableStateOf(false) }

    // Simulated click ripples
    var rippleOffset by remember { mutableStateOf<Offset?>(null) }
    var rippleScale = remember { Animatable(0f) }
    var rippleAlpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // Feedback logs
    var statusText by remember { mutableStateOf("Simulator Ready! Drag the touchpad below to move the pointer.") }

    fun triggerClick(x: Float, y: Float) {
        statusText = "Triggered standard Click at (${x.roundToInt()}, ${y.roundToInt()})!"
        if (clickVfxPref) {
            scope.launch {
                rippleOffset = Offset(x, y)
                rippleScale.snapTo(0f)
                rippleAlpha.snapTo(1f)
                launch {
                    rippleScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    rippleAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    )
                }
                delay(360)
                rippleOffset = null
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Status Banner Alert inside the sandbox
        Card(
            colors = CardDefaults.cardColors(containerColor = AccentGlow),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.setSimulatorActive(false) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop Demo",
                        tint = ErrorColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 2. Click ripples rendering
        rippleOffset?.let { offset ->
            val color = when (clickVfxColorPref) {
                "PURPLE" -> AccentPrimary
                "BLUE" -> InfoColor
                "GREEN" -> SuccessColor
                "RED" -> ErrorColor
                else -> AccentPrimary
            }
            Box(
                modifier = Modifier
                    .offset { IntOffset(offset.x.roundToInt() - 25, offset.y.roundToInt() - 25) }
                    .size(50.dp)
                    .background(
                        color.copy(alpha = rippleAlpha.value * 0.4f),
                        CircleShape
                    )
                    .border(2.dp, color.copy(alpha = rippleAlpha.value), CircleShape)
            )
        }

        // 3. Simulated Keyboard
        AnimatedVisibility(
            visible = showMockKeyboard,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.shadow(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Simulated Virtual Keyboard",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showMockKeyboard = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val keys = listOf(
                        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
                        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
                        listOf("Z", "X", "C", "V", "B", "N", "M", "⌫")
                    )
                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(BgTertiary)
                                        .clickable {
                                            statusText = "Pressed simulated key: $key"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Simulated Floating Control Panel (Draggable Overlay)
        Box(
            modifier = Modifier
                .offset { IntOffset(panelX.roundToInt(), panelY.roundToInt()) }
                .shadow(12.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(BgSecondary.copy(alpha = 0.95f))
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .width(if (isPanelExpanded) 220.dp else 50.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Header / Drag bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgTertiary)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                panelX = (panelX + dragAmount.x).coerceIn(0f, screenWidthPx - 50.dp.toPx())
                                panelY = (panelY + dragAmount.y).coerceIn(0f, screenHeightPx - 100.dp.toPx())
                            }
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Drag",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    if (isPanelExpanded) {
                        Text(
                            text = "PointerX Panel",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(start = 6.dp)
                        )
                        IconButton(
                            onClick = { isPanelExpanded = false },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    } else {
                        IconButton(
                            onClick = { isPanelExpanded = true },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, null, tint = AccentPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                if (isPanelExpanded) {
                    // Grid of Actions
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { triggerClick(simCursorX, simCursorY) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("L-CLK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Button(
                            onClick = {
                                statusText = "Right Click context menu activated at (${simCursorX.roundToInt()}, ${simCursorY.roundToInt()})!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BgTertiary),
                            border = BorderStroke(1.dp, BorderColor),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("R-CLK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                activeMode = if (activeMode == "DRAG") "POINTER" else "DRAG"
                                statusText = if (activeMode == "DRAG") "Drag Mode Active! Swipe to slide items." else "Normal Pointer mode active."
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeMode == "DRAG") SuccessColor else BgTertiary
                            ),
                            border = BorderStroke(1.dp, BorderColor),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("DRAG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Button(
                            onClick = {
                                activeMode = if (activeMode == "SCROLL") "POINTER" else "SCROLL"
                                statusText = if (activeMode == "SCROLL") "Scroll Mode Active! Swipe to scroll page." else "Normal Pointer mode active."
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeMode == "SCROLL") InfoColor else BgTertiary
                            ),
                            border = BorderStroke(1.dp, BorderColor),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("SCROLL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showMockKeyboard = !showMockKeyboard },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showMockKeyboard) AccentPrimary else BgTertiary
                            ),
                            border = BorderStroke(1.dp, BorderColor),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("KBD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Button(
                            onClick = {
                                statusText = "Long Click (1000ms) executed at (${simCursorX.roundToInt()}, ${simCursorY.roundToInt()})!"
                                triggerClick(simCursorX, simCursorY)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BgTertiary),
                            border = BorderStroke(1.dp, BorderColor),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("LNG-CLK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    // Small compact state buttons
                    Spacer(modifier = Modifier.height(6.dp))
                    IconButton(
                        onClick = { triggerClick(simCursorX, simCursorY) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = AccentPrimary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { showMockKeyboard = !showMockKeyboard },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.List, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        // 5. Simulated Floating Touchpad
        Card(
            colors = CardDefaults.cardColors(containerColor = BgSecondary.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, AccentPrimary),
            modifier = Modifier
                .offset { IntOffset(touchpadX.roundToInt(), touchpadY.roundToInt()) }
                .width(180.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                // Header (Draggable touchpad bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgTertiary, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                touchpadX = (touchpadX + dragAmount.x).coerceIn(0f, screenWidthPx - 180.dp.toPx())
                                touchpadY = (touchpadY + dragAmount.y).coerceIn(0f, screenHeightPx - 200.dp.toPx())
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Touchpad (Drag Me)",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(modifier = Modifier.size(12.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // The sensitive trackpad touchpad swipe zone
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgTertiary)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    triggerClick(simCursorX, simCursorY)
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    statusText = "Sliding pointer on touchpad..."
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val mult = cursorSensitivityPref
                                    simCursorX = (simCursorX + dragAmount.x * mult).coerceIn(10f, screenWidthPx - 10f)
                                    simCursorY = (simCursorY + dragAmount.y * mult).coerceIn(10f, screenHeightPx - 10f)
                                    statusText = "Pointer moving: (${simCursorX.roundToInt()}, ${simCursorY.roundToInt()})"
                                },
                                onDragEnd = {
                                    statusText = "Released touchpad. Cursor resting at (${simCursorX.roundToInt()}, ${simCursorY.roundToInt()})."
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = TextDisabled,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (activeMode == "SCROLL") "Scroll Swipe Zone" else "Slide Finger Here",
                            color = TextDisabled,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 6. Simulated High-Precision Mouse Cursor Pointer
        Box(
            modifier = Modifier
                .offset { IntOffset(simCursorX.roundToInt(), simCursorY.roundToInt()) }
                .size(cursorSizePref.dp)
        ) {
            val cursorColor = when (cursorColorPref) {
                "WHITE" -> Color.White
                "PURPLE" -> AccentPrimary
                "BLUE" -> InfoColor
                "GREEN" -> SuccessColor
                "RED" -> ErrorColor
                "BLACK" -> Color.Black
                else -> Color.White
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val sizePx = size.width
                val arrowPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(0f, sizePx * 0.75f)
                    lineTo(sizePx * 0.22f, sizePx * 0.55f)
                    lineTo(sizePx * 0.48f, sizePx * 0.95f)
                    lineTo(sizePx * 0.60f, sizePx * 0.90f)
                    lineTo(sizePx * 0.35f, sizePx * 0.50f)
                    lineTo(sizePx * 0.55f, sizePx * 0.50f)
                    close()
                }

                // Shadow border (skip if pitch black)
                if (cursorColorPref != "BLACK") {
                    drawPath(
                        path = arrowPath,
                        color = Color.Black.copy(alpha = 0.5f),
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                }

                // Fill cursor
                drawPath(
                    path = arrowPath,
                    color = cursorColor
                )

                // Clean stroke border matching cursor color when black
                drawPath(
                    path = arrowPath,
                    color = if (cursorColorPref == "BLACK") Color.Black else Color.Black,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}
