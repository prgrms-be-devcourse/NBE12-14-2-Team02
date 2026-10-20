package com.prgms.backend.domain.settlement;

import com.prgms.backend.domain.notification.service.NotificationService;
import com.prgms.backend.domain.settlement.integration.SettlementClosedEvent;
import com.prgms.backend.domain.settlement.integration.SettlementClosedEventListener;
import com.prgms.backend.domain.settlement.integration.SettlementNotificationWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettlementClosedEventTest {
    private AnnotationConfigApplicationContext context;
    private NotificationService notifications;
    private Connection settlementConnection;
    private Connection notificationConnection;
    private TransactionTemplate transaction;
    private final SettlementClosedEvent event = new SettlementClosedEvent(10, 100, LocalDateTime.now());

    @Configuration
    @EnableTransactionManagement
    static class TransactionConfig {}

    @BeforeEach
    void setup() throws Exception {
        notifications = mock(NotificationService.class);
        settlementConnection = mock(Connection.class);
        notificationConnection = mock(Connection.class);
        when(settlementConnection.getAutoCommit()).thenReturn(true);
        when(notificationConnection.getAutoCommit()).thenReturn(true);
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(settlementConnection, notificationConnection);
        var manager = new DataSourceTransactionManager(dataSource);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(NotificationService.class, () -> notifications);
        context.registerBean(DataSourceTransactionManager.class, () -> manager);
        context.register(TransactionConfig.class, SettlementClosedEventListener.class, SettlementNotificationWriter.class);
        context.refresh();
        transaction = new TransactionTemplate(manager);
    }

    @AfterEach
    void close() {
        context.close();
    }

    @Test
    void notificationCommitsSeparatelyAfterSettlementCommit() throws Exception {
        transaction.executeWithoutResult(status -> {
            context.publishEvent(event);
            verifyNoInteractions(notifications);
        });
        var order = inOrder(settlementConnection, notifications, notificationConnection);
        order.verify(settlementConnection).commit();
        order.verify(notifications).notifySettlementClosed(10L);
        order.verify(notificationConnection).commit();
    }

    @Test
    void notificationFailureDoesNotFailCommittedSettlement() throws Exception {
        doThrow(new IllegalStateException("notification unavailable")).when(notifications).notifySettlementClosed(10L);
        assertDoesNotThrow(() -> transaction.executeWithoutResult(status -> context.publishEvent(event)));
        verify(settlementConnection).commit();
        verify(settlementConnection, never()).rollback();
        verify(notificationConnection).rollback();
        verify(notificationConnection, never()).commit();
    }

    @Test
    void notificationCommitFailureDoesNotFailCommittedSettlement() throws Exception {
        doThrow(new java.sql.SQLException("notification commit failed")).when(notificationConnection).commit();
        assertDoesNotThrow(() -> transaction.executeWithoutResult(status -> context.publishEvent(event)));
        verify(settlementConnection).commit();
        verify(settlementConnection, never()).rollback();
        verify(notifications).notifySettlementClosed(10L);
    }

    @Test
    void rolledBackSettlementDoesNotSendNotification() throws Exception {
        transaction.executeWithoutResult(status -> {
            context.publishEvent(event);
            status.setRollbackOnly();
        });
        verify(settlementConnection).rollback();
        verifyNoInteractions(notifications, notificationConnection);
    }

    @Test
    void eventWithoutTransactionDoesNotSendNotification() {
        context.publishEvent(event);
        verifyNoInteractions(notifications, notificationConnection);
    }
}
