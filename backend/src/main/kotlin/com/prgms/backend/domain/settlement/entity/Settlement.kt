package com.prgms.backend.domain.settlement.entity

import com.prgms.backend.global.exception.custom.settlement.SettlementAlreadyDoneException
import com.prgms.backend.global.exception.custom.settlement.SettlementRequestException
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "settlements", uniqueConstraints = [UniqueConstraint(columnNames = ["meeting_id"])])
class Settlement protected constructor() {
    @field:Id @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
    @field:Column(name = "meeting_id", nullable = false)
    var meetingId: Long = 0
        protected set
    @field:Enumerated(EnumType.STRING) @field:Column(nullable = false, length = 10)
    var status: SettlementStatus = SettlementStatus.OPEN
        protected set
    var closedByMemberId: Long? = null
        protected set
    var closedAt: LocalDateTime? = null
        protected set
    @field:Version
    var version: Long? = null
        protected set

    @field:ElementCollection
    @field:CollectionTable(name = "settlement_balances", joinColumns = [JoinColumn(name = "settlement_id")])
    @field:OrderColumn(name = "balance_order")
    var balances: MutableList<SettlementBalance> = arrayListOf()
        protected set
    @field:ElementCollection
    @field:CollectionTable(name = "settlement_transfers", joinColumns = [JoinColumn(name = "settlement_id")])
    @field:OrderColumn(name = "transfer_order")
    var transfers: MutableList<SettlementTransfer> = arrayListOf()
        protected set
    @field:ElementCollection
    @field:CollectionTable(name = "settlement_account_snapshots", joinColumns = [JoinColumn(name = "settlement_id")])
    @field:OrderColumn(name = "account_order")
    var accounts: MutableList<SettlementAccountSnapshot> = arrayListOf()
        protected set

    constructor(meetingId: Long) : this() {
        require(meetingId > 0) { "모임 ID가 필요합니다." }
        this.meetingId = meetingId
    }

    fun checkOpen() {
        if (status == SettlementStatus.CLOSED) throw SettlementAlreadyDoneException()
    }

    fun saveResult(balances: List<SettlementBalance>, transfers: List<SettlementTransfer>) {
        checkOpen()
        this.balances.clear()
        this.balances.addAll(balances)
        this.transfers.clear()
        this.transfers.addAll(transfers)
    }

    fun saveAccounts(accounts: List<SettlementAccountSnapshot>) {
        checkOpen()
        this.accounts.clear()
        this.accounts.addAll(accounts)
    }

    // 과거 계좌 누락 확정 데이터의 보충 경로를 유지합니다.
    fun canRegisterMissingAccount(memberId: Long): Boolean = status == SettlementStatus.CLOSED &&
        transfers.any { it.recipientId == memberId } && accounts.none { it.memberId == memberId }

    fun registerMissingAccount(account: SettlementAccount) {
        if (!canRegisterMissingAccount(account.memberId))
            throw SettlementRequestException(409, "확정 후에는 미등록 수취인의 계좌만 등록할 수 있습니다.")
        accounts.add(SettlementAccountSnapshot(account))
    }

    fun close(leaderMemberId: Long) {
        require(leaderMemberId > 0) { "모임장 ID가 필요합니다." }
        checkOpen()
        status = SettlementStatus.CLOSED
        closedByMemberId = leaderMemberId
        closedAt = LocalDateTime.now()
    }
}
