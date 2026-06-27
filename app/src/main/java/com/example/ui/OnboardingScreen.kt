package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.sin

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentPage by remember { mutableIntStateOf(0) }

    // Dynamic state polling for permissions
    var overlayGranted by remember { mutableStateOf(viewModel.hasOverlayPermission(context)) }
    var accessibilityGranted by remember { mutableStateOf(viewModel.hasAccessibilityPermission(context)) }
    var notificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Standard Android lifecycle observer simulation via polling to check if user returns with permissions granted
    LaunchedEffect(currentPage) {
        if (currentPage == 1) {
            while (true) {
                overlayGranted = viewModel.hasOverlayPermission(context)
                accessibilityGranted = viewModel.hasAccessibilityPermission(context)
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    // Notification Permission Launcher
    val requestNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            notificationGranted = isGranted
        }
    )

    Scaffold(
        containerColor = BgPrimary
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AccentGlow, BgPrimary),
                        center = Offset(200f, 300f),
                        radius = 1200f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header (App Brand Indicator)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PointerX",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                // Changing content based on current page
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            fadeIn() + slideInHorizontally() togetherWith fadeOut() + slideOutHorizontally()
                        },
                        label = "onboarding_pages"
                    ) { page ->
                        when (page) {
                            0 -> WelcomePage()
                            1 -> PermissionsPage(
                                context = context,
                                overlayGranted = overlayGranted,
                                accessibilityGranted = accessibilityGranted,
                                notificationGranted = notificationGranted,
                                onRequestOverlay = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                },
                                onRequestAccessibility = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                },
                                onRequestNotification = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                            2 -> TutorialPage()
                            3 -> ReadyPage()
                        }
                    }
                }

                // Footer Navigation controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Page indicator pills
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        repeat(4) { idx ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .height(6.dp)
                                    .width(if (idx == currentPage) 20.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (idx == currentPage) AccentPrimary else TextDisabled)
                            )
                        }
                    }

                    // Button controls
                    Button(
                        onClick = {
                            if (currentPage < 3) {
                                currentPage++
                            } else {
                                viewModel.completeFirstRun()
                                onComplete()
                            }
                        },
                        enabled = currentPage != 1 || (overlayGranted && accessibilityGranted),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = when (currentPage) {
                                0 -> "البدء والتعريف"
                                1 -> "استمر بعد التفعيل"
                                2 -> "فهمت طريقة التحكم"
                                else -> "تشغيل المؤشر واللوحة"
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    if (currentPage > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "رجوع",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { currentPage-- }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomePage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        // Glowing Canvas mouse drawing instead of unstable Lottie file
        var ticks by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) {
            while (true) {
                ticks += 0.05f
                kotlinx.coroutines.delay(16)
            }
        }

        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            val h = size.height
            val w = size.width
            val cx = w / 2f
            val cy = h / 2f

            // Drawing central glowing ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentPrimary.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = w * 0.45f
                )
            )

            // Drawing mouse grid
            val pulse = sin(ticks) * 5f
            drawRoundRect(
                color = AccentPrimary,
                topLeft = Offset(cx - 30.dp.toPx(), cy - 50.dp.toPx() + pulse),
                size = androidx.compose.ui.geometry.Size(60.dp.toPx(), 100.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            // Split line (mouse buttons)
            drawLine(
                color = AccentPrimary,
                start = Offset(cx, cy - 50.dp.toPx() + pulse),
                end = Offset(cx, cy - 10.dp.toPx() + pulse),
                strokeWidth = 2.dp.toPx()
            )

            // Dynamic Pointer cursor floating around
            val pointerX = cx + sin(ticks * 1.5f) * 40.dp.toPx()
            val pointerY = cy + sin(ticks) * 30.dp.toPx()

            val arrowPath = Path().apply {
                moveTo(pointerX, pointerY)
                lineTo(pointerX, pointerY + 18.dp.toPx())
                lineTo(pointerX + 5.dp.toPx(), pointerY + 13.dp.toPx())
                lineTo(pointerX + 11.dp.toPx(), pointerY + 22.dp.toPx())
                lineTo(pointerX + 13.dp.toPx(), pointerY + 21.dp.toPx())
                lineTo(pointerX + 7.dp.toPx(), pointerY + 12.dp.toPx())
                lineTo(pointerX + 13.dp.toPx(), pointerY + 12.dp.toPx())
                close()
            }
            drawPath(arrowPath, color = TextPrimary)
            drawPath(arrowPath, color = BgPrimary, style = Stroke(width = 1.5.dp.toPx()))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "مؤشر ماوس ذكي متكامل",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "PointerX يمنحك لوحة لمس عائمة ومؤشر ماوس دقيق يحاكي تجربة الكومبيوتر بالكامل لتسهيل استخدام هاتفك بيد واحدة أو كأداة تحكم متقدمة.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun PermissionsPage(
    context: Context,
    overlayGranted: Boolean,
    accessibilityGranted: Boolean,
    notificationGranted: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestNotification: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "الصلاحيات الأساسية المطلوبة",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "يحتاج التطبيق تفعيل هذه الميزات للعمل فوق التطبيقات وتنفيذ نقرات الماوس الذكية بنجاح.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Overlay Permission Card
        PermissionRowCard(
            title = "رسم نافذة عائمة (Overlay)",
            description = "مطلوب لعرض المؤشر ولوحة التحكم فوق جميع التطبيقات الأخرى.",
            isGranted = overlayGranted,
            onGrantClick = onRequestOverlay
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Accessibility Permission Card
        PermissionRowCard(
            title = "خدمة إمكانية الوصول (Accessibility)",
            description = "الخدمة الوحيدة التي تمكن التطبيق من إرسال النقرات والإيماءات بمسار سليم.",
            isGranted = accessibilityGranted,
            onGrantClick = onRequestAccessibility
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Notification Permission Card
        PermissionRowCard(
            title = "الإشعارات (Notifications)",
            description = "مطلوب لإظهار الإشعار الدائم الذي يمنع النظام من إغلاق خدمة الماوس.",
            isGranted = notificationGranted,
            onGrantClick = onRequestNotification
        )
    }
}

@Composable
fun PermissionRowCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgTertiary),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isGranted) SuccessColor else WarningColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (isGranted) {
                Text(
                    text = "مفعّل",
                    color = SuccessColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else {
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "منح", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TutorialPage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "كيفية التحكم والاستخدام",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Canvas of Touchpad dragging simulation
        var progress by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) {
            while (true) {
                progress += 0.02f
                kotlinx.coroutines.delay(16)
            }
        }

        Canvas(
            modifier = Modifier
                .size(width = 220.dp, height = 120.dp)
                .padding(8.dp)
        ) {
            val w = size.width
            val h = size.height

            // Touchpad box outline
            drawRoundRect(
                color = BgTertiary,
                topLeft = Offset(0f, 0f),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawRoundRect(
                color = AccentPrimary,
                topLeft = Offset(0f, 0f),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Animated finger trail and movement
            val fingerX = w / 2f + sin(progress) * (w * 0.35f)
            val fingerY = h / 2f + sin(progress * 2f) * (h * 0.25f)

            // Draw finger representation (glowing dot)
            drawCircle(
                color = AccentPrimary.copy(alpha = 0.4f),
                center = Offset(fingerX, fingerY),
                radius = 20.dp.toPx()
            )
            drawCircle(
                color = AccentSecondary,
                center = Offset(fingerX, fingerY),
                radius = 6.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TutorialRow(icon = Icons.Default.Favorite, text = "حرك إصبعك داخل منطقة اللمس الرمادية لتحريك المؤشر بدقة متناهية.")
        Spacer(modifier = Modifier.height(12.dp))
        TutorialRow(icon = Icons.Default.Star, text = "انقر نقرة سريعة في أي مكان داخل منطقة اللمس لتنفيذ نقرة يسار اعتيادية.")
        Spacer(modifier = Modifier.height(12.dp))
        TutorialRow(icon = Icons.Default.List, text = "اضغط DRAG أو SCROLL في شبكة الأزرار للتبديل لأوضاع السحب والتمرير السلس.")
    }
}

@Composable
fun TutorialRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BgTertiary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextPrimary,
                lineHeight = 20.sp
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ReadyPage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        // High-contrast checkmark canvas
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .padding(16.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            
            drawCircle(
                color = SuccessColor.copy(alpha = 0.2f),
                center = Offset(cx, cy),
                radius = size.width / 2f
            )
            
            drawCircle(
                color = SuccessColor,
                center = Offset(cx, cy),
                radius = size.width * 0.4f,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw Checkmark path
            val path = Path().apply {
                moveTo(cx - 15.dp.toPx(), cy - 2.dp.toPx())
                lineTo(cx - 4.dp.toPx(), cy + 9.dp.toPx())
                lineTo(cx + 16.dp.toPx(), cy - 11.dp.toPx())
            }
            drawPath(
                path = path,
                color = SuccessColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "أنت جاهز تمامًا للتحليق!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "تم ضبط كافة الإعدادات الأساسية. يمكنك تخصيص أحجام الماوس وحساسية السحب واختصارات لوحة المفاتيح والاهتزازات في شاشة الإعدادات لاحقًا.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
