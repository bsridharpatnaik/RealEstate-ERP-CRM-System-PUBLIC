package com.ec.application.multitenant;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration
public class AutoDDLConfig
{

	@Value("${spring.datasource.username}")
	private String username;

	@Value("${spring.datasource.password}")
	private String password;

	@Value("${schemas.list}")
	private String schemasList;

	@Value("${db.host}")
	private String dbHost;

	@Bean
	public DataSource dataSource()
	{
		AbstractRoutingDataSource multiDataSource = new TenantAwareRoutingSource();
		if (StringUtils.isBlank(schemasList))
		{
			return multiDataSource;
		}

		String[] tenants = schemasList.split(",");
		Map<Object, Object> targetDataSources = new HashMap<>();
		for (String tenant : tenants)
		{
			System.out.println("####" + tenant);
			tenant = tenant.trim();
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName("com.mysql.jdbc.Driver"); // Change here to MySql Driver
			dataSource.setSchema(tenant);
			dataSource.setUrl("jdbc:mysql://" + dbHost + "/" + tenant
					+ "?autoReconnect=true&characterEncoding=utf8&useSSL=false&useTimezone=true&serverTimezone=Asia/Kolkata&useLegacyDatetimeCode=false&allowPublicKeyRetrieval=true");
			dataSource.setUsername(username);
			dataSource.setPassword(password);
			targetDataSources.put(tenant, dataSource);
			LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
			emfBean.setDataSource(dataSource);
			emfBean.setPackagesToScan("com"); // Here mention JPA entity path / u can leave it scans all packages
			emfBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
			emfBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
			Map<String, Object> properties = new HashMap<>();

			properties.put("hibernate.hbm2ddl.auto", "update");
			properties.put("hibernate.default_schema", tenant);
			properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
			emfBean.setJpaPropertyMap(properties);
			emfBean.setPersistenceUnitName(dataSource.toString());
			emfBean.afterPropertiesSet();
		}
		multiDataSource.setTargetDataSources(targetDataSources);
		multiDataSource.afterPropertiesSet();
		return multiDataSource;

	}

}
