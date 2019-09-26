/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import com.intel.dcsg.cpg.crypto.*;

import java.net.URL;

import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.mtwilson.jaxrs2.client.CMSClient;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import com.intel.mtwilson.crypto.password.GuardedPassword;
import java.io.File;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Properties;

public class DownloadCmsCACertificate extends AbstractSetupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DownloadCmsCACertificate.class);

    private TrustagentConfiguration trustagentConfiguration;
    private GuardedPassword keystoreGuardedPassword = new GuardedPassword();
    private File keystoreFile;
    private SimpleKeystore keystore;
    private static final String CMSCA_ALIAS = "CMSCA";


    @Override
    protected void configure() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        keystoreGuardedPassword = new GuardedPassword();

        keystoreGuardedPassword.setPassword(trustagentConfiguration.getTrustagentKeystorePassword());
        if( !keystoreGuardedPassword.isPasswordValid() ) {
            configuration("Trust Agent keystore password is not set");
        }
        keystoreGuardedPassword.dispose();
    }

    @Override
    protected void validate() throws Exception {
        keystoreFile = trustagentConfiguration.getTrustagentKeystoreFile();
        if( !keystoreFile.exists() ) {
            validation("Keystore file was not created");
            return;
        }
        try {
            keystore = new SimpleKeystore(new FileResource(keystoreFile), keystoreGuardedPassword.getInsPassword());
            log.info("no exception");
            X509Certificate certificate = keystore.getX509Certificate(CMSCA_ALIAS, SimpleKeystore.CA);
            log.info(certificate.toString());
            if( certificate == null ) {
                validation("Missing CMS CA certificate");
            }
            if( certificate != null ) {
                log.debug("Found CMS CA certificate {}", Sha1Digest.digestOf(certificate.getEncoded()).toHexString());
                log.info("hi praveen");
            }
        }
        catch(NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException | CertificateEncodingException e) {
            log.info(e.getMessage());
            log.debug("Cannot load CMS CA certificate");
            validation("Cannot load CMS CA certificate", e);
        }
    }

    @Override
    protected void execute() throws Exception {
        File keystoreFile = trustagentConfiguration.getTrustagentKeystoreFile();
        GuardedPassword keystoreGuardedPassword = new GuardedPassword();
        keystoreGuardedPassword = new GuardedPassword();
        keystoreGuardedPassword.setPassword(trustagentConfiguration.getTrustagentKeystorePassword());

        TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().insecure().build();
        CMSClient cmsClient = new CMSClient(new Properties(), new TlsConnection(new URL(trustagentConfiguration.getCmsApiUrl()), tlsPolicy));
        X509Certificate cmsCACert = cmsClient.getCACertificate();
        // store it in the keystore
        SimpleKeystore keystore = new SimpleKeystore(new FileResource(keystoreFile), keystoreGuardedPassword.getInsPassword());
        keystore.addTrustedCaCertificate(cmsCACert, CMSCA_ALIAS);
        keystore.save();
        keystoreGuardedPassword.dispose();

    }
}
