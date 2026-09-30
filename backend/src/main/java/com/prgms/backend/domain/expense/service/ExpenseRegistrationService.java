package com.prgms.backend.domain.expense.service;

import com.prgms.backend.domain.expense.dto.ExpenseCreateRequest;
import com.prgms.backend.domain.expense.dto.ExpenseResponse;
import com.prgms.backend.domain.expense.entity.ExpenseRegistration;
import com.prgms.backend.domain.expense.exception.ExpenseRequestException;
import com.prgms.backend.domain.expense.repository.ExpenseRegistrationRepository;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ExpenseRegistrationService {
    private final ExpenseService expenses;
    private final MeetingAccessPort access;
    private final ExpenseRepository expenseRepository;
    private final ExpenseRegistrationRepository registrations;
    private final Validator validator;

    @Transactional
    public ExpenseResponse create(long meetingId, Principal principal, ExpenseCreateRequest request, String requestKey) {
        if (meetingId <= 0 || requestKey == null || !requestKey.matches("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}")) {
            throw new ExpenseRequestException(400, "모임과 등록 요청 번호를 확인해주세요.");
        }
        var context = access.requireMember(meetingId, principal);
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new ExpenseRequestException(400, "지출 입력값을 확인해주세요.");
        }
        String key = requestKey.toLowerCase(Locale.ROOT);
        String fingerprint = fingerprint(request);
        var previous = registrations.findByMeetingIdAndMemberIdAndRequestKey(meetingId, context.memberId(), key);
        if (previous.isPresent()) {
            var registration = previous.get();
            if (!registration.getFingerprint().equals(fingerprint)) {
                throw new ExpenseRequestException(409, "같은 등록 요청의 내용이 달라졌습니다. 기존 지출을 확인해주세요.");
            }
            var saved = expenseRepository.findById(registration.getExpenseId())
                    .orElseThrow(() -> new ExpenseRequestException(410, "이미 등록 후 삭제된 지출입니다. 목록을 확인해주세요."));
            return ExpenseResponse.from(saved);
        }
        // 지출 서비스와 동일한 트랜잭션·모임 잠금 안에서 요청 번호까지 함께 저장합니다.
        ExpenseResponse saved = expenses.create(meetingId, principal, request);
        registrations.save(new ExpenseRegistration(meetingId, context.memberId(), key, fingerprint, saved.id()));
        return saved;
    }

    private String fingerprint(ExpenseCreateRequest request) {
        try {
            var bytes = new ByteArrayOutputStream();
            var output = new DataOutputStream(bytes);
            writeValue(output, request.title());
            writeValue(output, request.amount().stripTrailingZeros().toPlainString());
            writeValue(output, request.memo());
            writeValue(output, request.splitMode().name());
            writeValue(output, request.roundingUnit());
            writeValue(output, request.remainderMemberId());
            output.writeBoolean(request.randomRemainder());
            for (var participant : request.participants()) {
                output.writeLong(participant.memberId());
                writeValue(output, participant.amount() == null ? null : participant.amount().stripTrailingZeros().toPlainString());
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("등록 요청 확인에 실패했습니다.", e);
        }
    }

    private void writeValue(DataOutputStream output, Object value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) output.writeUTF(value.toString());
    }
}
