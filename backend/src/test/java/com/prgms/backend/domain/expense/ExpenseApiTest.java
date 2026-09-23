package com.prgms.backend.domain.expense;

import com.prgms.backend.domain.expense.controller.ExpenseController;
import com.prgms.backend.domain.expense.dto.*;
import com.prgms.backend.domain.expense.exception.ExpenseRequestException;
import com.prgms.backend.domain.expense.service.ExpenseService;
import com.prgms.backend.domain.settlement.controller.SettlementController;
import com.prgms.backend.domain.settlement.service.SettlementService;
import com.prgms.backend.global.exception.GlobalExceptionHandler;
import com.prgms.backend.global.exception.custom.settlement.SettlementRequestException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.security.Principal;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// HTTP 매핑·JSON·검증·오류 응답
class ExpenseApiTest {
    private final ExpenseService expenses = mock(ExpenseService.class);
    private final SettlementService settlements = mock(SettlementService.class);
    private final Principal principal = () -> "user";
    private MockMvc mvc;
    private static final String URL = "/api/meetings/10/expenses";
    private static final String REQUEST = """
            {"title":"식사","amount":100,"splitMode":"EXACT","randomRemainder":false,
             "participants":[{"memberId":1,"amount":100}]}
            """;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new ExpenseController(expenses), new SettlementController(settlements))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test @DisplayName("등록 201, 목록·상세·수정 200, 삭제 204와 빈 본문")
    void successResponses() throws Exception {
        ExpenseResponse response = new ExpenseResponse(7L, 10L, 1L, "식사", 100, null, "EXACT", null,
                null, List.of(new ExpenseResponse.Share(1, 100)), null);
        when(expenses.create(eq(10L), eq(principal), any())).thenReturn(response);
        when(expenses.list(10, principal)).thenReturn(new ExpenseListResponse(1, true, true, List.of(response)));
        when(expenses.get(10, 7, principal)).thenReturn(response);
        when(expenses.update(eq(10L), eq(7L), eq(principal), any())).thenReturn(response);
        mvc.perform(post(URL).principal(principal).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.amount").value(100));
        mvc.perform(get(URL).principal(principal)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expenses[0].id").value(7));
        mvc.perform(get(URL + "/7").principal(principal)).andExpect(status().isOk());
        mvc.perform(put(URL + "/7").principal(principal).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.participants[0].amount").value(100));
        mvc.perform(delete(URL + "/7").principal(principal))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(expenses).delete(10, 7, principal);
    }

    @Test @DisplayName("음수 금액의 요청은 서비스 호출 전에 400으로 거절")
    void invalidRequest() throws Exception {
        mvc.perform(post(URL).principal(principal).contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST.replace("\"amount\":100", "\"amount\":-1")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(expenses);
    }

    @ParameterizedTest @ValueSource(ints = {400, 403, 404, 409})
    @DisplayName("업무 예외의 HTTP 상태와 메시지를 그대로 반환")
    void businessErrors(int code) throws Exception {
        doThrow(new ExpenseRequestException(code, "지출 변경 불가")).when(expenses).delete(10, 7, principal);
        mvc.perform(delete(URL + "/7").principal(principal)).andExpect(status().is(code))
                .andExpect(jsonPath("$.code").value(code)).andExpect(jsonPath("$.message").value("지출 변경 불가"));
    }

    @Test @DisplayName("정산 확정 권한 오류도 403으로 반환")
    void settlementPermission() throws Exception {
        when(settlements.confirm(10, principal)).thenThrow(new SettlementRequestException(403, "모임장만 확정 가능"));
        mvc.perform(post("/api/meetings/10/settlement/confirm").principal(principal))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.success").value(false));
    }
}
