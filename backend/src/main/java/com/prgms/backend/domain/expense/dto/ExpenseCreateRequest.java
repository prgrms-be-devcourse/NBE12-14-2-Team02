package com.prgms.backend.domain.expense.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record ExpenseCreateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull @DecimalMin("1") @Digits(integer = 12, fraction = 0) BigDecimal amount,
        @Size(max = 2000) String memo,
        @NotNull SplitMode splitMode,
        Integer roundingUnit,
        @Positive Long remainderMemberId,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid Participant> participants,
        boolean randomRemainder
) {
    public ExpenseCreateRequest(String title, BigDecimal amount, String memo, SplitMode splitMode,
                                Integer roundingUnit, Long remainderMemberId, List<Participant> participants) {
        this(title, amount, memo, splitMode, roundingUnit, remainderMemberId, participants, false);
    }
    public enum SplitMode { EQUAL, EXACT }
    public record Participant(
            @NotNull @Positive Long memberId,
            @DecimalMin("0") @Digits(integer = 12, fraction = 0) BigDecimal amount
    ) {}
}
