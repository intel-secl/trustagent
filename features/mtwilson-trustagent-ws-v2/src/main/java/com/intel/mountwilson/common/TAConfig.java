/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mountwilson.common;

import com.intel.dcsg.cpg.configuration.CommonsConfigurationAdapter;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.dcsg.cpg.io.pem.Pem;
import com.intel.mtwilson.Environment;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.configuration.EncryptedConfigurationProvider;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;


import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.io.IOUtils;
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
    
    // for troubleshooting
    private void dumpConfiguration(Configuration c, String label) {
        String keys[] = new String[] { /*"app.path",*/ "debug", "trustagent.http.tls.port", "mtwilson.api.url" };
        for(String key : keys) {
            String value = c.getString(key);
            System.out.println(String.format("TAConfig [%s]: %s=%s", label, key, value));
        }
    }

    private Configuration gatherConfiguration(Properties defaults)  {
        try {
            CompositeConfiguration composite = new CompositeConfiguration();
            Configuration standard=null;

            // first priority is the configuration file
            File file = new File(Folders.configuration() + File.separator + "trustagent.properties");        
            try(FileInputStream in = new FileInputStream(file)) {
                String content = IOUtils.toString(in);
                if (Pem.isPem(content)) {
                    String password = Environment.get("PASSWORD");

                    //read in password from the file if the env variable is not set
                    if( password == null || password.isEmpty() ) {
                        File passwordFile = new File(Folders.configuration() + File.separator + ".trustagent_password");
                        if (passwordFile.exists()) {
                            try(FileInputStream passwordIn = new FileInputStream(passwordFile)) {
                                password = IOUtils.toString(passwordIn).trim();
                            }
                        }                            
                    }

                    com.intel.dcsg.cpg.configuration.Configuration configuration = new EncryptedConfigurationProvider(new FileResource(file), password).load();
                    standard = new CommonsConfigurationAdapter(configuration);
                }
            }
            if(standard != null) {
                dumpConfiguration(standard, "file:" + file.getAbsolutePath());
                composite.addConfiguration(standard);
            }

            // second priority are the defaults that were passed in, we use them if no better source was found
            if( defaults != null ) {
                MapConfiguration defaultconfig = new MapConfiguration(defaults);
                dumpConfiguration(defaultconfig, "default");
                composite.addConfiguration(defaultconfig);
            }
            dumpConfiguration(composite, "composite");
            return composite;
        }
        catch(Exception e) {
            throw new RuntimeException("Cannot load properties configuration", e);
        }
    }
    

    
}
