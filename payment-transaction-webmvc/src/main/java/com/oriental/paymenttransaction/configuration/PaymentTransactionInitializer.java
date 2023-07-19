package com.oriental.paymenttransaction.configuration;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author nsharma0
 * 
 */
public class PaymentTransactionInitializer  implements WebApplicationInitializer {
	public void onStartup(ServletContext container) throws ServletException {

		AnnotationConfigWebApplicationContext servletContext = getContext();
		servletContext.setServletContext(container);

		ServletRegistration.Dynamic dispatcher = container.addServlet("dispatcher",
				new DispatcherServlet(servletContext));
		dispatcher.setLoadOnStartup(1);
		dispatcher.addMapping("/");

		// Adding Filters
		container.addFilter("hiddenHttpMethodFilter", HiddenHttpMethodFilter.class)
				 .addMappingForUrlPatterns(null, false, "/*");
		container.addFilter("openEntityManagerInViewFilter",OpenEntityManagerInViewFilter.class);
		
		//logging filter
		/*FilterRegistration.Dynamic logging = container.addFilter("requestLoggingFilter", new RequestLoggingFilter());
		logging.addMappingForUrlPatterns(null, false, "/*");
				
		// UTF-8 Encoding
		logging = container.addFilter("encodingFilter", new CharacterEncodingFilter());
		logging.setInitParameter("encoding", "UTF-8");
		logging.setInitParameter("forceEncoding", "true");
		logging.addMappingForUrlPatterns(null, true, "/*");*/

		container.addListener(new ContextLoaderListener(servletContext));
		container.addListener(new HibernateProviderListener());
	}

	public AnnotationConfigWebApplicationContext getContext() {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(PaymentTransactionConfiguration.class);

		return context;
	}
}