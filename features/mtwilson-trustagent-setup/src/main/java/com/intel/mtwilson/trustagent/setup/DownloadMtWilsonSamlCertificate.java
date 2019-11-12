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
 *
 * Prerequisites:  Trust Agent Keystore must already be created
 * @author boskisha
 */
public class DownloadMtWilsonSamlCertificate extends AbstractSetupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DownloadMtWilsonSamlCertificate.class);

    private String url;
    private String bearerToken;
    private GuardedPassword keystoreGuardedPassword = new GuardedPassword();
    private SimpleKeystore keystore;
    private TrustagentConfiguration trustagentConfiguration;
    private File truststoreFile;
    private String truststorePassword;
    @Override
    protected void configure() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        url = trustagentConfiguration.getMtWilsonApiUrl();
        if( url == null || url.isEmpty() ) {
            configuration("Mt Wilson URL is not set");
        }
        bearerToken = System.getenv(TrustagentConfiguration.BEARER_TOKEN_ENV);
        if (bearerToken == null || bearerToken.isEmpty()) {
            configuration("BEARER_TOKEN not set in the environment");
        }
        truststoreFile = trustagentConfiguration.getTrustagentTruststoreFile();
        if( truststoreFile == null || !truststoreFile.exists() ) {
            configuration("Trust Agent truststore does not exist");
        }
        truststorePassword = trustagentConfiguration.getTrustagentTruststorePassword();
        keystoreGuardedPassword.setPassword(truststorePassword);
        if( !keystoreGuardedPassword.isPasswordValid() ) {
            configuration("Trust Agent truststore password is not set");
        }
        keystore = new SimpleKeystore(new FileResource(truststoreFile), keystoreGuardedPassword.getInsPassword());
        keystoreGuardedPassword.dispose();
    }

    @Override
    protected void validate() throws Exception {
        try {
            X509Certificate samlCertificate = keystore.getX509Certificate("saml", SimpleKeystore.CA);
            if(samlCertificate == null){
                validation("Missing Saml Certificate");
            }
            if(samlCertificate != null){
                log.debug("Found saml Certificate {}", Sha1Digest.digestOf(samlCertificate.getEncoded()).toHexString());
            }
        }
        catch(NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException | CertificateEncodingException e) {
            log.debug("Cannot load Saml certificate");
            validation("Cannot load Saml certificate", e);
        }
    }

    @Override
    protected void execute() throws Exception {
        log.debug("Downloading SAML certificate and adding it to the keystore");
        TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(truststoreFile, truststorePassword).build();
        TlsConnection tlsConnection = new TlsConnection(new URL(url), tlsPolicy);
        Properties clientConfiguration = new Properties();
        clientConfiguration.setProperty(TrustagentConfiguration.BEARER_TOKEN, bearerToken);
        CaCertificates client = new CaCertificates(clientConfiguration, tlsConnection);
        X509Certificate certificate = client.retrieveCaCertificate("saml");
        keystore.addTrustedCaCertificate(certificate, "saml");
        keystore.save();
    }

}
