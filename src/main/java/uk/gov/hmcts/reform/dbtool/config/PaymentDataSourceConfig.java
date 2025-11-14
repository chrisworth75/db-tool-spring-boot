package uk.gov.hmcts.reform.dbtool.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Payment database
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "uk.gov.hmcts.reform.dbtool.repository"
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                value = uk.gov.hmcts.reform.dbtool.repository.RefundRepository.class
        ),
        entityManagerFactoryRef = "paymentEntityManagerFactory",
        transactionManagerRef = "paymentTransactionManager"
)
public class PaymentDataSourceConfig {

    @Primary
    @Bean(name = "paymentDataSourceProperties")
    @ConfigurationProperties("spring.datasource.payment")
    public DataSourceProperties paymentDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "paymentDataSource")
    @ConfigurationProperties("spring.datasource.payment.hikari")
    public DataSource paymentDataSource(
            @Qualifier("paymentDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Primary
    @Bean(name = "paymentEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean paymentEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("paymentDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "validate");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        return builder
                .dataSource(dataSource)
                .packages(
                        "uk.gov.hmcts.reform.dbtool.database"
                )
                .persistenceUnit("payment")
                .properties(properties)
                .build();
    }

    @Primary
    @Bean(name = "paymentTransactionManager")
    public PlatformTransactionManager paymentTransactionManager(
            @Qualifier("paymentEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
