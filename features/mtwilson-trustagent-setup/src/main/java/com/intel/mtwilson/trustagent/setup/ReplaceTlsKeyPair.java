/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.trustagent.setup;

import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.console.InteractiveCommand;
import com.intel.dcsg.cpg.crypto.RsaUtil;
import com.intel.dcsg.cpg.crypto.Sha384Digest;
import com.intel.dcsg.cpg.crypto.key.password.Password;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.dcsg.cpg.x509.X509Util;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.configuration.ConfigurationProvider;
import com.intel.mtwilson.util.crypto.keystore.PrivateKeyStore;
import com.intel.mtwilson.crypto.password.GuardedPassword;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

/**
 *
 * @author rksavino/ssbangal
 */
public class ReplaceTlsKeyPair extends InteractiveCommand {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReplaceTlsKeyPair.class);

    // constants
    private static final String TLS_ALIAS = "tls";
    
    // configuration keys
    private static final String TRUST_AGENT_TLS_CERT_DN = "trustagent.tls.cert.dn";
    private static final String TRUST_AGENT_TLS_CERT_IP = "trustagent.tls.cert.ip";
    private static final String TRUST_AGENT_TLS_CERT_DNS = "trustagent.tls.cert.dns";
    private static final String TRUST_AGENT_TLS_CERT_SHA384 = "trustagent.tls.cert.sha384";
    private static final String TRUST_AGENT_SSL_KEYSTORE = "trustagent.keystore";
    private static final String TRUST_AGENT_SSL_KEYSTOREPASSWORD = "trustagent.keystore.password";
    
    private PrivateKey privateKey;
    private List<X509Certificate> x509CertChainList;
    private String dn, ip, dns;
    
    private final String USAGE = "Usage: replace-tls-key-pair <--private-key=private-key-file> <--cert-chain=cert-chain-file>";
    
    @Override
    public void execute(String[] args) throws Exception {
        ConfigurationProvider provider = ConfigurationFactory.getConfigurationProvider();
        Configuration configuration = provider.load();
        validateInput();
        
        LinkedList<Certificate> certChainList = new LinkedList();
        for (X509Certificate x509Cert : x509CertChainList) {
            certChainList.add((Certificate)x509Cert);
        }
        Certificate[] certChainArray = certChainList.toArray(new Certificate[0]);
        X509Certificate publicKeyCert = x509CertChainList.get(0);
        
        String keystorePath = configuration.get(TRUST_AGENT_SSL_KEYSTORE, null);
        if( keystorePath == null ) {
            keystorePath = Folders.configuration() + File.separator + "trustagent.p12";
        }
        File keystoreFile = new File(keystorePath);
        if( !keystoreFile.exists() ) {
            throw new FileNotFoundException("\nKeystore file does not exist");
        }

        GuardedPassword keystorePasswordString = new GuardedPassword();
        keystorePasswordString.setPassword(configuration.get(TRUST_AGENT_SSL_KEYSTOREPASSWORD, null));
        if (!keystorePasswordString.isPasswordValid()) {
            throw new IllegalArgumentException("\nKeystore password is not set");
        }
        Password keystorePassword = new Password(keystorePasswordString.getInsPassword());                     
        
        try (PrivateKeyStore keystore = new PrivateKeyStore(KeyStore.getDefaultType(), new FileResource(keystoreFile), keystorePassword)) {
            // remove existing keypair from keystore
            if (keystore.contains(TLS_ALIAS)) {
                try {
                    keystore.remove(TLS_ALIAS);
                } catch (KeyStoreException e) {
                    throw new KeyStoreException("\nCannot remove existing keypair", e);
                }
            } else {
                log.warn("Keystore does not currently contain the specified key pair [{}]", TLS_ALIAS);
            }
            
            // store it in the keystore
            keystore.set(TLS_ALIAS, privateKey, certChainArray);
        }
        
        // save the settings in configuration
        configuration.set(TRUST_AGENT_TLS_CERT_SHA384, Sha384Digest.digestOf(publicKeyCert.getEncoded()).toString());
        configuration.set(TRUST_AGENT_TLS_CERT_DN, dn);
        if (ip != null && !ip.isEmpty()) {
            configuration.set(TRUST_AGENT_TLS_CERT_IP, ip);
        }
        if (dns != null && !dns.isEmpty()) {
            configuration.set(TRUST_AGENT_TLS_CERT_DNS, dns);
        }
        provider.save(configuration);
    }
    
    public void validateInput() throws Exception {
        if (options == null || !options.containsKey("private-key") || !options.containsKey("cert-chain")) {
            throw new IllegalArgumentException(String.format("\n%s", USAGE));
        }
        File privateKeyFile = new File(options.getString("private-key"));
        if (!privateKeyFile.exists()) {
            throw new FileNotFoundException("\nPrivate key file specified does not exist or user does not have required read permission");
        }
        File certChainFile = new File(options.getString("cert-chain"));
        if (!certChainFile.exists()) {
            throw new FileNotFoundException("\nCertificate chain file specified does not exist or user does not have required read permission");
        }
        
        String privateKeyFileContent = FileUtils.readFileToString(privateKeyFile, Charset.forName("UTF-8"));
        if (privateKeyFileContent.contains("ENCRYPTED PRIVATE KEY")) {
            throw new IllegalArgumentException("\nPassphrase protected private key not supported");
        }
        privateKey = RsaUtil.decodePemPrivateKey(privateKeyFileContent);
        if (privateKey == null) {
            throw new IllegalArgumentException("\nPrivate key file specified is not in PEM format");
        }
        
        x509CertChainList = X509Util.decodePemCertificates(FileUtils.readFileToString(certChainFile, Charset.forName("UTF-8")));
        if (x509CertChainList == null || x509CertChainList.isEmpty()) {
            throw new IllegalArgumentException("\nCertificate chain file specified is not in PEM format");
        }
        // Check if last cert is self signed?
        // Check if first cert is compatible with private key?
        
        X509Certificate publicKeyCert = x509CertChainList.get(0);
        if (publicKeyCert == null) {
            throw new IllegalArgumentException("\nCannot establish certificate from file specified");
        }
        
        // get subject distinguished name (DN)
        Principal publicKeyCertSubject = publicKeyCert.getSubjectDN();
        if (publicKeyCertSubject == null || publicKeyCertSubject.getName() == null) {
            throw new IllegalArgumentException("\nSubject distinguished name must be specified within certificate");
        }
        dn = publicKeyCertSubject.getName();
        if (dn.isEmpty()) {
            throw new IllegalArgumentException("\nSubject distinguished name must be specified within certificate");
        }
        
        List<String> ipList = new ArrayList();
        List<String> dnsList = new ArrayList();
        
        Set<String> altNames = X509Util.alternativeNames(publicKeyCert);
        if (altNames == null || altNames.isEmpty()) {
            throw new IllegalArgumentException("\nSubject alternative names must be specified within certificate");
        }
        for (String altName : altNames) {
            if (InetAddressValidator.getInstance().isValid(altName)) {
                ipList.add(altName);
            } else {
                dnsList.add(altName);
            }
        }
        
        ip = null;
        if (!ipList.isEmpty()) {
            ip = StringUtils.join(ipList, ",");
        }
        dns = null;
        if (!dnsList.isEmpty()) {
            dns = StringUtils.join(dnsList, ",");
        }
    }
}
