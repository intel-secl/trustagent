/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.crypto.Sha1Digest;
import com.intel.dcsg.cpg.crypto.SimpleKeystore;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.dcsg.cpg.x509.X509Util;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.privacyca.v2.model.CaCertificateFilterCriteria;
import com.intel.mtwilson.client.jaxrs.CaCertificates;
import com.intel.mtwilson.client.jaxrs.PrivacyCA;
import com.intel.mtwilson.core.tpm.Tpm;
import com.intel.mtwilson.core.tpm.Tpm.CredentialType;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.tpm.endorsement.client.jaxrs.TpmEndorsements;
import com.intel.mtwilson.tpm.endorsement.model.TpmEndorsement;
import com.intel.mtwilson.tpm.endorsement.model.TpmEndorsementCollection;
import com.intel.mtwilson.tpm.endorsement.model.TpmEndorsementFilterCriteria;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import gov.niarl.his.privacyca.TpmUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import com.intel.mtwilson.crypto.password.GuardedPassword;

/**
 *
 * @author jbuhacoff
 */
public class RequestEndorsementCertificate extends AbstractSetupTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RequestEndorsementCertificate.class);
    private TrustagentConfiguration config;
    private File keystoreFile;
    private SimpleKeystore keystore;
    private String tpmOwnerSecretHex;
    private String url;
    private String username;
    private GuardedPassword guardedPassword = new GuardedPassword();
    private X509Certificate ekCert;
    private CaCertificates caCertificatesClient;
    private TpmEndorsements tpmEndorsementsClient;
    private File endorsementAuthoritiesFile;
    private List<X509Certificate> endorsementAuthorities;
    private UUID hostHardwareId;

    @Override
    protected void configure() throws Exception {
        config = new TrustagentConfiguration(getConfiguration());

        url = config.getMtWilsonApiUrl();
        username = config.getMtWilsonApiUsername();
        guardedPassword.setPassword(config.getMtWilsonApiPassword());
        if (url == null || url.isEmpty()) {
            configuration("Mt Wilson URL [mtwilson.api.url] must be set");
        }
        if (username == null || username.isEmpty()) {
            configuration("Mt Wilson username [mtwilson.api.username] must be set");
        }
        if (!guardedPassword.isPasswordValid()) {
            configuration("Mt Wilson password [mtwilson.api.password] must be set");
        }

        tpmOwnerSecretHex = config.getTpmOwnerSecretHex(); // we check it here because ProvisionTPM calls getOwnerSecret() which relies on this
        if (tpmOwnerSecretHex == null) {
            configuration("TPM Owner Secret is not configured: %s", TrustagentConfiguration.TPM_OWNER_SECRET); // this constant is the name of the property, literally "tpm.owner.secret"
        }
        Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
        if (!tpm.isOwnedWithAuth(config.getTpmOwnerSecret())) {
            configuration("Trust Agent is not the TPM owner");
        }

        keystoreFile = config.getTrustagentKeystoreFile();
        /*
        if (keystoreFile.exists()) {
            keystore = new SimpleKeystore(new FileResource(keystoreFile), config.getTrustagentKeystorePassword());
            try {
                X509Certificate endorsementCA = keystore.getX509Certificate("endorsement", SimpleKeystore.CA);
                log.debug("Endorsement CA {}", Sha1Digest.digestOf(endorsementCA.getEncoded()).toHexString());
            } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException | CertificateEncodingException e) {
                configuration("Endorsement CA certificate cannot be loaded");
            }
        } else {
            configuration("Keystore file is missing");
        }
         */

        endorsementAuthoritiesFile = config.getEndorsementAuthoritiesFile();
        if (endorsementAuthoritiesFile == null) {
            configuration("Endorsement authorities file location is not set");
        }

        try {
            tpmEndorsementsClient = new TpmEndorsements(config.getMtWilsonClientProperties());
        } catch (Exception e) {
            log.error("Cannot configure TPM Endorsements API client", e);
            configuration(e, "Cannot configure TPM Endorsements API client");
        }

        try {
            caCertificatesClient = new CaCertificates(config.getMtWilsonClientProperties());
        } catch (Exception e) {
            configuration(e, "Cannot configure CA Certificates API client");
        }

        String hostHardwareIdHex = config.getHardwareUuid();
        if (hostHardwareIdHex == null || hostHardwareIdHex.isEmpty() || !UUID.isValid(hostHardwareIdHex)) {
            configuration("Host hardware UUID [hardware.uuid] must be set");
        } else {
            hostHardwareId = UUID.valueOf(hostHardwareIdHex);
        }

    }

    @Override
    protected void validate() throws Exception {
        try {
            readEndorsementCertificate();
        } catch (Exception e) {
            validation(e, "Cannot read endorsement certificate");
        }

        /*
        X509Certificate endorsementCA = keystore.getX509Certificate("endorsement", SimpleKeystore.CA);
        try {
            ekCert.verify(endorsementCA.getPublicKey());
        } catch (SignatureException e) {
            validation("Known Endorsement CA did not sign TPM EC", e);
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException e) {
            validation("Unable to verify TPM EC", e);
        }
         */
        if (!endorsementAuthoritiesFile.exists()) {
            validation("Endorsement authorities file is missing");
        }
        if (endorsementAuthorities == null || endorsementAuthorities.isEmpty()) {
            validation("No endorsement authorities");
        } else {
            String errorMessage;
            log.debug("Found {} endorsement authorities in {}", endorsementAuthorities.size(), endorsementAuthoritiesFile.getAbsolutePath());
            // if we find one certificate authority that can verify our current EC, then we don't need to request a new EC
            if (!isEkSignedByEndorsementAuthority()) {
                errorMessage = String.format("Unable to verify TPM EC with %d authorities from %s.", endorsementAuthorities.size(), endorsementAuthoritiesFile.getAbsolutePath());

                // check if we have registered with MTW
                if (!isEkRegisteredWithMtWilson()) {
                    errorMessage += "EC is also not registered with Mt.Wilson";
                    validation(errorMessage);
                }

//                validation("Unable to verify TPM EC with %d authorities from %s", endorsementAuthorities.size(), endorsementAuthoritiesFile.getAbsolutePath());
            }
        }
    }

    @Override
    protected void execute() throws Exception {
        /*
        System.setProperty("javax.net.ssl.trustStore", config.getTrustagentKeystoreFile().getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", config.getTrustagentKeystorePassword());
        System.setProperty("javax.net.ssl.keyStore", config.getTrustagentKeystoreFile().getAbsolutePath());
        System.setProperty("javax.net.ssl.keyStorePassword", config.getTrustagentKeystorePassword());
         */

        // try to read the local EC from the TPM ; will set tpmEndorsementCertificate if successful, or leave it null if unsuccessful
        readEndorsementCertificate();

        // first check if we have an EC and if it can be validated against known manufacturer CA certs
        log.debug("RequestEndorsementCertificate checking if EC is issued by known manufacturer");
        downloadEndorsementAuthorities();
        if (isEkSignedByEndorsementAuthority()) {
            log.debug("EC is already issued by endorsement authority; no need to request new EC");
            return;
        }

        // second check if we have an EC and if it's already registered with Mt Wilson
        log.debug("RequestEndorsementCertificate checking if EC is registered with Mt Wilson");
        if (ekCert != null && isEkRegisteredWithMtWilson()) {
            log.debug("EK is already registered with Mt Wilson; no need to request an EC");
            return;
        }

        // now if we have an EC register it with Mt Wilson
        if (ekCert != null) {
            log.debug("RequestEndorsementCertificate registering EC with Mt Wilson");
            registerEkWithMtWilson();
        } else {
            // otherwise if we don't have an EC try to get our EK endorsed by Mt Wilson and install the received EC in TPM NNRAM
            log.debug("RequestEndorsementCertificate endorsing EC with Mt Wilson");
            endorseTpmWithMtWilson();
        }
    }

    private void readEndorsementCertificate() throws IOException, CertificateException {
        byte[] ekCertBytes;
        Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));/* Linux -- Also need to distinguish between TPM 1.2 and TPM 2.0 */
        try {
            ekCertBytes = tpm.getCredential(config.getTpmOwnerSecret(), CredentialType.EC);
            log.debug("EC base64: {}", Base64.encodeBase64String(ekCertBytes));
            ekCert = X509Util.decodeDerCertificate(ekCertBytes);
        } catch (Tpm.TpmException e) {
            ekCert = null;
            log.debug("EC not present on TPM");
        }
    }

    private void downloadEndorsementAuthorities() throws IOException, CertificateException {
        // we create or replace our endorsement.pem file with what mtwilson provides
        // because it's mtwilson that will be evaluating it anyway in order to 
        // issue AIK certiicates later,  and because it's handy for the admin to
        // see locally the list of certs for troubleshooting -- so this could be
        // converted to getting the array of X509Certificate objects directly 
        // from the client without saving anything to disk. 

        CaCertificateFilterCriteria criteria = new CaCertificateFilterCriteria();
        criteria.domain = "ek"; // or "endorsement"
        String endorsementAuthoritiesPem = caCertificatesClient.searchCaCertificatesPem(criteria);
        try (OutputStream out = new FileOutputStream(endorsementAuthoritiesFile)) {
            IOUtils.write(endorsementAuthoritiesPem, out);
        }
        try (InputStream in = new FileInputStream(endorsementAuthoritiesFile)) {
            String pem = IOUtils.toString(in);
            endorsementAuthorities = X509Util.decodePemCertificates(pem);
            log.debug("Found {} endorsement authorities in {}", endorsementAuthorities.size(), endorsementAuthoritiesFile.getAbsolutePath());
        }
    }

    private boolean isEkSignedByEndorsementAuthority() {
        for (X509Certificate ca : endorsementAuthorities) {
            try {
                log.debug("Trying to verify EC with {}", ca.getSubjectX500Principal().getName());
                if (ekCert != null) {
                    ekCert.verify(ca.getPublicKey());
                    log.debug("Verified EC with {}", ca.getSubjectX500Principal().getName());
                    return true;
                }
            } catch (SignatureException e) {
                log.debug("Endorsement CA '{}' did not sign TPM EC: {}", ca.getSubjectX500Principal().getName(), e.getMessage());
            } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | NullPointerException e) {
                log.debug("Unable to verify TPM EC '{}': {}", ca.getSubjectX500Principal().getName(), e.getMessage());
            }
        }
        return false;
    }

    private boolean isEkRegisteredWithMtWilson() throws JsonProcessingException {
        TpmEndorsementFilterCriteria criteria = new TpmEndorsementFilterCriteria();
        criteria.hardwareUuidEqualTo = hostHardwareId.toString();
        TpmEndorsementCollection collection = tpmEndorsementsClient.searchTpmEndorsements(criteria);
        if (collection.getTpmEndorsements().isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            log.debug("Did not find EC with search criteria {}", mapper.writeValueAsString(criteria));
            return false;
        }
        log.debug("Found EC by hardware uuid");
        return true;
    }

    private void registerEkWithMtWilson() throws CertificateEncodingException {
        TpmEndorsement tpmEndorsement = new TpmEndorsement();
        tpmEndorsement.setId(new UUID());
        tpmEndorsement.setCertificate(ekCert.getEncoded());
        tpmEndorsement.setComment("registered by trust agent");
        tpmEndorsement.setHardwareUuid(hostHardwareId.toString());
        tpmEndorsement.setIssuer(ekCert.getIssuerDN().getName().replaceAll("\\x00", "")); // should be automatically set by server upon receiving the cert
        tpmEndorsement.setRevoked(false); // should default to false on server
        tpmEndorsementsClient.createTpmEndorsement(tpmEndorsement);
    }

    private void endorseTpmWithMtWilson() throws IOException, Tpm.TpmException, CertificateEncodingException {
        TrustagentConfiguration config = new TrustagentConfiguration(getConfiguration());
        Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
        byte[] pubEkMod = tpm.getEndorsementKeyModulus(config.getTpmOwnerSecret());
        log.debug("Public EK Modulus: {}", TpmUtils.byteArrayToHexString(pubEkMod));
        log.debug("Requesting TPM endorsement from Privacy CA");
        TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(config.getTrustagentKeystoreFile(), config.getTrustagentKeystorePassword()).build();
        TlsConnection tlsConnection = new TlsConnection(new URL(config.getMtWilsonApiUrl()), tlsPolicy);
        Properties clientConfiguration = new Properties();
        clientConfiguration.setProperty(TrustagentConfiguration.MTWILSON_API_USERNAME, config.getMtWilsonApiUsername());
        clientConfiguration.setProperty(TrustagentConfiguration.MTWILSON_API_PASSWORD, config.getMtWilsonApiPassword());
        PrivacyCA pcaClient;
        try {
            pcaClient = new PrivacyCA(clientConfiguration, tlsConnection);
        } catch (Exception e) {
            log.error("Unable to create Privacy CA client");
            throw new RuntimeException("Unable to create Privacy CA client " + ExceptionUtils.getFullStackTrace(e));
        }
        X509Certificate ekCert = pcaClient.endorseTpm(pubEkMod);
        log.debug("Received EC SHA1: {}", Sha1Digest.digestOf(ekCert.getEncoded()).toHexString());
        // write the EC to the TPM NVRAM
        tpm.setCredential(config.getTpmOwnerSecret(), CredentialType.EC, ekCert.getEncoded());

    }
}
