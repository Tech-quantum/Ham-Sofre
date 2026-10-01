package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Member
import com.example.data.model.SettlementRecord
import com.example.ui.DebtTransfer
import com.example.ui.MainViewModel
import com.example.ui.MemberBalanceSummary
import com.example.ui.components.MemberAvatar
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettlementScreen(
    viewModel: MainViewModel,
    balances: List<MemberBalanceSummary>,
    debtTransfers: List<DebtTransfer>,
    settlements: List<SettlementRecord>,
    members: List<Member>
) {
    var transferToSettle by remember { mutableStateOf<DebtTransfer?>(null) }
    var showManualSettleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Balance Overview Cards
        Text(
            text = "وضعیت حساب و دُنگ اعضا",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Member Balances
            items(balances, key = { it.member.id }) { balance ->
                MemberBalanceCard(balance = balance, formatNumber = viewModel::formatNumber)
            }

            // Smart Settlement Plan
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "پیشنهاد تسویه بهینه (چه کسی به چه کسی؟)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { showManualSettleDialog = true }) {
                        Text("تسویه دستی", fontSize = 12.sp)
                    }
                }
            }

            if (debtTransfers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "همه حساب‌ها صاف و تسویه شده است! 🎉",
                                color = Color(0xFF166534),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                items(debtTransfers) { transfer ->
                    DebtTransferCard(
                        transfer = transfer,
                        formatNumber = viewModel::formatNumber,
                        onSettleClick = { transferToSettle = transfer }
                    )
                }
            }

            // Past Settlement History
            if (settlements.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "تاریخچه تسویه‌حساب‌ها",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                items(settlements, key = { it.id }) { s ->
                    PastSettlementRow(
                        settlement = s,
                        formatNumber = viewModel::formatNumber,
                        onDelete = { viewModel.deleteSettlement(s) }
                    )
                }
            }
        }
    }

    // Direct Settle Dialog
    transferToSettle?.let { transfer ->
        AlertDialog(
            onDismissRequest = { transferToSettle = null },
            title = { Text("ثبت تسویه حساب") },
            text = {
                Text(
                    text = "آیا پرداخت مبلغ ${viewModel.formatNumber(transfer.amount)} تومان از طرف «${transfer.fromMemberName}» به «${transfer.toMemberName}» را تأیید می‌کنید؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fromM = members.find { it.id == transfer.fromMemberId }
                        val toM = members.find { it.id == transfer.toMemberId }
                        if (fromM != null && toM != null) {
                            viewModel.recordSettlement(fromM, toM, transfer.amount)
                        }
                        transferToSettle = null
                    }
                ) {
                    Text("بله، تسویه شد")
                }
            },
            dismissButton = {
                TextButton(onClick = { transferToSettle = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Manual Settlement Dialog
    if (showManualSettleDialog) {
        ManualSettleDialog(
            members = members,
            onDismiss = { showManualSettleDialog = false },
            onConfirm = { from, to, amount ->
                viewModel.recordSettlement(from, to, amount)
                showManualSettleDialog = false
            }
        )
    }
}

@Composable
fun MemberBalanceCard(
    balance: MemberBalanceSummary,
    formatNumber: (Long) -> String
) {
    val isCreditor = balance.netBalance > 0
    val isSettled = balance.netBalance == 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(member = balance.member, size = 44)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = balance.member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "پرداخت: ${formatNumber(balance.totalPaid)} | مصرف: ${formatNumber(balance.totalShare)} تومان",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val balanceColor = when {
                    isCreditor -> Color(0xFF16A34A)
                    isSettled -> MaterialTheme.colorScheme.outline
                    else -> Color(0xFFDC2626)
                }
                val label = when {
                    isCreditor -> "طلبکار (+)"
                    isSettled -> "تسویه"
                    else -> "بدهکار (-)"
                }

                Text(
                    text = "${if (isCreditor) "+" else ""}${formatNumber(balance.netBalance)} تومان",
                    fontWeight = FontWeight.Bold,
                    color = balanceColor,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = balanceColor
                )
            }
        }
    }
}

@Composable
fun DebtTransferCard(
    transfer: DebtTransfer,
    formatNumber: (Long) -> String,
    onSettleClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transfer.fromMemberName,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                    Text(" ➔ پرداخت به ➔ ", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = transfer.toMemberName,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatNumber(transfer.amount)} تومان",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onSettleClick,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("تسویه شد", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PastSettlementRow(
    settlement: SettlementRecord,
    formatNumber: (Long) -> String,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "«${settlement.fromMemberName}» پرداخت به «${settlement.toMemberName}»",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${formatNumber(settlement.amount)} تومان در ${dateFormat.format(Date(settlement.timestampMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "حذف تسویه",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSettleDialog(
    members: List<Member>,
    onDismiss: () -> Unit,
    onConfirm: (from: Member, to: Member, amount: Long) -> Unit
) {
    var fromMember by remember { mutableStateOf(members.firstOrNull()) }
    var toMember by remember { mutableStateOf(members.getOrNull(1) ?: members.firstOrNull()) }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت تسویه مستقیم") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("پرداخت‌کننده:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    members.forEach { m ->
                        FilterChip(
                            selected = fromMember?.id == m.id,
                            onClick = { fromMember = m },
                            label = { Text(m.name) }
                        )
                    }
                }

                Text("دریافت‌کننده:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    members.forEach { m ->
                        FilterChip(
                            selected = toMember?.id == m.id,
                            onClick = { toMember = m },
                            label = { Text(m.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ تسویه (تومان)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val from = fromMember ?: return@Button
                    val to = toMember ?: return@Button
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (amount > 0L && from.id != to.id) {
                        onConfirm(from, to, amount)
                    }
                }
            ) {
                Text("ثبت تسویه")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
