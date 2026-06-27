package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.PointerOverlayService
import com.example.ui.AboutTab
import com.example.ui.HomeTab
import com.example.ui.MainViewModel
import com.example.ui.OnboardingScreen
import com.example.ui.SettingsTab
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val firstRun by viewModel.firstRun.collectAsState()

                if (firstRun) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onComplete = {
                            // Onboarding completed, proceed
                        }
                    )
                } else {
                    MainAppLayout(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableIntStateOf(0) }
    val isRunning by viewModel.isOverlayActive.collectAsState()
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var dialogPermissionType by remember { mutableStateOf("") } // "OVERLAY" or "ACCESSIBILITY"

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgSecondary,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = AccentPrimary
                ),
                title = {
                    Text(
                        text = "PointerX",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isRunning) "مفعّل" else "متوقف",
                            color = if (isRunning) SuccessColor else TextSecondary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = isRunning,
                            onCheckedChange = { start ->
                                toggleService(context, start, viewModel) { type ->
                                    dialogPermissionType = type
                                    showPermissionDialog = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = AccentPrimary,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = BgTertiary
                            )
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BgSecondary,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                    label = { Text(text = "الرئيسية", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        selectedTextColor = TextPrimary,
                        indicatorColor = AccentPrimary,
                        unselectedIconColor = TextDisabled,
                        unselectedTextColor = TextDisabled
                    )
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                    label = { Text(text = "الإعدادات", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        selectedTextColor = TextPrimary,
                        indicatorColor = AccentPrimary,
                        unselectedIconColor = TextDisabled,
                        unselectedTextColor = TextDisabled
                    )
                )

                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
                    label = { Text(text = "عن التطبيق", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        selectedTextColor = TextPrimary,
                        indicatorColor = AccentPrimary,
                        unselectedIconColor = TextDisabled,
                        unselectedTextColor = TextDisabled
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) + slideInVertically(initialOffsetY = { 8 }) togetherWith
                            fadeOut(animationSpec = tween(150))
                },
                label = "navigation_tab_animation"
            ) { tab ->
                when (tab) {
                    0 -> HomeTab(
                        viewModel = viewModel,
                        onToggleService = { start ->
                            toggleService(context, start, viewModel) { type ->
                                dialogPermissionType = type
                                showPermissionDialog = true
                            }
                        }
                    )
                    1 -> SettingsTab(viewModel = viewModel)
                    2 -> AboutTab()
                }
            }
        }
    }

    // Custom explanatory Permission Assist dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    text = if (dialogPermissionType == "OVERLAY") "تفعيل ميزة النافذة العائمة" else "تفعيل خدمة إمكانية الوصول",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (dialogPermissionType == "OVERLAY") {
                        "يحتاج PointerX لإذن 'العرض فوق التطبيقات الأخرى' ليتمكن من رسم الماوس واللوحة العائمة على شاشتك بنجاح. يرجى تفعيل الخيار من قائمة النظام."
                    } else {
                        "من أجل تنفيذ نقرات الماوس، الضغط المطول، التمرير التلقائي، والسحب بأمان بدون روت، يرجى تفعيل خدمة 'PointerX' في قائمة إمكانية الوصول في هاتفك."
                    },
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        if (dialogPermissionType == "OVERLAY") {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "افتح الإعدادات لتفعيلها", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(text = "إلغاء", color = TextSecondary)
                }
            },
            containerColor = BgSecondary
        )
    }
}

private fun toggleService(
    context: Context,
    start: Boolean,
    viewModel: MainViewModel,
    onPermissionRequired: (String) -> Unit
) {
    if (start) {
        // Check overlay permission
        if (!viewModel.hasOverlayPermission(context)) {
            onPermissionRequired("OVERLAY")
            return
        }

        // Check accessibility service
        if (!viewModel.hasAccessibilityPermission(context)) {
            onPermissionRequired("ACCESSIBILITY")
            return
        }

        // Both granted, launch overlay service
        val intent = Intent(context, PointerOverlayService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "فشل بدء الخدمة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    } else {
        // Stop service
        val intent = Intent(context, PointerOverlayService::class.java)
        context.stopService(intent)
    }
}
