/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.trustagent.setup;

import com.intel.dcsg.cpg.crypto.CryptographyException;
import com.intel.dcsg.cpg.crypto.RsaCredentialX509;
import com.intel.dcsg.cpg.crypto.SimpleKeystore;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.mtwilson.core.common.utils.AASTokenFetcher;
import com.intel.mtwilson.jaxrs2.client.MtWilsonClient;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.tls.policy.TlsPolicyDescriptor;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyFactoryUtil;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import com.intel.mtwilson.trustagent.as.rest.v2.model.Host;
import com.intel.mtwilson.trustagent.as.rest.v2.model.HostCollection;
import java.io.File;
import java.net.URL;
import java.util.Properties;
import com.intel.mtwilson.trustagent.as.rest.v2.model.HostCreateCriteria;
import com.intel.mtwilson.trustagent.as.rest.v2.model.HostFilterCriteria;
import com.intel.mtwilson.trustagent.attestation.client.jaxrs.Hosts;
import java.io.FileNotFoundException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.exception.ExceptionUtils;
import com.intel.mtwilson.crypto.password.GuardedPassword;
import java.security.GeneralSecurityException;
import java.io.IOException;

/**
 *
 * @author hmgowda
 */
public class AttestationRegistration extends AbstractSetupTask{
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttestationRegistration.class);
    private static final String TLS_ALIAS = "tls";

    private TrustagentConfiguration trustagentConfiguration;
    private GuardedPassword keystoreGuardedPassword = new GuardedPassword();
    private SimpleKeystore keystore;
    private String currentIp;
    private TlsConnection tlsConnection;
    private Properties clientConfiguration = new Properties();
    private File truststoreFile;
    private String truststorePassword;

    @Override
    protected void configure() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        String url = trustagentConfiguration.getMtWilsonApiUrl();
        if( url == null || url.isEmpty() ) {
            configuration("Mt Wilson URL is not set");
        }
        String username = trustagentConfiguration.getTrustAgentAdminUserName();
        if (username == null || username.isEmpty()) {
            configuration("TA admin username is not set");
        }
        String password = trustagentConfiguration.getTrustAgentAdminPassword();
        if (password == null || password.isEmpty()) {
            configuration("TA admin password is not set");
        }
        String aasApiUrl = trustagentConfiguration.getAasApiUrl();
        if (aasApiUrl == null || aasApiUrl.isEmpty()) {
            configuration("AAS API URL is not set");
        }
        currentIp = trustagentConfiguration.getCurrentIp();
        if( currentIp == null || currentIp.isEmpty() ) {
            configuration("Current IP is not set");
        }
        File truststoreFile = trustagentConfiguration.getTrustagentTruststoreFile();
        if( truststoreFile == null || !truststoreFile.exists() ) {
            configuration("Trust Agent truststore does not exist");
        }
        truststorePassword = trustagentConfiguration.getTrustagentTruststorePassword();
        keystoreGuardedPassword.setPassword(truststorePassword);

        if( !keystoreGuardedPassword.isPasswordValid() ) {
            configuration("Trust Agent truststore password is not set");
        }
        keystore = new SimpleKeystore(new FileResource(truststoreFile), keystoreGuardedPassword.getInsPassword());
        TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(truststoreFile, truststorePassword).build();

        tlsConnection = new TlsConnection(new URL(trustagentConfiguration.getMtWilsonApiUrl()), tlsPolicy);
        clientConfiguration.setProperty(TrustagentConfiguration.BEARER_TOKEN, new AASTokenFetcher().getAASToken(username, password, new TlsConnection(new URL(aasApiUrl), tlsPolicy)));

    }

    @Override
    protected void validate() throws Exception {

        Hosts hostsClient = new Hosts(clientConfiguration, tlsConnection);
        HostFilterCriteria filterCriteria = new HostFilterCriteria();
        filterCriteria.nameEqualTo = currentIp;
        HostCollection hostCollection = hostsClient.searchHosts(filterCriteria);
        if(!(hostCollection != null && hostCollection.getHosts() != null && !hostCollection.getHosts().isEmpty() && hostCollection.getHosts().get(0) != null)){
            validation("Host Registration for host {} was unsuccessful", currentIp);
        }
    }
    
    @Override
    protected void execute() throws Exception {
        String distro = "intel";
        if(System.getProperty("os.name").toLowerCase().contains("windows")) {
            distro = "microsoft";
        }
        
        String connectionString = String.format("%s:https://%s:%s", distro, currentIp, trustagentConfiguration.getTrustagentHttpTlsPort());

        clientConfiguration.setProperty(TrustagentConfiguration.CLIENT_CONNECT_TIMEOUT, "10");
        clientConfiguration.setProperty(TrustagentConfiguration.CLIENT_READ_TIMEOUT, "60");
        
        log.debug("the os name is {}", System.getProperty("os.name"));
        log.debug("connection string is {}", connectionString);
        
        HostTlsPolicy hostTlsPolicy = getTlsPolicy(clientConfiguration, tlsConnection);
        
        Hosts hostsClient = new Hosts(clientConfiguration, tlsConnection);
        
        //Making client call to HVS for creating a host
        HostFilterCriteria hostFilterCriteria = new HostFilterCriteria();
        hostFilterCriteria.nameEqualTo = currentIp;
        HostCollection existingHostCollection = hostsClient.searchHosts(hostFilterCriteria);
        
        if (existingHostCollection != null && existingHostCollection.getHosts() != null 
                && !existingHostCollection.getHosts().isEmpty()) {
            Host existingHost = existingHostCollection.getHosts().get(0);
            Host host = new Host();
            host.setId(existingHost.getId());
            host.setTlsPolicyId(hostTlsPolicy.getId().toString());
            hostsClient.editHost(host);
            log.warn("Host with name {} already exists", existingHost.getHostName());
        }
        else { 
            //Building create criteria for client call
            HostCreateCriteria hostCreateCriteria = new HostCreateCriteria();
            hostCreateCriteria.setConnectionString(connectionString);
            hostCreateCriteria.setHostName(currentIp);
            hostCreateCriteria.setTlsPolicyId(hostTlsPolicy.getId());
            hostsClient.createHost(hostCreateCriteria);
            log.info("Host with name {} created", currentIp);
        }
    }
    
    private HostTlsPolicy getTlsPolicy(Properties clientConfiguration, TlsConnection tlsConnection) throws CertificateException, CertificateEncodingException {
        TlsPolicyDescriptor tlsPolicyDescriptor = new TlsPolicyDescriptor();
        tlsPolicyDescriptor.setPolicyType("certificate");
        tlsPolicyDescriptor.setProtection(TlsPolicyFactoryUtil.getAllTlsProtection());
        tlsPolicyDescriptor.setMeta(new HashMap());
        tlsPolicyDescriptor.getMeta().put("encoding", "base64");
        RsaCredentialX509 rsaCert = getCertificateData();
        X509Certificate certificate = null;
        if (rsaCert != null) {
            certificate = rsaCert.getCertificate();
        if ( certificate != null ) {
            tlsPolicyDescriptor.setData(new HashSet());
            String certificateString = Base64.encodeBase64String(certificate.getEncoded());
            tlsPolicyDescriptor.getData().add(certificateString);
            log.debug("Added certificate to TLS policy: {}", certificateString);
        } else {
            log.error("getCertificate failed for X509Certificate.");
        }
        } else {
            log.error("getCertificateData failed for RsaCredentialX509.");
        }
        
        UUID id = new UUID();
        HostTlsPolicy tlsPolicy = new HostTlsPolicy();
        tlsPolicy.setId(id);
        tlsPolicy.setName(id.toString());
        tlsPolicy.setPrivate(true);
        tlsPolicy.setDescriptor(tlsPolicyDescriptor);
        MtWilsonClient mtWilsonClient;
        try {
            mtWilsonClient = new MtWilsonClient(clientConfiguration, tlsConnection);
        } catch (Exception e) {
            log.error("Unable to create MtWilson client");
            throw new RuntimeException("Unable to create Privacy CA client " + ExceptionUtils.getFullStackTrace(e));
        }
        log.debug("target: {}", mtWilsonClient.getTarget().getUri().toString());
        return mtWilsonClient.getTarget().path("tls-policies").request().accept(MediaType.APPLICATION_JSON).post(Entity.json(tlsPolicy), HostTlsPolicy.class);
    }
    
    private RsaCredentialX509 getCertificateData() throws CertificateException {
        RsaCredentialX509 credential = null;
        try {
            credential = keystore.getRsaCredentialX509(TLS_ALIAS, keystoreGuardedPassword.getInsPassword());
            if (credential == null || credential.getCertificate() == null 
                    || credential.getCertificate().getSubjectX500Principal() == null 
                    || credential.getCertificate().getSubjectX500Principal().getName() == null) {
                log.debug("Invalid TLS certificate: credential contains null value");
                throw new CertificateException("Invalid TLS certificate: credential contains null value.");
            }
            log.debug("Found TLS key {}", credential.getCertificate().getSubjectX500Principal().getName());
        } catch (FileNotFoundException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException | CertificateEncodingException | CryptographyException e) {
            log.warn("Unable to get certificate from trustagent properties.");
            throw new CertificateException("Unable to get certificate from trustagent properties.", e);
        } catch (GeneralSecurityException ex) {
            log.error("Failed to get credentials from trustagent properties. ", ex);
        } catch (IOException ex) {
            log.error("Unable to get certificate from trustagent properties. ", ex);
        }
        return credential;
    }
    
    public static class CertificateException extends Exception {  
        CertificateException(String msg){  
            super(msg);  
        }
        CertificateException(String msg, Exception e){  
            super(msg, e);  
        }
    }
}
