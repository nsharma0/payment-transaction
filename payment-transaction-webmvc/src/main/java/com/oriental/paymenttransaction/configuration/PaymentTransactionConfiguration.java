package com.oriental.paymenttransaction.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.oriental.comet.db2.config.EnterpriseDbConfig;

/**
 * @author nsharma0
 * 
 */

@Configuration
@EnableWebMvc
@EnableAsync(proxyTargetClass=true)
@Import({EnterpriseDbConfig.class, SwaggerConfiguration.class })
@ComponentScan(basePackages = { "com.oriental.paymenttransaction","com.oriental.comet.db2.dao"})
@EnableScheduling
public class PaymentTransactionConfiguration extends WebMvcConfigurerAdapter implements SchedulingConfigurer, AsyncConfigurer {

	/*@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new RequestProcessingInterceptor(new PAYMENTGATEWAYController()));
	}*/

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");

		registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
	}

	@Bean
	public ViewResolver viewResolver() {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
		viewResolver.setViewClass(JstlView.class);
		viewResolver.setPrefix("/WEB-INF/views/");
		viewResolver.setSuffix(".jsp");

		return viewResolver;
	}

	@Bean(name = "messageSource")
	public MessageSource messageSource() {
		ReloadableResourceBundleMessageSource bean = new ReloadableResourceBundleMessageSource();
		bean.setBasename("classpath:messages");
		// bean.setDefaultEncoding("UTF-8");
		return bean;
	}

	@Bean(name = "validator")
	public LocalValidatorFactoryBean validator() {
		LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
		bean.setValidationMessageSource(messageSource());
		return bean;
	}

	@Override
	public Validator getValidator() {
		return validator();
	}

	
	@Bean
	public ObjectMapper getObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
		// mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.enable(SerializationFeature.INDENT_OUTPUT);

		mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}

	@Bean
	public MappingJackson2HttpMessageConverter getMappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
		MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
		jsonConverter.setObjectMapper(objectMapper);
		return jsonConverter;
	}

	@Bean
	public RestTemplate getRestTemplate(MappingJackson2HttpMessageConverter converter) {

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(converter);
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setMessageConverters(messageConverters);

		return restTemplate;
	}
	
	@Override
	public Executor getAsyncExecutor() {
		Executor executor = this.taskScheduler();
        return executor;
	} 
	
	 @Bean
	 public ThreadPoolTaskScheduler taskScheduler() {
	  	ThreadPoolTaskScheduler pool = new ThreadPoolTaskScheduler();
	   	pool.setPoolSize(10);
    	return pool;
    }

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		TaskScheduler scheduler = this.taskScheduler();
		taskRegistrar.setTaskScheduler(scheduler);
	} 
}
