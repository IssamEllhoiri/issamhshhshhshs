package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeTab(
    viewModel: MainViewModel,
    onToggleService: (Boolean) -> Unit
) {
    val isRunning by viewModel.isOverlayActive.collectAsState()
    val clicks by viewModel.clicksToday.collectAsState()
    val drags by viewModel.dragCount.collectAsState()
    val distance by viewModel.pointerDistance.collectAsState()

    // Central tips list
    val tipsList = remember {
        listOf(
            "يمكنك سحب شريط العنوان في اللوحة العائمة لنقلها لأي موضع تريده.",
            "فعل وضع SCROLL واسحب إصبعك على منطقة اللمس للتمرير العمودي السلس.",
            "تلتصق اللوحة العائمة تلقائياً بحواف الشاشة اليسرى أو اليمنى بمجرد إفلاتها.",
            "اضغط DRAG لتفعيل وضع السحب، ثم انقر مرتين لتحديد البداية والنهاية.",
            "يمكنك تغيير ألوان المؤشر (بنفسجي، أزرق، أحمر) من شاشة الإعدادات مباشرة.",
            "عند تفعيل موفر البطارية، ينخفض معدل تحديث إطارات المؤشر لتوفير الطاقة.",
            "يدعم التطبيق التشغيل التلقائي الآمن مع إقلاع هاتفك دون تدخل منك.",
            "انقر LNG-CLK لتنفيذ نقرة طويلة (مثالية للرموز والوسائط والتحديد).",
            "انقر KBD في الأزرار لإظهار أو إخفاء الكيبورد الافتراضي بمرونة.",
            "تحقق من إعدادات شدة الاهتزاز إذا كنت تفضل تغذية راجعة لمسية قوية."
        )
    }

    var activeTip by remember { mutableStateOf(tipsList[0]) }
    LaunchedEffect(Unit) {
        activeTip = tipsList.random()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Status Hero Banner Card (140dp)
        val containerColor by animateColorAsState(
            targetValue = if (isRunning) AccentPrimary else BgSecondary,
            label = "banner_color"
        )
        val gradientBrush = if (isRunning) {
            Brush.linearGradient(
                colors = listOf(AccentPrimary, Color(0xFF4C44CC)),
                start = Offset(0f, 0f),
                end = Offset(1000f, 1000f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(BgSecondary, BgTertiary),
                start = Offset(0f, 0f),
                end = Offset(1000f, 1000f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(gradientBrush)
                .clickable { onToggleService(!isRunning) }
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isRunning) "حالة التشغيل: مفعّل" else "حالة التشغيل: متوقف",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    // Infinite pulsing dot
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot")
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(750, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(if (isRunning) dotScale else 1.0f)
                            .clip(CircleShape)
                            .background(if (isRunning) SuccessColor else ErrorColor)
                    )
                }

                Text(
                    text = if (isRunning) 
                        "المؤشر العائم ولوحة التحكم جاهزان للاستخدام. انقر هنا للإيقاف." 
                    else 
                        "انقر هنا لتفعيل الماوس واللوحة العائمة فوراً فوق جميع التطبيقات.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isRunning) TextPrimary.copy(alpha = 0.8f) else TextSecondary,
                        lineHeight = 16.sp
                    )
                )
            }
        }

        // 2. Statistics Grid (2x2)
        Text(
            text = "إحصائيات الجلسة الحالية",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "نقرات اليوم",
                value = "$clicks",
                icon = Icons.Default.Star,
                accentColor = SuccessColor
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "عمليات السحب",
                value = "$drags",
                icon = Icons.Default.PlayArrow,
                accentColor = InfoColor
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "مسافة المؤشر",
                value = String.format("%.1f م", distance * 0.026), // Pixel travel converted roughly to meters
                icon = Icons.Default.Check,
                accentColor = WarningColor
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "لغة التطبيق",
                value = "عربي",
                icon = Icons.Default.Menu,
                accentColor = AccentSecondary
            )
        }

        // 3. Quick Access Shortcut Row
        Text(
            text = "تخصيص سريع مجسم",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        val sizePreference by viewModel.cursorSize.collectAsState()
        val colorPreference by viewModel.cursorColor.collectAsState()
        val sensitivityPreference by viewModel.cursorSensitivity.collectAsState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAccessItem(
                label = "حجم المؤشر: $sizePreference",
                icon = Icons.Default.Home,
                onClick = {
                    val nextSize = when (sizePreference) {
                        24 -> 32
                        32 -> 40
                        else -> 24
                    }
                    viewModel.updateCursorSize(nextSize)
                }
            )

            QuickAccessItem(
                label = "حساسية الحركة: ${String.format("%.1f", sensitivityPreference)}",
                icon = Icons.Default.PlayArrow,
                onClick = {
                    val nextSensitivity = when (sensitivityPreference) {
                        2.0f -> 3.0f
                        3.0f -> 4.0f
                        else -> 2.0f
                    }
                    viewModel.updateCursorSensitivity(nextSensitivity)
                }
            )

            QuickAccessItem(
                label = "اللون: $colorPreference",
                icon = Icons.Default.Favorite,
                onClick = {
                    val nextColor = when (colorPreference) {
                        "WHITE" -> "PURPLE"
                        "PURPLE" -> "BLUE"
                        else -> "WHITE"
                    }
                    viewModel.updateCursorColor(nextColor)
                }
            )
        }

        // 4. Custom Tip Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BgTertiary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentGlow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نصيحة الاستخدام الذكي",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeTip,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgTertiary),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun QuickAccessItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = BgSecondary,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }
    }
}
