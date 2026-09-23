package com.prgms.backend.domain.expense.dto;

import java.util.List;

// 버튼 표시의 기준도 서버에서 전달합니다. 변경 요청의 권한은 서버에서 다시 검사합니다.
public record ExpenseListResponse(long currentMemberId, boolean leader, boolean editable,
                                  List<ExpenseResponse> expenses) {}
