package org.fabric3.cache.infinispan.provision;

import java.util.ArrayList;
import java.util.List;

import org.fabric3.cache.spi.CacheConfiguration;
import org.w3c.dom.Document;

/**
 * Infinispan cache configuration.
 * 
 * @version $Rev$ $Date$
 */
public class InfinispanCacheConfiguration extends CacheConfiguration {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4317772018610416411L;

    private List<Document> configurations = new ArrayList<Document>();

    public void addCacheConfiguration(Document configuration) {
    	configurations.add(configuration);
    }
    
    public List<Document> getCacheConfigurations() {
    	return configurations;
    }    
}
