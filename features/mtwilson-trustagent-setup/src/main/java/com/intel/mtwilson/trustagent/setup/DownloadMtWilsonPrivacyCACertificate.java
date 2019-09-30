/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import com.intel.dcsg.cpg.crypto.SimpleKeystore;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.dcsg.cpg.crypto.Sha1Digest;
import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.mtwilson.client.jaxrs.CaCertificates;
import com.intel.mtwilson.core.common.utils.AASTokenFetcher;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import java.io.File;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import com.intel.mtwilson.crypto.password.GuardedPassword;

/**
 * Prerequisites:  Trust Agent Keystore must already be created
 *
 * @author jbuhacoff
 */
public class DownloadMtWilsonPrivacyCACertificate extends AbstractSetupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DownloadMtWilsonPrivacyCACertificate.class);

    private String url;
    private String username;
    private String password;
    private String aasApiUrl;
    private GuardedPassword keystoreGuardedPassword = new GuardedPassword();
    private SimpleKeystore keystore;
    private TrustagentConfiguration trustagentConfiguration;

    @Override
    protected void configure() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        url = trustagentConfiguration.getMtWilsonApiUrl();
        if( url == null || url.isEmpty() ) {
            configuration("Mt Wilson URL is not set");
        }
        username = trustagentConfiguration.getTrustAgentAdminUserName();
        if (username == null || username.isEmpty()) {
            configuration("TA admin username is not set");
        }
        password = trustagentConfiguration.getTrustAgentAdminPassword();
        if (password == null || password.isEmpty()) {
            configuration("TA admin password is not set");
        }
        aasApiUrl = trustagentConfiguration.getAasApiUrl();
        if (aasApiUrl == null || aasApiUrl.isEmpty()) {
            configuration("AAS API URL is not set");
        }
        File truststoreFile = trustagentConfiguration.getTrustagentTruststoreFile();
        if( truststoreFile == null || !truststoreFile.exists() ) {
            configuration("Trust Agent keystore does not exist");
        }
        keystoreGuardedPassword.setPassword(trustagentConfiguration.getTrustagentTruststorePassword());
        if( !keystoreGuardedPassword.isPasswordValid() ) {
            configuration("Trust Agent keystore password is not set");
        }
        keystore = new SimpleKeystore(new FileResource(truststoreFile), keystoreGuardedPassword.getInsPassword());
        keystoreGuardedPassword.dispose();
    }

    @Override
    protected void validate() throws Exception {
        try {
            X509Certificate certificate = keystore.getX509Certificate("privacy", SimpleKeystore.CA);
            if( certificate == null ) {
                validation("Missing Privacy CA certificate");
            }
            if( certificate != null ) {
                log.debug("Found Privacy CA certificate {}", Sha1Digest.digestOf(certificate.getEncoded()).toHexString());
            }
        }
        catch(NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException | CertificateEncodingException e) {
            log.debug("Cannot load Privacy CA certificate");
            validation("Cannot load Privacy CA certificate", e);
        }
    }

    @Override
    protected void execute() throws Exception {
        log.debug("Creating TLS policy");
        TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustagentConfiguration.getTrustagentTruststoreFile(),
            trustagentConfiguration.getTrustagentTruststorePassword()).build();
        TlsConnection tlsConnection = new TlsConnection(new URL(url), tlsPolicy);
        Properties clientConfiguration = new Properties();

        clientConfiguration.setProperty(TrustagentConfiguration.BEARER_TOKEN, new AASTokenFetcher().getAASToken(username, password, new TlsConnection(new URL(aasApiUrl), tlsPolicy)));
        CaCertificates client = new CaCertificates(clientConfiguration, tlsConnection);
        X509Certificate certificate = client.retrieveCaCertificate("privacy");
        keystore.addTrustedCaCertificate(certificate, "privacy");
        keystore.save();
    }

}
