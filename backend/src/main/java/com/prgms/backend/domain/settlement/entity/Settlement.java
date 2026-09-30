package com.prgms.backend.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.prgms.backend.global.exception.custom.settlement.SettlementAlreadyDoneException;

@Entity
@Table(name = "settlements", uniqueConstraints = @UniqueConstraint(columnNames = "meeting_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SettlementStatus status = SettlementStatus.OPEN;
    private Long closedByMemberId;
    private LocalDateTime closedAt;
    @Version
    private Long version;

    @ElementCollection
    @CollectionTable(name = "settlement_balances", joinColumns = @JoinColumn(name = "settlement_id"))
    @OrderColumn(name = "balance_order")
    private List<SettlementBalance> balances = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "settlement_transfers", joinColumns = @JoinColumn(name = "settlement_id"))
    @OrderColumn(name = "transfer_order")
    private List<SettlementTransfer> transfers = new ArrayList<>();

    public void checkOpen() {
        if (status == SettlementStatus.CLOSED) {
            throw new SettlementAlreadyDoneException();
        }
    }

    @ElementCollection
    @CollectionTable(name = "settlement_account_snapshots", joinColumns = @JoinColumn(name = "settlement_id"))
    @OrderColumn(name = "account_order")
    private List<SettlementAccountSnapshot> accounts = new ArrayList<>();

    public void saveAccounts(List<SettlementAccountSnapshot> accounts) {
        checkOpen();
        this.accounts.clear();
        this.accounts.addAll(accounts);
    }

    public boolean canRegisterMissingAccount(long memberId) {
        return status == SettlementStatus.CLOSED
                && transfers.stream().anyMatch(transfer -> transfer.getRecipientId() == memberId)
                && accounts.stream().noneMatch(account -> account.getMemberId() == memberId);
    }

    // 확정된 금액과 기존 계좌는 유지하고, 미등록 수취인의 계좌만 최초 1회 보충합니다.
    public void registerMissingAccount(SettlementAccount account) {
        if (!canRegisterMissingAccount(account.getMemberId())) {
            throw new com.prgms.backend.global.exception.custom.settlement.SettlementRequestException(
                    409, "확정 후에는 미등록 수취인의 계좌만 등록할 수 있습니다.");
        }
        accounts.add(new SettlementAccountSnapshot(account));
    }

    public void saveResult(List<SettlementBalance> balances, List<SettlementTransfer> transfers) {
        checkOpen();
        this.balances.clear();
        this.balances.addAll(balances);
        this.transfers.clear();
        this.transfers.addAll(transfers);
    }

    public Settlement(long meetingId) {
        if (meetingId <= 0) throw new IllegalArgumentException("모임 ID가 필요합니다.");
        this.meetingId = meetingId;
    }

    // 모임장 확정 (권한 확인) 후 계산 결과 저장
    public void close(long leaderMemberId) {
        if (leaderMemberId <= 0) {
            throw new IllegalArgumentException("모임장 ID가 필요합니다.");
        }
        checkOpen();
        status = SettlementStatus.CLOSED;
        closedByMemberId = leaderMemberId;
        closedAt = LocalDateTime.now();
    }
}
