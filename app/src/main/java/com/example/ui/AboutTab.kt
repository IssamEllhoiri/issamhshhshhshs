package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.sin

@Composable
fun AboutTab() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic vector-drawn Canvas logo representing PointerX
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(AccentGlow),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Draw radar ring
                drawCircle(
                    color = AccentPrimary,
                    center = Offset(cx, cy),
                    radius = size.width * 0.45f,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Cursor pointer path
                val arrowPath = Path().apply {
                    moveTo(cx - 5.dp.toPx(), cy - 12.dp.toPx())
                    lineTo(cx - 5.dp.toPx(), cy + 10.dp.toPx())
                    lineTo(cx + 2.dp.toPx(), cy + 3.dp.toPx())
                    lineTo(cx + 10.dp.toPx(), cy + 12.dp.toPx())
                    lineTo(cx + 13.dp.toPx(), cy + 9.dp.toPx())
                    lineTo(cx + 5.dp.toPx(), cy + 1.dp.toPx())
                    lineTo(cx + 13.dp.toPx(), cy + 1.dp.toPx())
                    close()
                }
                drawPath(arrowPath, color = TextPrimary)
                drawPath(arrowPath, color = BgPrimary, style = Stroke(width = 1.5.dp.toPx()))
            }
        }

        Text(
            text = "PointerX",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
        )

        Text(
            text = "تطبيق محاكاة مؤشر الماوس الاحترافي لنظام أندرويد",
            style = MaterialTheme.typography.bodyMedium.copy(color = AccentSecondary),
            textAlign = TextAlign.Center
        )

        Divider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

        // General info text block
        Text(
            text = "تم تصميم PointerX لحل مشكلة استخدام الهواتف ذات الشاشات الكبيرة بيد واحدة تمامًا. يوفر التطبيق لوحة لمس عائمة دقيقة متكاملة وذات استجابة سريعة جداً لرسم المؤشر وتنفيذ نقرات الكليك والمسح التمريري ومحاكاة السحب بأمان كامل ودون الحاجة لصلاحيات Root.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Features list inside a card
        Card(
            colors = CardDefaults.cardColors(containerColor = BgTertiary),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "المميزات والتقنيات البرمجية",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                AboutFeatureRow(icon = Icons.Default.Star, text = "دعم كامل لنواة الحظر وحواف الشاشة الذكية.")
                AboutFeatureRow(icon = Icons.Default.Refresh, text = "خوارزمية تسريع المؤشر التراكمي (Cursor Acceleration).")
                AboutFeatureRow(icon = Icons.Default.Notifications, text = "تغذية راجعة لمسية مخصصة بكل حدث (Haptic Effects).")
                AboutFeatureRow(icon = Icons.Default.Settings, text = "تخزين الإعدادات الآمن عبر Jetpack Preferences DataStore.")
                AboutFeatureRow(icon = Icons.Default.Lock, text = "أمان مطلق يحفظ خصوصية بيانات المستخدم 100%.")
            }
        }

        // Version card
        Card(
            colors = CardDefaults.cardColors(containerColor = BgTertiary),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "الإصدار الحالي: v1.0.0 (Build 35)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "تطوير فريق PointerX التقني. كافة الحقوق محفوظة © 2026",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
    }
}

@Composable
fun AboutFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
        )
    }
}
