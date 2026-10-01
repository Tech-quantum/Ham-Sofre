package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_records")
data class PurchaseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Long, // in Tomans
    val paidByMemberId: Long,
    val paidByMemberName: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val beneficiaryMemberIds: String = "", // Comma-separated member IDs e.g. "1,2,3"
    val itemsSummary: String = "",
    val notes: String = "",
    val splitMode: String = "EQUAL", // EQUAL, SHARES, EXACT, ITEMIZED
    val customSharesData: String = "" // format "memberId:amount,memberId:amount"
) {
    fun getBeneficiaryIdsList(): List<Long> {
        if (beneficiaryMemberIds.isBlank()) return emptyList()
        return beneficiaryMemberIds.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
    }

    fun getMemberShares(allMembers: List<Member>): Map<Long, Long> {
        if (customSharesData.isNotBlank()) {
            val parsed = customSharesData.split(",").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val mId = parts[0].trim().toLongOrNull()
                    val share = parts[1].trim().toLongOrNull()
                    if (mId != null && share != null) mId to share else null
                } else null
            }.toMap()
            if (parsed.isNotEmpty()) return parsed
        }

        // Fallback to equal split among beneficiaries
        val beneficiaries = getBeneficiaryIdsList().ifEmpty { allMembers.map { it.id } }
        if (beneficiaries.isEmpty()) return emptyMap()
        val perPerson = amount / beneficiaries.size
        return beneficiaries.associateWith { perPerson }
    }
}
