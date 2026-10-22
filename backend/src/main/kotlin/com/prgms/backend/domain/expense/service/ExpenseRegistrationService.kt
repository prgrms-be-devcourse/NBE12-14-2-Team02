package com.prgms.backend.domain.expense.service

import com.prgms.backend.domain.expense.dto.*
import com.prgms.backend.domain.expense.entity.ExpenseRegistration
import com.prgms.backend.domain.expense.exception.ExpenseRequestException
import com.prgms.backend.domain.expense.repository.*
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort
import jakarta.validation.Validator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.Principal
import java.util.HexFormat
import java.util.Locale

@Service
class ExpenseRegistrationService(
    private val expenses: ExpenseService,
    private val access: MeetingAccessPort,
    private val expenseRepository: ExpenseRepository,
    private val registrations: ExpenseRegistrationRepository,
    private val validator: Validator
) {
    @Transactional
    fun create(meetingId: Long, principal: Principal?, request: ExpenseCreateRequest?, requestKey: String?): ExpenseResponse {
        if (meetingId <= 0 || requestKey == null || !requestKey.matches(Regex("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}")))
            throw ExpenseRequestException(400, "모임과 등록 요청 번호를 확인해주세요.")
        val context = access.requireMember(meetingId, principal)
        if (request == null || validator.validate(request).isNotEmpty()) throw ExpenseRequestException(400, "지출 입력값을 확인해주세요.")
        val key = requestKey.lowercase(Locale.ROOT)
        val fingerprint = fingerprint(request)
        val previous = registrations.findByMeetingIdAndMemberIdAndRequestKey(meetingId, context.memberId, key).orElse(null)
        if (previous != null) {
            if (previous.fingerprint != fingerprint) throw ExpenseRequestException(409, "같은 등록 요청의 내용이 달라졌습니다. 기존 지출을 확인해주세요.")
            val saved = expenseRepository.findById(previous.expenseId)
                .orElseThrow { ExpenseRequestException(410, "이미 등록 후 삭제된 지출입니다. 목록을 확인해주세요.") }
            return ExpenseResponse.from(saved)
        }
        val saved = expenses.create(meetingId, principal, request)
        registrations.save(ExpenseRegistration(meetingId, context.memberId, key, fingerprint, requireNotNull(saved.id)))
        return saved
    }

    private fun fingerprint(request: ExpenseCreateRequest): String {
        try {
            val bytes = ByteArrayOutputStream()
            val output = DataOutputStream(bytes)
            writeValue(output, request.title)
            writeValue(output, requireNotNull(request.amount).stripTrailingZeros().toPlainString())
            writeValue(output, request.memo)
            writeValue(output, requireNotNull(request.splitMode).name)
            writeValue(output, request.roundingUnit)
            writeValue(output, request.remainderMemberId)
            output.writeBoolean(request.randomRemainder)
            for (participant in requireNotNull(request.participants)) {
                val checked = requireNotNull(participant)
                output.writeLong(requireNotNull(checked.memberId))
                writeValue(output, checked.amount?.stripTrailingZeros()?.toPlainString())
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()))
        } catch (exception: IOException) {
            throw IllegalStateException("등록 요청 확인에 실패했습니다.", exception)
        } catch (exception: NoSuchAlgorithmException) {
            throw IllegalStateException("등록 요청 확인에 실패했습니다.", exception)
        }
    }

    private fun writeValue(output: DataOutputStream, value: Any?) {
        output.writeBoolean(value != null)
        if (value != null) output.writeUTF(value.toString())
    }
}
