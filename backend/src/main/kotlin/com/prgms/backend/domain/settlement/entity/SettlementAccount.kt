package com.prgms.backend.domain.settlement.entity

import jakarta.persistence.*

@Entity
@Table(name = "settlement_accounts", uniqueConstraints = [UniqueConstraint(columnNames = ["meeting_id", "member_id"])])
class SettlementAccount protected constructor() {
    @field:Id @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
    @field:Column(name = "meeting_id", nullable = false)
    var meetingId: Long = 0
        protected set
    @field:Column(name = "member_id", nullable = false)
    var memberId: Long = 0
        protected set
    @field:Column(nullable = false, length = 100)
    var bankName: String = ""
        protected set
    @field:Column(nullable = false, length = 50)
    var accountNumber: String = ""
        protected set
    @field:Column(nullable = false, length = 100)
    var accountHolder: String = ""
        protected set

    constructor(meetingId: Long, memberId: Long, bankName: String?, accountNumber: String?, accountHolder: String?) : this() {
        require(meetingId > 0 && memberId > 0) { "모임/모임원 ID가 필요합니다." }
        require(!bankName.isNullOrBlank() && bankName.length <= 100 &&
            !accountHolder.isNullOrBlank() && accountHolder.length <= 100 &&
            accountNumber != null && accountNumber.length <= 50 && accountNumber.matches(Regex("[0-9]+(-[0-9]+)*"))) {
            "정산 계좌 정보를 확인해주세요."
        }
        this.meetingId = meetingId
        this.memberId = memberId
        this.bankName = bankName.trim { Character.isWhitespace(it) }
        this.accountNumber = accountNumber
        this.accountHolder = accountHolder.trim { Character.isWhitespace(it) }
    }

    fun update(bankName: String?, accountNumber: String?, accountHolder: String?) {
        val checked = SettlementAccount(meetingId, memberId, bankName, accountNumber, accountHolder)
        this.bankName = checked.bankName
        this.accountNumber = checked.accountNumber
        this.accountHolder = checked.accountHolder
    }
}
