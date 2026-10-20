package com.prgms.backend.domain.content;

import com.prgms.backend.domain.content.integration.ContentNotificationWriter;
import com.prgms.backend.domain.content.integration.ContentPollClosedEvent;
import com.prgms.backend.domain.content.integration.ContentPollClosedEventListener;
import com.prgms.backend.domain.notification.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class ContentPollClosedEventTest {

    private AnnotationConfigApplicationContext context;
    private NotificationService notificationService;
    private Connection pollConnection;
    private Connection notificationConnection;
    private TransactionTemplate transaction;

    private final ContentPollClosedEvent event = new ContentPollClosedEvent(10L, 100L);

    @Configuration
    @EnableTransactionManagement
    static class TransactionConfig {}

    @BeforeEach
    void setUp() throws Exception {
        notificationService = mock(NotificationService.class);
        pollConnection = mock(Connection.class);
        notificationConnection = mock(Connection.class);
        when(pollConnection.getAutoCommit()).thenReturn(true);
        when(notificationConnection.getAutoCommit()).thenReturn(true);

        // 첫 연결은 마감 트랜잭션, 두 번째 연결은 REQUIRES_NEW 알림 트랜잭션
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(pollConnection, notificationConnection);
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);

        context = new AnnotationConfigApplicationContext();
        context.registerBean(NotificationService.class, () -> notificationService);
        context.registerBean(DataSourceTransactionManager.class, () -> transactionManager);
        context.register(
            TransactionConfig.class,
            ContentPollClosedEventListener.class,
            ContentNotificationWriter.class
        );
        context.refresh();

        transaction = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("콘텐츠 투표 마감이 커밋된 뒤에 알림을 별도 트랜잭션으로 저장")
    void saveNotificationAfterPollCommit() throws Exception {

        // when
        transaction.executeWithoutResult(status -> {
            context.publishEvent(event);
            verifyNoInteractions(notificationService);
        });

        // then
        InOrder order = inOrder(pollConnection, notificationService, notificationConnection);
        order.verify(pollConnection).commit();
        order.verify(notificationService).notifyContentClosed(10L);
        order.verify(notificationConnection).commit();
    }

    @Test
    @DisplayName("알림 저장이 실패해도 이미 커밋된 콘텐츠 투표 마감은 롤백않음")
    void notificationSaveFailureKeepsPollClosed() throws Exception {

        // given
        doThrow(new IllegalStateException("notification unavailable"))
            .when(notificationService).notifyContentClosed(10L);

        // when
        assertDoesNotThrow(() ->
            transaction.executeWithoutResult(status -> context.publishEvent(event))
        );

        // then
        verify(pollConnection).commit();
        verify(pollConnection, never()).rollback();
        verify(notificationConnection).rollback();
        verify(notificationConnection, never()).commit();
    }

    @Test
    @DisplayName("알림 트랜잭션 커밋이 실패해도 콘텐츠 투표 마감은 유지되고 예외가 전파되지 않음")
    void notificationCommitFailureKeepsPollClosed() throws Exception {

        // given
        doThrow(new SQLException("notification commit failed"))
            .when(notificationConnection).commit();

        // when
        assertDoesNotThrow(() ->
            transaction.executeWithoutResult(status -> context.publishEvent(event))
        );

        // then
        verify(pollConnection).commit();
        verify(pollConnection, never()).rollback();
        verify(notificationService).notifyContentClosed(10L);
    }

    @Test
    @DisplayName("콘텐츠 투표 마감이 롤백되면 알림을 저장하지 않음")
    void rolledBackPollSendsNoNotification() throws Exception {

        // when
        transaction.executeWithoutResult(status -> {
            context.publishEvent(event);
            status.setRollbackOnly();
        });

        // then
        verify(pollConnection).rollback();
        verifyNoInteractions(notificationService, notificationConnection);
    }
}
