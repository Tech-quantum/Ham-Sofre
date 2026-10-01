package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FoodItem
import com.example.data.model.Member

@Composable
fun MemberAvatar(
    member: Member,
    modifier: Modifier = Modifier,
    size: Int = 36
) {
    val bgColor = try {
        Color(android.graphics.Color.parseColor(member.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = member.avatarEmoji.ifBlank { member.name.take(1) },
            fontSize = (size * 0.45).sp,
            color = Color.White
        )
    }
}

@Composable
fun ExpiryBadge(item: FoodItem) {
    val now = System.currentTimeMillis()
    val daysLeft = item.daysUntilExpiry(now)
    val isExpired = item.isExpired(now)

    val (bgColor, textColor, text) = when {
        isExpired -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "منقضی شده!"
        )
        daysLeft <= 1L -> Triple(
            Color(0xFFFFE4E6),
            Color(0xFFBE123C),
            if (daysLeft == 0L) "امروز منقضی می‌شود!" else "۱ روز مانده"
        )
        daysLeft <= 3L -> Triple(
            Color(0xFFFEF3C7),
            Color(0xFFB45309),
            "$daysLeft روز مانده"
        )
        else -> Triple(
            Color(0xFFDCFCE7),
            Color(0xFF15803D),
            "$daysLeft روز باقی‌مانده"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpired || daysLeft <= 3L) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LocationBadge(location: String) {
    val (label, icon, color) = when (location) {
        "FRIDGE" -> Triple("یخچال", Icons.Default.Kitchen, Color(0xFF0284C7))
        "FREEZER" -> Triple("فریزر", Icons.Default.AcUnit, Color(0xFF0369A1))
        else -> Triple("کابینت", Icons.Default.Inventory2, Color(0xFFD97706))
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp
            )
        }
    }
}
