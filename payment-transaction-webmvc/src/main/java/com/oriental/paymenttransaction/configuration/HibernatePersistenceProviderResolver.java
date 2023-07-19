package com.oriental.paymenttransaction.configuration;

import java.util.Collections;
import java.util.List;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;
import javax.persistence.spi.PersistenceProviderResolverHolder;

import org.hibernate.ejb.HibernatePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author nsharma0
 * 
 */
@SuppressWarnings("deprecation")
public class HibernatePersistenceProviderResolver  implements PersistenceProviderResolver{
	private static Logger logger = LoggerFactory.getLogger(HibernatePersistenceProviderResolver.class);

	    
		private volatile PersistenceProvider persistenceProvider = new HibernatePersistence();

	    public List<PersistenceProvider> getPersistenceProviders() {
	        return Collections.singletonList(persistenceProvider);
	    }

	    public void clearCachedProviders() {
	        persistenceProvider = new HibernatePersistence();
	    }

	    public static void register() {
	    	logger.info("Registering HibernatePersistenceProviderResolver");
	        PersistenceProviderResolverHolder.setPersistenceProviderResolver(new HibernatePersistenceProviderResolver());
	    }
}
