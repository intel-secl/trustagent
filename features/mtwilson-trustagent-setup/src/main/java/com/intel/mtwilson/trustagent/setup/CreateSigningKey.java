/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.core.tpm.Tpm;
import com.intel.mtwilson.core.tpm.Tpm.KeyType;
import com.intel.mtwilson.core.tpm.model.CertifiedKey;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import gov.niarl.his.privacyca.TpmCertifyKey;
import java.io.File;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author ssbangal
 */
public class CreateSigningKey extends AbstractSetupTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateSigningKey.class);
    private TrustagentConfiguration trustagentConfiguration;
    private File signingKeyBlob;
    private File signingKeyModulus;
    private File signingKeyTCGCertificate;
    private File signingKeyTCGCertificateSignature;
    private File signingKeyOpaqueBlob;
    private File signingKeyName;

    @Override
    protected void configure() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
    }

    @Override
    protected void validate() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        String signingKeySecretHex = trustagentConfiguration.getSigningKeySecretHex();
        if (signingKeySecretHex == null || signingKeySecretHex.isEmpty()) {
            validation("Signing key secret is not set");
        }

        // Now check for the existence of the signing private/public key and the tcg standard signing certificate from the 
        // certifyKey output.
        signingKeyBlob = trustagentConfiguration.getSigningKeyBlobFile();
        if (signingKeyBlob == null || !signingKeyBlob.exists()) {
            validation("Private component of signing key does not exist.");
        }

        signingKeyTCGCertificate = trustagentConfiguration.getSigningKeyTCGCertificateFile();
        if (signingKeyTCGCertificate == null || !signingKeyTCGCertificate.exists()) {
            validation("TCG standard certificate for the signing key does not exist.");
        }

        signingKeyTCGCertificateSignature = trustagentConfiguration.getSigningKeyTCGCertificateSignatureFile();
        if (signingKeyTCGCertificateSignature == null || !signingKeyTCGCertificateSignature.exists()) {
            validation("TCG standard certificate for the signing key does not exist.");
        }

        signingKeyModulus = trustagentConfiguration.getSigningKeyModulusFile();
        if (signingKeyModulus == null || !signingKeyModulus.exists()) {
            validation("Public component of signing key does not exist.");
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0) { //Windows
            signingKeyOpaqueBlob = trustagentConfiguration.getSigningKeyOpaqueBlobFile();
            if (signingKeyOpaqueBlob == null || !signingKeyOpaqueBlob.exists()) {
                validation("Opaque blob component of signing key does not exist.");
            }
        }
    }

    @Override
    protected void execute() throws Exception {
        try {
            log.info("Starting the process to create the TCG standard signing key certificate");

            String signingKeySecretHex = RandomUtil.randomHexString(20);
            log.debug("Generated random Signing key secret");

            getConfiguration().set(TrustagentConfiguration.SIGNING_KEY_SECRET, signingKeySecretHex);
            Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
            // Call into the TpmModule certifyKey function to create the signing key and certify the same using the AIK so that we have the chain of trust.
            CertifiedKey certifiedKey = tpm.createAndCertifyKey(KeyType.SIGN, trustagentConfiguration.getSigningKeySecret(), trustagentConfiguration.getAikSecret());

            // Store the public key modulus, tcg standard certificate (output of certifyKey) & the private key blob.
            signingKeyBlob = trustagentConfiguration.getSigningKeyBlobFile();
            signingKeyTCGCertificate = trustagentConfiguration.getSigningKeyTCGCertificateFile();
            signingKeyModulus = trustagentConfiguration.getSigningKeyModulusFile();
            signingKeyTCGCertificateSignature = trustagentConfiguration.getSigningKeyTCGCertificateSignatureFile();
            signingKeyName = trustagentConfiguration.getSigningKeyNameFile();

            log.debug("Blob path is : {}", signingKeyBlob.getAbsolutePath());
            log.debug("TCG Cert path is : {}", signingKeyTCGCertificate.getAbsolutePath());
            log.debug("TCG Cert signature path is : {}", signingKeyTCGCertificateSignature.getAbsolutePath());
            log.debug("Public key modulus path is : {}", signingKeyModulus.getAbsolutePath());

            FileUtils.writeByteArrayToFile(signingKeyModulus, certifiedKey.getKeyModulus());
            FileUtils.writeByteArrayToFile(signingKeyBlob, certifiedKey.getKeyBlob());
            FileUtils.writeByteArrayToFile(signingKeyTCGCertificate, certifiedKey.getKeyData());
            FileUtils.writeByteArrayToFile(signingKeyTCGCertificateSignature, certifiedKey.getKeySignature());
            if (certifiedKey.getKeyName() != null) {
                FileUtils.writeByteArrayToFile(signingKeyName, certifiedKey.getKeyName());
            }

            String os = System.getProperty("os.name").toLowerCase();
            if (os.indexOf("win") >= 0) { //Windows
                FileUtils.writeByteArrayToFile(signingKeyOpaqueBlob, certifiedKey.getKeyBlob());
                log.debug("Opaque blob path is : {}", signingKeyOpaqueBlob.getAbsolutePath());
            }

            if (tpm.getTpmVersion().equals("1.2")) {
                TpmCertifyKey tpmCertifyKey = new TpmCertifyKey(certifiedKey.getKeyData());
                log.debug("TCG Signing Key contents: {} - {}", tpmCertifyKey.getKeyParms().getAlgorithmId(), tpmCertifyKey.getKeyParms().getTrouSerSmode());
            }
        } catch (Exception e) {
            log.error("Failed to create signing key");
            throw e;
        }
    }
}
