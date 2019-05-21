/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import com.intel.dcsg.cpg.crypto.SimpleKeystore;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.dcsg.cpg.x509.X509Util;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.client.jaxrs.PrivacyCA;
import com.intel.mtwilson.core.tpm.Tpm;
import com.intel.mtwilson.core.tpm.Tpm.CredentialType;
import com.intel.mtwilson.core.common.tpm.model.IdentityProofRequest;
import com.intel.mtwilson.core.common.tpm.model.IdentityRequest;
import com.intel.mtwilson.privacyca.v2.model.IdentityChallengeRequest;
import com.intel.mtwilson.privacyca.v2.model.IdentityChallengeResponse;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import gov.niarl.his.privacyca.IdentityOS;
import gov.niarl.his.privacyca.TpmIdentityRequest;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import com.intel.mtwilson.crypto.password.GuardedPassword;


/**
 *
 * @author jbuhacoff
 */
public class RequestAikCertificate extends AbstractSetupTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RequestAikCertificate.class);
    private TrustagentConfiguration config;
    private SimpleKeystore keystore;
    private X509Certificate privacyCA;
    private String url;
    private String username;
    private  GuardedPassword guardedPassword = new GuardedPassword();
    private byte[] ekCert;

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

        if (config.getTrustagentKeystoreFile().exists()) {
            keystore = new SimpleKeystore(new FileResource(config.getTrustagentKeystoreFile()), config.getTrustagentKeystorePassword());
            try {
                privacyCA = keystore.getX509Certificate("privacy", SimpleKeystore.CA);
            } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException | CertificateEncodingException e) {
                log.debug("Cannot load Privacy CA certificate", e);
                configuration("Privacy CA certificate is missing");
            }
        } else {
            configuration("Keystore file is missing");
        }
        Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
        if (!tpm.isOwnedWithAuth(config.getTpmOwnerSecret())) {
            configuration("Trust Agent is not the TPM owner");
            return;
        }
        // we need an EC in order to request an AIK, so make sure we have it
        /* add the code to deal with Windows platform and TPM 2.0 in Linux */
 /* return for now since Windows usually take the ownership of TPM be default 
                    * need to check later for exceptions
         */
        try {
            //#5819: Call to static method 'com.intel.mtwilson.trustagent.tpmmodules.Tpm.getTpm' via instance reference.
            //Tpm tpm = new Tpm();
            ekCert = tpm.getCredential(config.getTpmOwnerSecret(), CredentialType.EC);
            if (ekCert == null || ekCert.length == 0) {
                configuration("Endorsement Certificate is null or zero-length");
            }
        } catch (Tpm.TpmCredentialMissingException e) {
            configuration("Endorsement Certificate is missing");
        } catch (Tpm.TpmException e) {
            configuration("Cannot determine presence of Endorsement Certificate: %s", e.getMessage());
        }
    }

    @Override
    protected void validate() throws Exception {
        File aikCertificateFile = config.getAikCertificateFile();
        if (!aikCertificateFile.exists()) {
            validation("AIK has not been created");
            return;
        }

        X509Certificate aikCertificate = X509Util.decodePemCertificate(FileUtils.readFileToString(aikCertificateFile));
        try {
            aikCertificate.verify(privacyCA.getPublicKey());
        } catch (SignatureException e) {
            validation("Known Privacy CA did not sign AIK", e);
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException e) {
            validation("Unable to verify AIK", e);
        }
    }

    @Override
    protected void execute() throws Exception {
        try {
            Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
            TrustagentConfiguration taConfig = new TrustagentConfiguration(getConfiguration());
            SimpleKeystore taKeystore = new SimpleKeystore(new FileResource(taConfig.getTrustagentKeystoreFile()), taConfig.getTrustagentKeystorePassword());
            X509Certificate privacy = taKeystore.getX509Certificate("privacy", SimpleKeystore.CA);
            TpmIdentityRequest encryptedEkCert = new TpmIdentityRequest(ekCert, (RSAPublicKey) privacy.getPublicKey(), false);
            //boolean shortcut = false;
            IdentityRequest newId = tpm.collateIdentityRequest(taConfig.getTpmOwnerSecret(), taConfig.getAikSecret(), privacy.getPublicKey());
            if (IdentityOS.isWindows()) {
                /* Call Windows API to get the TPM EK certificate and assign it to "ekCert" */

                // write the AikOpaque to file
                String aikblob = taConfig.getAikBlobFile().getAbsolutePath();
                writeBlob(aikblob, newId.getAikBlob());
            }
            TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(taConfig.getTrustagentKeystoreFile(), taConfig.getTrustagentKeystorePassword()).build();
            TlsConnection tlsConnection = new TlsConnection(new URL(taConfig.getMtWilsonApiUrl()), tlsPolicy);

            Properties clientConfiguration = new Properties();
            clientConfiguration.setProperty(TrustagentConfiguration.MTWILSON_API_USERNAME, taConfig.getMtWilsonApiUsername());
            clientConfiguration.setProperty(TrustagentConfiguration.MTWILSON_API_PASSWORD, taConfig.getMtWilsonApiPassword());

            // send the identity request to the privacy ca to get a challenge
            PrivacyCA client = new PrivacyCA(clientConfiguration, tlsConnection);

            IdentityChallengeRequest request = new IdentityChallengeRequest();
            request.setEndorsementCertificate(encryptedEkCert.toByteArray());
            request.setIdentityRequest(newId);
            IdentityProofRequest identityChallenge = client.identityChallengeRequest(request);
            byte[] decrypted1 = tpm.activateIdentity(taConfig.getTpmOwnerSecret(), taConfig.getAikSecret(), identityChallenge);
            // send the answer and receive the AIK certificate
            // even thoug hits called TpmIdentityRequest, it's just standard RSA encryption... nothing about it needs to be using TPM structures.
            // this is because we are encrypting using the PrivacyCA's public key. 
            TpmIdentityRequest encryptedChallenge = new TpmIdentityRequest(decrypted1, (RSAPublicKey) privacy.getPublicKey(), false);
            System.err.println("Create Identity... Calling into HisPriv second time, size of msg = " + encryptedChallenge.toByteArray().length);

            IdentityChallengeResponse identityChallengeResponse = new IdentityChallengeResponse();
            identityChallengeResponse.setResponseToChallenge(encryptedChallenge.toByteArray());
            identityChallengeResponse.setIdentityRequest(newId);
            IdentityProofRequest identityBlob = client.identityChallengeResponse(identityChallengeResponse);
            String aikcertfilepath = taConfig.getAikCertificateFile().getAbsolutePath();
            String aikblobfilepath = taConfig.getAikBlobFile().getAbsolutePath();
            byte[] decrypted2 = tpm.activateIdentity(taConfig.getTpmOwnerSecret(), taConfig.getAikSecret(), identityBlob);
            writeBlob(aikblobfilepath, newId.getAikBlob());
            writeCert(aikcertfilepath, decrypted2);
        } catch (Exception e) {
            System.err.println("Exception while provisioning AIK, message: " + e.getMessage());
        }
    }

    private static void writeBlob(String absoluteFilePath, byte[] encryptedBytes) throws IOException {
        File file = new File(absoluteFilePath);
        makeDir(file); // ensure the parent directory exists
        try (FileOutputStream out = new FileOutputStream(file)) { // throws FileNotFoundException
            IOUtils.write(encryptedBytes, out); // throws IOException
        }
    }

    private static void makeDir(File file) throws IOException {
        if (!file.getParentFile().isDirectory()) {
            if (!file.getParentFile().mkdirs()) {
                log.warn("Failed to create client installation path!");
                throw new IOException("Failed to create client installation path!");
            }
        }
    }

    private static void writeCert(String absoluteFilePath, byte[] certificateBytes) throws FileNotFoundException, java.security.cert.CertificateException, IOException {
        File file = new File(absoluteFilePath);
        makeDir(file); // ensure the parent directory exists
        X509Certificate certificate = X509Util.decodeDerCertificate(certificateBytes); // throws CertificateException
        String certificatePem = X509Util.encodePemCertificate(certificate);
        try (FileOutputStream out = new FileOutputStream(file)) { // throws FileNotFoundException
            IOUtils.write(certificatePem, out); // throws IOException
        }
    }

    public static byte[] fixMakeCredentialBlobForWindows(byte[] in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        final int SECRET_SIZE = 134;
        //final int ASYM_SIZE = 256 + 2;
        ByteBuffer buf = ByteBuffer.wrap(in);
        int secretLength = buf.order(ByteOrder.LITTLE_ENDIAN).getShort();
        out.writeShort((short) secretLength);
        byte[] b = new byte[secretLength];
        buf.get(b);
        out.write(b);
        buf.position(SECRET_SIZE);
        int asymLength = buf.order(ByteOrder.LITTLE_ENDIAN).getShort();
        out.writeShort((short) asymLength);
        byte[] c = new byte[asymLength];
        buf.get(c);
        out.write(c);
        return bos.toByteArray();
    }
}
