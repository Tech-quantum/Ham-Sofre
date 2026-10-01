package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Member
import com.example.data.model.PurchaseRecord
import com.example.ui.MemberBalanceSummary
import java.text.SimpleDateFormat
import java.util.*

data class MonthlySpending(
    val monthLabel: String,
    val totalAmount: Long,
    val memberContributions: Map<Long, Long> // memberId -> amount
)

@Composable
fun DongAnalyticsView(
    purchases: List<PurchaseRecord>,
    members: List<Member>,
    balances: List<MemberBalanceSummary>,
    formatNumber: (Long) -> String
) {
    var selectedPeriod by remember { mutableStateOf("ALL") } // ALL, 30_DAYS, 7_DAYS

    val now = remember { System.currentTimeMillis() }
    val filteredPurchases = remember(purchases, selectedPeriod) {
        when (selectedPeriod) {
            "7_DAYS" -> purchases.filter { it.dateMillis >= now - (7L * 24 * 60 * 60 * 1000) }
            "30_DAYS" -> purchases.filter { it.dateMillis >= now - (30L * 24 * 60 * 60 * 1000) }
            else -> purchases
        }
    }

    val totalSpent = remember(filteredPurchases) {
        filteredPurchases.sumOf { it.amount }
    }

    // Monthly data aggregation
    val monthlyData = remember(purchases) {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("fa", "IR"))
        val monthGroup = purchases.groupBy { purchase ->
            dateFormat.format(Date(purchase.dateMillis))
        }

        monthGroup.map { (month, list) ->
            val monthTotal = list.sumOf { it.amount }
            val memberMap = mutableMapOf<Long, Long>()
            list.forEach { p ->
                val shares = p.getMemberShares(members)
                shares.forEach { (mId, amt) ->
                    memberMap[mId] = (memberMap[mId] ?: 0L) + amt
                }
            }
            MonthlySpending(
                monthLabel = month,
                totalAmount = monthTotal,
                memberContributions = memberMap
            )
        }.take(6)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Period Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedPeriod == "ALL",
                onClick = { selectedPeriod = "ALL" },
                label = { Text("کل دوره‌ها", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedPeriod == "30_DAYS",
                onClick = { selectedPeriod = "30_DAYS" },
                label = { Text("۳۰ روز اخیر", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedPeriod == "7_DAYS",
                onClick = { selectedPeriod = "7_DAYS" },
                label = { Text("۷ روز اخیر", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
        }

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مجموع هزینه‌های دُنگی در این بازه",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatNumber(totalSpent)} تومان",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(28.dp)
                    )
                }
            }
        }

        // Chart 1: Visual Member Contribution Bar & Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 سهم مشارکت و پرداخت هر فرد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Multi-segment Visual Progress Bar
                if (totalSpent > 0L) {
                    MemberContributionSegmentedBar(
                        filteredPurchases = filteredPurchases,
                        members = members
                    )
                } else {
                    Text(
                        text = "داده‌ای در این بازه زمانی وجود ندارد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Individual Contribution Rows
                members.forEach { member ->
                    val memberPaid = filteredPurchases.filter { it.paidByMemberId == member.id }.sumOf { it.amount }
                    val memberShare = filteredPurchases.sumOf { p ->
                        p.getMemberShares(members)[member.id] ?: 0L
                    }
                    val percentage = if (totalSpent > 0L) ((memberPaid.toDouble() / totalSpent) * 100).toInt() else 0

                    MemberContributionRow(
                        member = member,
                        paidAmount = memberPaid,
                        shareAmount = memberShare,
                        percentage = percentage,
                        formatNumber = formatNumber
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Chart 2: Monthly Spending Trend (Bar Chart Visual Summary)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📈 الگوی خرید و مخارج ماهانه (روند دُنگ)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (monthlyData.isNotEmpty()) {
                    MonthlyBarChartCanvas(
                        monthlyList = monthlyData,
                        formatNumber = formatNumber
                    )
                } else {
                    Text(
                        text = "هنوز خریدی در ماه‌های گذشته ثبت نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Chart 3: Donut / Circular Share Distribution
        if (totalSpent > 0L) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🥧 توزیع درصدی پرداخت هزینه‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ContributionDonutChart(
                            filteredPurchases = filteredPurchases,
                            members = members,
                            modifier = Modifier.size(130.dp)
                        )

                        // Legend
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            members.forEach { m ->
                                val memberPaid = filteredPurchases.filter { it.paidByMemberId == m.id }.sumOf { it.amount }
                                val pct = if (totalSpent > 0L) ((memberPaid.toDouble() / totalSpent) * 100).toInt() else 0
                                val mColor = try {
                                    Color(android.graphics.Color.parseColor(m.colorHex))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(mColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${m.name} ($pct%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberContributionSegmentedBar(
    filteredPurchases: List<PurchaseRecord>,
    members: List<Member>
) {
    val total = filteredPurchases.sumOf { it.amount }.coerceAtLeast(1L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        members.forEach { member ->
            val paid = filteredPurchases.filter { it.paidByMemberId == member.id }.sumOf { it.amount }
            val weight = (paid.toFloat() / total).coerceAtLeast(0f)
            val memberColor = try {
                Color(android.graphics.Color.parseColor(member.colorHex))
            } catch (_: Exception) {
                MaterialTheme.colorScheme.primary
            }

            if (weight > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight)
                        .background(memberColor)
                )
            }
        }
    }
}

@Composable
fun MemberContributionRow(
    member: Member,
    paidAmount: Long,
    shareAmount: Long,
    percentage: Int,
    formatNumber: (Long) -> String
) {
    val memberColor = try {
        Color(android.graphics.Color.parseColor(member.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(memberColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = memberColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "$percentage% از کل",
                    color = memberColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "پرداخت: ${formatNumber(paidAmount)} ت",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "سهم مصرف: ${formatNumber(shareAmount)} ت",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun MonthlyBarChartCanvas(
    monthlyList: List<MonthlySpending>,
    formatNumber: (Long) -> String,
    modifier: Modifier = Modifier
) {
    val maxVal = (monthlyList.maxOfOrNull { it.totalAmount } ?: 100000L).coerceAtLeast(1L)
    val primaryColor = MaterialTheme.colorScheme.primary
    val barTrackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            monthlyList.forEach { item ->
                val ratio = (item.totalAmount.toFloat() / maxVal).coerceIn(0.08f, 1f)
                val animatedRatio by animateFloatAsState(
                    targetValue = ratio,
                    animationSpec = tween(durationMillis = 600)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${(item.totalAmount / 1000)}k",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(100.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(barTrackColor),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedRatio)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(primaryColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.monthLabel.split(" ").firstOrNull() ?: item.monthLabel,
                        fontSize = 10.sp,
                        maxLines = 1,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ContributionDonutChart(
    filteredPurchases: List<PurchaseRecord>,
    members: List<Member>,
    modifier: Modifier = Modifier
) {
    val total = filteredPurchases.sumOf { it.amount }.coerceAtLeast(1L)

    Canvas(modifier = modifier) {
        var startAngle = -90f
        val strokeWidth = 24.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val arcSize = Size(diameter, diameter)

        members.forEach { m ->
            val memberPaid = filteredPurchases.filter { it.paidByMemberId == m.id }.sumOf { it.amount }
            val sweep = (memberPaid.toFloat() / total) * 360f
            val color = try {
                Color(android.graphics.Color.parseColor(m.colorHex))
            } catch (_: Exception) {
                Color.Gray
            }

            if (sweep > 0f) {
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
    }
}
