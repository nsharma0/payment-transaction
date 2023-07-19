package com.oriental.paymenttransaction.configuration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author nsharma0
 * 
 */
public class HibernateProviderListener implements ServletContextListener {
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		HibernatePersistenceProviderResolver.register();
		
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// TODO Auto-generated method stub
		
	}
}
