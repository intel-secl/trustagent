/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.dcsg.cpg.x509.X509Util;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.privacyca.v2.model.BindingKeyEndorsementRequest;
import com.intel.mtwilson.client.jaxrs.HostTpmKeys;
import com.intel.mtwilson.core.tpm.Tpm;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import java.io.File;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.intel.mtwilson.crypto.password.GuardedPassword;

/**
 *
 * @author ssbangal
 */
public class CertifyBindingKey extends AbstractSetupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CertifyBindingKey.class);
    
    private TrustagentConfiguration trustagentConfiguration;
    private File bindingKeyPem;    
    private String url;
    private String username;
    private GuardedPassword guardedPassword = new GuardedPassword();
    private File keystoreFile;
    private GuardedPassword keystoreGuardedPassword = new GuardedPassword();
    private File bindingKeyName;
    private File bindingKeyModulus;
    private File bindingKeyTCGCertificate;
    private File bindingKeyTCGCertificateSignature;
    private File aikPemCertificate;
    
    @Override
    protected void configure() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        url = trustagentConfiguration.getMtWilsonApiUrl();
        if( url == null || url.isEmpty() ) {
            configuration("Mt Wilson URL is not set");
        }
        username = trustagentConfiguration.getMtWilsonApiUsername();
        guardedPassword.setPassword(trustagentConfiguration.getMtWilsonApiPassword());
        if( username == null || username.isEmpty() ) {
            configuration("Mt Wilson username is not set");
        }
        if(!guardedPassword.isPasswordValid()) {
            configuration("Mt Wilson password is not set");
        }
        
        keystoreFile = trustagentConfiguration.getTrustagentKeystoreFile();
        if( keystoreFile == null || !keystoreFile.exists() ) {
            configuration("Trust Agent keystore does not exist");
        }
        keystoreGuardedPassword.setPassword(trustagentConfiguration.getTrustagentKeystorePassword());
        if( !keystoreGuardedPassword.isPasswordValid() ) {
            configuration("Trust Agent keystore password is not set");
        }         
        
        bindingKeyPem = trustagentConfiguration.getBindingKeyX509CertificateFile();
        keystoreGuardedPassword.dispose();
    }

    @Override
    protected void validate() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        
        // Now check for the existence of the MTW signed PEM file.
        if (bindingKeyPem == null || !bindingKeyPem.exists()) {
            validation("MTW signed Binding Key certificate does not exist.");
        }        
    }

    @Override
    protected void execute() throws Exception {
        Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
        log.info("Calling into MTW to certify the TCG standard binding key");
        String os = System.getProperty("os.name").toLowerCase();
        bindingKeyTCGCertificate = trustagentConfiguration.getBindingKeyTCGCertificateFile(); 
        bindingKeyModulus = trustagentConfiguration.getBindingKeyModulusFile();
        bindingKeyTCGCertificateSignature = trustagentConfiguration.getBindingKeyTCGCertificateSignatureFile();
        aikPemCertificate = trustagentConfiguration.getAikCertificateFile();        
	if  ( !os.contains("win" ) && tpm.getTpmVersion().equals("2.0")) //Linux and TPM 2.0
            bindingKeyName = trustagentConfiguration.getBindingKeyNameFile();
        else 
            bindingKeyName = null;
        
        //ToDo: Need to verify OS and TPMVersion for the name digest file
        log.debug("TCG Cert path is : {}", bindingKeyTCGCertificate.getAbsolutePath());
        log.debug("Public key modulus path is : {}", bindingKeyModulus.getAbsolutePath());
        log.debug("TCG Cert signature path is : {}", bindingKeyTCGCertificateSignature.getAbsolutePath());
        log.debug("AIK Certificate path is : {}", aikPemCertificate.getAbsolutePath());
        if(bindingKeyName != null)
            log.debug("Key Name file path is : {}", bindingKeyName.getAbsolutePath());
        
        BindingKeyEndorsementRequest obj = new BindingKeyEndorsementRequest();
        obj.setPublicKeyModulus(FileUtils.readFileToByteArray(bindingKeyModulus));
        obj.setTpmCertifyKey(FileUtils.readFileToByteArray(bindingKeyTCGCertificate));
        obj.setTpmCertifyKeySignature(FileUtils.readFileToByteArray(bindingKeyTCGCertificateSignature));
        //ToDo: Need to verify  TPMVersion for the name digest file
        if  ( !os.contains("win" ) && tpm.getTpmVersion().equals("2.0") && bindingKeyName != null) //Linux and TPM 2.0
            obj.setNameDigest(FileUtils.readFileToByteArray(bindingKeyName));
        else
            obj.setNameDigest(null);
        obj.setTpmVersion(tpm.getTpmVersion());
        log.debug("Detected TPM Version: {}", tpm.getTpmVersion());
        if (os.contains("win"))
            obj.setOperatingSystem("Windows");
        else
            obj.setOperatingSystem("Linux");
        // set encyrption scheme. This is especially used for TPM 2.0 since the encryption scheme is not included in the TPM_ST_ATTEST_CERTIFY
        // Windows uses PKCS by default; Linux uses OAEP by default,
        // rsa enc scheme defined in tpm1.2
        short TPM_ES_RSAESPKCSv15 = 0x0002;
        short TPM_ES_RSAESOAEP_SHA1_MGF1 = 0x0003;
        //rsa enc scheme defined in tpm2.0
        short TPM_ALG_RSAES = 0x0015; //RSAES-PKCS1_v1_5 -- this is the one hardcoded in tpm2 tools for RSA encryption and decryption
        //short TPM_ALG_OAEP = 0x0017; //RSAES_OAEP padding algorithm
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("windows"))
            if (tpm.getTpmVersion().equals("2.0")) {
                obj.setEncryptionScheme(TPM_ALG_RSAES);
            }
            else {
                obj.setEncryptionScheme(TPM_ES_RSAESPKCSv15);
            }
        else {
            if (tpm.getTpmVersion().equals("2.0")) {
                obj.setEncryptionScheme(TPM_ALG_RSAES);
            }
            else {
                obj.setEncryptionScheme(TPM_ES_RSAESOAEP_SHA1_MGF1); //Linux
            }
        }
        X509Certificate aikCert = X509Util.decodePemCertificate(FileUtils.readFileToString(aikPemCertificate));
        byte[] encodedAikDerCertificate = X509Util.encodeDerCertificate(aikCert);
        obj.setAikDerCertificate(encodedAikDerCertificate);
        
        log.debug("Creating TLS policy");
        TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustagentConfiguration.getTrustagentKeystoreFile(), 
                trustagentConfiguration.getTrustagentKeystorePassword()).build();
        TlsConnection tlsConnection = new TlsConnection(new URL(url), tlsPolicy);
        
        Properties clientConfiguration = new Properties();
        clientConfiguration.setProperty(TrustagentConfiguration.MTWILSON_API_USERNAME, username);
        clientConfiguration.setProperty(TrustagentConfiguration.MTWILSON_API_PASSWORD, guardedPassword.getInsPassword());
        guardedPassword.dispose();
        
        HostTpmKeys client = new HostTpmKeys(clientConfiguration, tlsConnection);
        X509Certificate bindingKeyCertificate = client.createBindingKeyCertificate(obj);
        String pemCertificate = X509Util.encodePemCertificate(bindingKeyCertificate);
        log.debug("MTW signed PEM certificate is {} ", pemCertificate);
        
        FileUtils.writeStringToFile(bindingKeyPem, pemCertificate);
        log.debug("Successfully created the MTW signed X509Certificate for the binding key and stored at {}.", 
                bindingKeyPem.getAbsolutePath());
        if(bindingKeyName != null)
            Files.deleteIfExists(Paths.get(bindingKeyName.getAbsolutePath()));
        
    }
}
