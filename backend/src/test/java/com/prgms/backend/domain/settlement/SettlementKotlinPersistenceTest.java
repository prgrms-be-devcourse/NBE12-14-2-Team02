package com.prgms.backend.domain.settlement;

import com.prgms.backend.domain.expense.entity.Expense;
import com.prgms.backend.domain.expense.entity.ExpenseRegistration;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.expense.repository.ExpenseRegistrationRepository;
import com.prgms.backend.domain.settlement.entity.*;
import com.prgms.backend.domain.settlement.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

// 실제 Hibernate/H2로 Kotlin 엔티티의 생성자, 프록시, 컬렉션 매핑과 기존 JPQL을 검증합니다.
@SpringJUnitConfig(SettlementKotlinPersistenceTest.Config.class)
@Transactional
class SettlementKotlinPersistenceTest {
    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = {ExpenseRepository.class, SettlementRepository.class})
    static class Config {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan(Expense.class.getPackageName(), Settlement.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            var properties = new Properties();
            properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            factory.setJpaProperties(properties);
            return factory;
        }

        @Bean
        JpaTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
    }

    @Autowired EntityManager entityManager;
    @Autowired ExpenseRepository expenses;
    @Autowired ExpenseRegistrationRepository registrations;
    @Autowired SettlementRepository settlements;
    @Autowired SettlementAccountRepository accounts;

    @Test
    void expenseRoundTripPreservesSharesAndSupportsLazyProxyAndExistingQuery() {
        Expense saved = expenses.saveAndFlush(Expense.register(10, 1, "식사", 300, null,
                Map.of(1L, 100L, 2L, 200L), "EXACT", null, null));
        long id = saved.getId();
        registrations.saveAndFlush(new ExpenseRegistration(10, 1, "request-key", "fingerprint", id));
        entityManager.clear();

        Expense loaded = entityManager.getReference(Expense.class, id);
        assertFalse(Hibernate.isInitialized(loaded));
        assertEquals("식사", loaded.getTitle());
        assertTrue(Hibernate.isInitialized(loaded));
        assertNotNull(loaded.getCreatedAt());
        assertEquals(Map.of(1L, 100L, 2L, 200L), loaded.getParticipantAmounts());
        assertTrue(expenses.existsMemberInExpense(10L, 2L));
        assertFalse(expenses.existsMemberInExpense(10L, 3L));
        assertEquals(id, registrations.findByMeetingIdAndMemberIdAndRequestKey(10, 1, "request-key")
                .orElseThrow().getExpenseId());

        loaded.update("수정", 500, null, Map.of(2L, 500L), "EXACT", null, null);
        entityManager.flush();
        entityManager.clear();
        assertEquals(Map.of(2L, 500L), expenses.findById(id).orElseThrow().getParticipantAmounts());
    }

    @Test
    void settlementRoundTripPreservesStatusBalancesTransfersAndAccountSnapshot() {
        SettlementAccount account = accounts.saveAndFlush(new SettlementAccount(10, 1, "은행", "001-234", "사용자"));
        Settlement settlement = new Settlement(10);
        settlement.saveResult(
                List.of(new SettlementBalance(1, 300, 100), new SettlementBalance(2, 0, 200)),
                List.of(new SettlementTransfer(2, 1, 200)));
        settlement.saveAccounts(List.of(new SettlementAccountSnapshot(account)));
        settlement.close(1);
        settlements.saveAndFlush(settlement);
        entityManager.clear();

        Settlement loaded = settlements.findByMeetingId(10).orElseThrow();
        assertEquals(SettlementStatus.CLOSED, loaded.getStatus());
        assertNotNull(loaded.getVersion());
        assertNotNull(loaded.getClosedAt());
        assertEquals(2, loaded.getBalances().size());
        assertEquals(300, loaded.getBalances().getFirst().getPaidAmount());
        assertEquals(200, loaded.getTransfers().getFirst().getAmount());
        assertEquals("001-234", loaded.getAccounts().getFirst().getAccountNumber());

        accounts.findByMeetingIdAndMemberId(10, 1).orElseThrow().update("변경은행", "999", "사용자");
        entityManager.flush();
        entityManager.clear();
        assertEquals("001-234", settlements.findByMeetingId(10).orElseThrow().getAccounts().getFirst().getAccountNumber());
    }
}
