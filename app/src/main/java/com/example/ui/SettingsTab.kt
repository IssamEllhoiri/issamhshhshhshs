package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsTab(viewModel: MainViewModel) {
    val scrollState = rememberScrollState()

    // Collect settings flows
    val cursorSize by viewModel.cursorSize.collectAsState()
    val cursorSensitivity by viewModel.cursorSensitivity.collectAsState()
    val cursorColor by viewModel.cursorColor.collectAsState()
    val cursorClickVfx by viewModel.cursorClickVfx.collectAsState()
    val cursorClickVfxColor by viewModel.cursorClickVfxColor.collectAsState()
    val defaultCursorShape by viewModel.defaultCursorShape.collectAsState()
    val hoverCursorShape by viewModel.hoverCursorShape.collectAsState()
    val textCursorShape by viewModel.textCursorShape.collectAsState()
    val defaultCursorImage by viewModel.defaultCursorImage.collectAsState()
    val hoverCursorImage by viewModel.hoverCursorImage.collectAsState()
    val textCursorImage by viewModel.textCursorImage.collectAsState()
    val panelAlpha by viewModel.panelAlpha.collectAsState()
    val panelSize by viewModel.panelSize.collectAsState()
    val panelColor by viewModel.panelColor.collectAsState()
    val vibrationEnabled by viewModel.vibration.collectAsState()
    val scrollSpeed by viewModel.scrollSpeed.collectAsState()
    val autoStart by viewModel.autoStart.collectAsState()
    val longClickMs by viewModel.longClickMs.collectAsState()
    val doubleClickMs by viewModel.doubleClickMs.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // --- SECTION 1: CURSOR SETTINGS ---
        SettingsSectionHeader(title = "Cursor & Appearance Settings")
        Card(
            colors = CardDefaults.cardColors(containerColor = BgTertiary),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Size
                Text(
                    text = "Cursor Size ($cursorSize dp)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Slider(
                    value = cursorSize.toFloat(),
                    onValueChange = { viewModel.updateCursorSize(it.toInt()) },
                    valueRange = 16f..40f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = AccentPrimary,
                        thumbColor = AccentPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sensitivity
                Text(
                    text = "Sensitivity (${String.format("%.1f", cursorSensitivity)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Slider(
                    value = cursorSensitivity,
                    onValueChange = { viewModel.updateCursorSensitivity(it) },
                    valueRange = 1.0f..4.0f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = AccentPrimary,
                        thumbColor = AccentPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Color Picker Grid
                Text(
                    text = "Cursor Color",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ColorPickerButton(
                        colorName = "WHITE",
                        colorVal = Color(0xFFEEEEF8),
                        isSelected = cursorColor == "WHITE",
                        onClick = { viewModel.updateCursorColor("WHITE") }
                    )
                    ColorPickerButton(
                        colorName = "PURPLE",
                        colorVal = Color(0xFF6C63FF),
                        isSelected = cursorColor == "PURPLE",
                        onClick = { viewModel.updateCursorColor("PURPLE") }
                    )
                    ColorPickerButton(
                        colorName = "BLUE",
                        colorVal = Color(0xFF40C4FF),
                        isSelected = cursorColor == "BLUE",
                        onClick = { viewModel.updateCursorColor("BLUE") }
                    )
                    ColorPickerButton(
                        colorName = "RED",
                        colorVal = Color(0xFFFF5252),
                        isSelected = cursorColor == "RED",
                        onClick = { viewModel.updateCursorColor("RED") }
                    )
                    ColorPickerButton(
                        colorName = "BLACK",
                        colorVal = Color(0xFF000000),
                        isSelected = cursorColor == "BLACK",
                        onClick = { viewModel.updateCursorColor("BLACK") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle click ripple VFX
                SettingsToggleRow(
                    title = "Visual Click VFX (Ripple Effect)",
                    checked = cursorClickVfx,
                    onCheckedChange = { viewModel.updateCursorClickVfx(it) }
                )

                if (cursorClickVfx) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Click VFX Color",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ColorPickerButton(
                            colorName = "PURPLE",
                            colorVal = Color(0xFF6C63FF),
                            isSelected = cursorClickVfxColor == "PURPLE",
                            onClick = { viewModel.updateCursorClickVfxColor("PURPLE") }
                        )
                        ColorPickerButton(
                            colorName = "BLUE",
                            colorVal = Color(0xFF40C4FF),
                            isSelected = cursorClickVfxColor == "BLUE",
                            onClick = { viewModel.updateCursorClickVfxColor("BLUE") }
                        )
                        ColorPickerButton(
                            colorName = "RED",
                            colorVal = Color(0xFFFF5252),
                            isSelected = cursorClickVfxColor == "RED",
                            onClick = { viewModel.updateCursorClickVfxColor("RED") }
                        )
                        ColorPickerButton(
                            colorName = "GOLD",
                            colorVal = Color(0xFFFFB300),
                            isSelected = cursorClickVfxColor == "GOLD",
                            onClick = { viewModel.updateCursorClickVfxColor("GOLD") }
                        )
                        ColorPickerButton(
                            colorName = "GREEN",
                            colorVal = Color(0xFF4CAF50),
                            isSelected = cursorClickVfxColor == "GREEN",
                            onClick = { viewModel.updateCursorClickVfxColor("GREEN") }
                        )
                        ColorPickerButton(
                            colorName = "WHITE",
                            colorVal = Color(0xFFEEEEF8),
                            isSelected = cursorClickVfxColor == "WHITE",
                            onClick = { viewModel.updateCursorClickVfxColor("WHITE") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val defaultShapeOptions = listOf(
                    "DEFAULT" to "Classic Arrow",
                    "CROSSHAIR" to "Crosshair",
                    "WAIT" to "Waiting Spinner (Animated)",
                    "TEXT" to "I-Beam Text Selector",
                    "GRAB" to "Grab Hand (Open)",
                    "POINTER" to "Pointer Hand (Selection)",
                    "RESIZE_H" to "Horizontal Resize Arrow",
                    "RESIZE_V" to "Vertical Resize Arrow"
                )

                val textShapeOptions = listOf(
                    "TEXT" to "I-Beam Text Selector",
                    "DEFAULT" to "Classic Arrow",
                    "CROSSHAIR" to "Crosshair",
                    "WAIT" to "Waiting Spinner (Animated)",
                    "POINTER" to "Pointer Hand (Selection)"
                )

                val hoverShapeOptions = listOf(
                    "POINTER" to "Pointer Hand (Selection)",
                    "DEFAULT" to "Classic Arrow",
                    "CROSSHAIR" to "Crosshair",
                    "GRAB" to "Grab Hand (Open)",
                    "NOT_ALLOWED" to "Blocked / Not Allowed"
                )

                // 1. Default cursor
                ShapeDropdownSelector(
                    label = "Default Cursor Shape (General)",
                    selectedValue = defaultCursorShape,
                    options = defaultShapeOptions,
                    onValueSelected = { viewModel.updateDefaultCursorShape(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                CustomImagePickerSection(
                    label = "Custom Image for Default Cursor",
                    currentPath = defaultCursorImage,
                    onImageSelected = { uri ->
                        coroutineScope.launch(Dispatchers.IO) {
                            val path = saveUriToInternalStorage(context, uri, "default_cursor.png")
                            viewModel.updateDefaultCursorImage(path)
                        }
                    },
                    onRemoveImage = { viewModel.updateDefaultCursorImage("") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Text cursor
                ShapeDropdownSelector(
                    label = "Text Hover Cursor Shape (Input & Search)",
                    selectedValue = textCursorShape,
                    options = textShapeOptions,
                    onValueSelected = { viewModel.updateTextCursorShape(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                CustomImagePickerSection(
                    label = "Custom Image for Text Input Cursor",
                    currentPath = textCursorImage,
                    onImageSelected = { uri ->
                        coroutineScope.launch(Dispatchers.IO) {
                            val path = saveUriToInternalStorage(context, uri, "text_cursor.png")
                            viewModel.updateTextCursorImage(path)
                        }
                    },
                    onRemoveImage = { viewModel.updateTextCursorImage("") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Hover cursor
                ShapeDropdownSelector(
                    label = "Selection Hover Cursor Shape (Buttons & Links)",
                    selectedValue = hoverCursorShape,
                    options = hoverShapeOptions,
                    onValueSelected = { viewModel.updateHoverCursorShape(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                CustomImagePickerSection(
                    label = "Custom Image for Hover Cursor",
                    currentPath = hoverCursorImage,
                    onImageSelected = { uri ->
                        coroutineScope.launch(Dispatchers.IO) {
                            val path = saveUriToInternalStorage(context, uri, "hover_cursor.png")
                            viewModel.updateHoverCursorImage(path)
                        }
                    },
                    onRemoveImage = { viewModel.updateHoverCursorImage("") }
                )
            }
        }

        // --- SECTION 2: FLOATING PANEL SETTINGS ---
        SettingsSectionHeader(title = "Floating Control Panel")
        Card(
            colors = CardDefaults.cardColors(containerColor = BgTertiary),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Transparency Alpha
                Text(
                    text = "Panel Opacity (${(panelAlpha * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Slider(
                    value = panelAlpha,
                    onValueChange = { viewModel.updatePanelAlpha(it) },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = AccentPrimary,
                        thumbColor = AccentPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Floating Panel Size Setting
                SegmentedSelector(
                    label = "Floating Panel Size",
                    selectedValue = panelSize,
                    options = listOf(
                        Pair("SMALL", "Small"),
                        Pair("MEDIUM", "Medium"),
                        Pair("LARGE", "Large")
                    ),
                    onValueSelected = { viewModel.updatePanelSize(it) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Floating Panel Color Setting
                Text(
                    text = "Floating Panel Color Theme",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ColorPickerButton(
                        colorName = "DEFAULT",
                        colorVal = Color(0xFF1C1C28), // Slate Gray
                        isSelected = panelColor == "DEFAULT",
                        onClick = { viewModel.updatePanelColor("DEFAULT") }
                    )
                    ColorPickerButton(
                        colorName = "BLACK",
                        colorVal = Color(0xFF000000), // Pitch Black
                        isSelected = panelColor == "BLACK",
                        onClick = { viewModel.updatePanelColor("BLACK") }
                    )
                    ColorPickerButton(
                        colorName = "BLUE",
                        colorVal = Color(0xFF0A192F), // Navy/Blue
                        isSelected = panelColor == "BLUE",
                        onClick = { viewModel.updatePanelColor("BLUE") }
                    )
                    ColorPickerButton(
                        colorName = "RED",
                        colorVal = Color(0xFF2B1212), // Dark Red
                        isSelected = panelColor == "RED",
                        onClick = { viewModel.updatePanelColor("RED") }
                    )
                }
            }
        }

        // --- SECTION 3: CLICK TIMERS & HAPTICS ---
        SettingsSectionHeader(title = "Click & Haptic Settings")
        Card(
            colors = CardDefaults.cardColors(containerColor = BgTertiary),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Long Click duration
                Text(
                    text = "Long Click Duration ($longClickMs ms)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Slider(
                    value = longClickMs.toFloat(),
                    onValueChange = { viewModel.updateLongClickMs(it.toInt()) },
                    valueRange = 300f..1200f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = AccentPrimary,
                        thumbColor = AccentPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Double Click gap
                Text(
                    text = "Double Click Gap ($doubleClickMs ms)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Slider(
                    value = doubleClickMs.toFloat(),
                    onValueChange = { viewModel.updateDoubleClickMs(it.toInt()) },
                    valueRange = 80f..300f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = AccentPrimary,
                        thumbColor = AccentPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Vibration toggle
                SettingsToggleRow(
                    title = "Haptic Feedback (Vibration)",
                    checked = vibrationEnabled,
                    onCheckedChange = { viewModel.updateVibration(it) }
                )
            }
        }

        // --- SECTION 4: SCROLL SETTINGS ---
        SettingsSectionHeader(title = "Vertical Scroll Settings")
        Card(
            colors = CardDefaults.cardColors(containerColor = BgTertiary),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Scroll Speed
                Text(
                    text = "Scroll Speed (${String.format("%.1f", scrollSpeed)}x)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Slider(
                    value = scrollSpeed,
                    onValueChange = { viewModel.updateScrollSpeed(it) },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = AccentPrimary,
                        thumbColor = AccentPrimary
                    )
                )
            }
        }

        // --- SECTION 5: SYSTEM & LANGUAGE ---
        SettingsSectionHeader(title = "System & Language Settings")
        Card(
            colors = CardDefaults.cardColors(containerColor = BgTertiary),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Boot completed toggle
                SettingsToggleRow(
                    title = "Auto-start mouse service on boot",
                    checked = autoStart,
                    onCheckedChange = { viewModel.updateAutoStart(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reset Settings Button
                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reset Settings to Factory Default",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Reset settings confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = "Confirm Reset", color = TextPrimary) },
            text = { Text(text = "Are you sure you want to restore all configurations and custom cursors back to their factory defaults?", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetSettings()
                        showResetDialog = false
                    }
                ) {
                    Text(text = "Reset", color = ErrorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = "Cancel", color = TextSecondary)
                }
            },
            containerColor = BgSecondary
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = AccentSecondary,
            letterSpacing = 0.5.sp
        ),
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = AccentPrimary,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = BgTertiary
            )
        )
    }
}

@Composable
fun ColorPickerButton(
    colorName: String,
    colorVal: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colorVal)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) AccentPrimary else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
fun ShapeDropdownSelector(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onValueSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BgSecondary)
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayName = options.find { it.first == selectedValue }?.second ?: selectedValue
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = AccentPrimary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(BgSecondary)
                    .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
            ) {
                options.forEach { (value, displayName) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (value == selectedValue) AccentPrimary else TextPrimary,
                                fontWeight = if (value == selectedValue) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onValueSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomImagePickerSection(
    label: String,
    currentPath: String,
    onImageSelected: (Uri) -> Unit,
    onRemoveImage: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BgSecondary)
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentPath.isNotEmpty()) {
                val file = java.io.File(currentPath)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Custom Cursor: ${file.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onRemoveImage) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete custom image",
                        tint = Color.Red
                    )
                }
            } else {
                Text(
                    text = "No custom image selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { launcher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Select Image",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

fun saveUriToInternalStorage(context: android.content.Context, uri: Uri, fileName: String): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val file = java.io.File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

@Composable
fun SegmentedSelector(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onValueSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BgSecondary)
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(10.dp)),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEachIndexed { index, (value, display) ->
                val isSelected = selectedValue == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) AccentPrimary else Color.Transparent)
                        .clickable { onValueSelected(value) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = display,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
                if (index < options.size - 1 && !isSelected && selectedValue != options[index+1].first) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .align(Alignment.CenterVertically)
                            .background(BorderColor)
                    )
                }
            }
        }
    }
}
