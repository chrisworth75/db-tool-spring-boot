package uk.gov.hmcts.reform.dbtool.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Refund database
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "uk.gov.hmcts.reform.dbtool.repository",
        includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                value = uk.gov.hmcts.reform.dbtool.repository.RefundRepository.class
        ),
        entityManagerFactoryRef = "refundEntityManagerFactory",
        transactionManagerRef = "refundTransactionManager"
)
public class RefundDataSourceConfig {

    @Bean(name = "refundDataSourceProperties")
    @ConfigurationProperties("spring.datasource.refunds")
    public DataSourceProperties refundDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "refundDataSource")
    @ConfigurationProperties("spring.datasource.refunds.hikari")
    public DataSource refundDataSource(
            @Qualifier("refundDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "refundEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean refundEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("refundDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        return builder
                .dataSource(dataSource)
                .packages("uk.gov.hmcts.reform.dbtool.database")
                .persistenceUnit("refunds")
                .properties(properties)
                .build();
    }

    @Bean(name = "refundTransactionManager")
    public PlatformTransactionManager refundTransactionManager(
            @Qualifier("refundEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "refundLiquibase")
    public SpringLiquibase refundLiquibase(
            @Qualifier("refundDataSource") DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:db/changelog/refunds/db.changelog-master.yaml");
        liquibase.setDefaultSchema("public");
        return liquibase;
    }
}
