package com.prgms.backend.domain.settlement.entity

import jakarta.persistence.*

@Embeddable
class SettlementAccountSnapshot protected constructor() {
    var memberId: Long = 0
        protected set
    @field:Column(length = 100)
    var bankName: String = ""
        protected set
    @field:Column(length = 50)
    var accountNumber: String = ""
        protected set
    @field:Column(length = 100)
    var accountHolder: String = ""
        protected set

    constructor(account: SettlementAccount) : this() {
        memberId = account.memberId
        bankName = account.bankName
        accountNumber = account.accountNumber
        accountHolder = account.accountHolder
    }
}
