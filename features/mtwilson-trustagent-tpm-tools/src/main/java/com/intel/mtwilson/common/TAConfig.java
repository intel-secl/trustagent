/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.common;

import com.intel.mtwilson.Folders;
import java.io.File;
import java.util.Properties;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attempts to use commons-configuration to load the Trust Agent settings.
 * 
 * The configuration is loaded in the following priority order:
 * System properties
 * Properties in the file trustagent.properties (create this file in your classpath to customize local settings)
 * Hard-coded defaults (defined in this class)
 * 
 * The available configuration sources (such as trustagent.properties) are configured in the ta-config.xml
 * included with Trust Agent
 * 
 * @author jbuhacoff
 */
public class TAConfig {

    private static final TAConfig global = new TAConfig();
    public static final Configuration getConfiguration() { return global.getConfigurationInstance(); }
    
    private final Configuration config;
    private Configuration getConfigurationInstance() { return config; }
    private Logger log = LoggerFactory.getLogger(getClass().getName());
    
    private TAConfig() {
        Properties defaults = new Properties();
        defaults.setProperty("debug", "false"); // allowed values: false, true (case insensitive)
        defaults.setProperty("aikblob.filename", "aik.blob");
        defaults.setProperty("aikcert.filename", "aik.pem"); // issue #878 the aikcert is in PEM format so we label it properly
        defaults.setProperty("ekcert.filename", "ekcert.cer");
        defaults.setProperty("daa.challenge.filename", "daa-challenge");
        defaults.setProperty("daa.response.filename.filename", "daa-response");        
        config = gatherConfiguration(defaults);
    }

    private Configuration gatherConfiguration(Properties defaults)  {
        try {
        CompositeConfiguration composite = new CompositeConfiguration();
        
        // first priority is the configuration file
        File file = new File(Folders.configuration() + File.separator + "trustagent.properties");
        PropertiesConfiguration standard = new PropertiesConfiguration(file);
        //dumpConfiguration(standard, "file:"+file.getAbsolutePath());
        composite.addConfiguration(standard);
        
        // second priority are the defaults that were passed in, we use them if no better source was found
        if( defaults != null ) {
            MapConfiguration defaultconfig = new MapConfiguration(defaults);
            //dumpConfiguration(defaultconfig, "default");
            composite.addConfiguration(defaultconfig);
        }
        //dumpConfiguration(composite, "composite");
        return composite;
        }
        catch(ConfigurationException e) {
            throw new RuntimeException("Cannot load properties configuration", e);
        }
    }
    

    
}
