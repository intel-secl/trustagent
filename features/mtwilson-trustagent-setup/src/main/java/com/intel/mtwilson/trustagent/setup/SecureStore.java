/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.dcsg.cpg.crypto.SimpleKeystore;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import com.intel.mtwilson.Folders;
import com.intel.dcsg.cpg.io.Platform;
import java.security.KeyStore;
import java.security.KeyStoreException;
import com.intel.mtwilson.crypto.password.SecureStoreUtil;

public class SecureStore extends AbstractSetupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecureStore.class);
    private TrustagentConfiguration trustagentConfiguration;
    
    @Override
    protected void configure() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
    }

    @Override
    protected void validate() throws Exception {
        String keystorePassword = trustagentConfiguration.getTrustagentSecureStorePassword();
        if( keystorePassword == null || keystorePassword.isEmpty() ) {
            validation("Keystore password is not set");
        }
    }

    @Override
    protected void execute() throws Exception {
       File privateDir = new File(Folders.configuration() + File.separator + "private");
        if( !privateDir.exists() ) { privateDir.mkdirs(); }
        if( Platform.isUnix() ) {
            Runtime.getRuntime().exec("chmod 700 "+privateDir.getAbsolutePath());
        }
        File keystoreFile = privateDir.toPath().resolve("securestore.jks").toFile();
        String keystorePassword = RandomUtil.randomBase64String(8).replace("=","_");
         if(!keystoreFile.exists()) {
             SecureStoreUtil.createKeyStore(keystoreFile.getAbsolutePath(),keystorePassword);    
        } else {
         
          try (FileOutputStream fos = new FileOutputStream(keystoreFile);
               FileInputStream fis = new FileInputStream(keystoreFile);) 
          {
            String existingKeystorePassword = trustagentConfiguration.getTrustagentSecureStorePassword();
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            keyStore.load(fis, existingKeystorePassword.toCharArray());
            keyStore.store(fos, keystorePassword.toCharArray());
          }
          catch(KeyStoreException e) {
            log.debug("Cannot open keystore, deleting it", e);
            keystoreFile.delete();
          }
          
        }
       // store the new password
        getConfiguration().set(TrustagentConfiguration.TRUSTAGENT_SECURESTORE_PASSWORD, keystorePassword);
       log.info("End of SecureStore.execute()");
    }
    

}
