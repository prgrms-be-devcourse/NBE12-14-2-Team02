package com.prgms.backend.domain.expense.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "expenses")
class Expense protected constructor() {
    @field:Id @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
    @field:Column(nullable = false)
    var meetingId: Long = 0
        protected set
    @field:Column(nullable = false)
    var payerMemberId: Long = 0
        protected set
    @field:Column(nullable = false, length = 255)
    var title: String = ""
        protected set
    @field:Column(nullable = false)
    var amount: Long = 0
        protected set
    @field:Column(length = 2000)
    var memo: String? = null
        protected set
    @field:Column(length = 500)
    var receiptKey: String? = null
        protected set
    @field:Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @field:ElementCollection
    @field:CollectionTable(name = "expense_participants", joinColumns = [JoinColumn(name = "expense_id")])
    @field:Column(name = "member_id", nullable = false)
    private var participantIds: MutableSet<Long> = hashSetOf()

    @field:ElementCollection
    @field:CollectionTable(name = "expense_shares", joinColumns = [JoinColumn(name = "expense_id")])
    @field:MapKeyColumn(name = "member_id")
    @field:Column(name = "share_amount", nullable = false)
    private var participantAmounts: MutableMap<Long, Long> = hashMapOf()

    @field:Column(length = 10)
    var splitMode: String? = null
        protected set
    var roundingUnit: Int? = null
        protected set
    var remainderMemberId: Long? = null
        protected set

    constructor(meetingId: Long, payerMemberId: Long, title: String, amount: Long,
                memo: String?, participantIds: Set<Long>) : this() {
        this.meetingId = meetingId
        this.payerMemberId = payerMemberId
        this.title = title.trim { Character.isWhitespace(it) }
        this.amount = amount
        this.memo = memo
        this.participantIds.addAll(participantIds)
    }

    fun getParticipantIds(): Set<Long> = java.util.Set.copyOf(participantIds)
    fun getParticipantAmounts(): Map<Long, Long> = java.util.Map.copyOf(participantAmounts)

    fun update(title: String, amount: Long, memo: String?, shares: Map<Long, Long>,
               splitMode: String?, roundingUnit: Int?, remainderMemberId: Long?) {
        this.title = title.trim { Character.isWhitespace(it) }
        this.amount = amount
        this.memo = memo
        participantIds.clear()
        participantIds.addAll(shares.keys)
        participantAmounts.clear()
        participantAmounts.putAll(shares)
        this.splitMode = splitMode
        this.roundingUnit = roundingUnit
        this.remainderMemberId = remainderMemberId
    }

    fun attachReceipt(storageKey: String?) { receiptKey = storageKey }

    @PrePersist
    protected fun onCreate() { createdAt = LocalDateTime.now() }

    companion object {
        @JvmStatic fun register(meetingId: Long, payerMemberId: Long, title: String, amount: Long,
                                memo: String?, shares: Map<Long, Long>, splitMode: String?,
                                roundingUnit: Int?, remainderMemberId: Long?): Expense =
            Expense(meetingId, payerMemberId, title, amount, memo, shares.keys).apply {
                participantAmounts.putAll(shares)
                this.splitMode = splitMode
                this.roundingUnit = roundingUnit
                this.remainderMemberId = remainderMemberId
            }
    }
}
