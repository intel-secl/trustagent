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
import java.util.HashMap;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author ssbangal
 */
public class CreateBindingKey extends AbstractSetupTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateBindingKey.class);
    private TrustagentConfiguration trustagentConfiguration;
    private File bindingKeyBlob;
    private File bindingKeyModulus;
    private File bindingKeyTCGCertificate;
    private File bindingKeyTCGCertificateSignature;
    private File bindingKeyOpaqueBlob;
    private File bindingKeyFileName;

    @Override
    protected void configure() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
    }

    @Override
    protected void validate() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        String bindingKeySecretHex = trustagentConfiguration.getBindingKeySecretHex();
        if (bindingKeySecretHex == null || bindingKeySecretHex.isEmpty()) {
            validation("Binding key secret is not set");
        }

        // Now check for the existence of the binding private/public key and the tcg standard binding certificate from the 
        // certifyKey output.
        bindingKeyBlob = trustagentConfiguration.getBindingKeyBlobFile();
        if (bindingKeyBlob == null || !bindingKeyBlob.exists()) {
            validation("Private component of binding key does not exist.");
        }

        bindingKeyTCGCertificate = trustagentConfiguration.getBindingKeyTCGCertificateFile();
        if (bindingKeyTCGCertificate == null || !bindingKeyTCGCertificate.exists()) {
            validation("TCG standard certificate for the binding key does not exist.");
        }

        bindingKeyTCGCertificateSignature = trustagentConfiguration.getBindingKeyTCGCertificateSignatureFile();
        if (bindingKeyTCGCertificateSignature == null || !bindingKeyTCGCertificateSignature.exists()) {
            validation("Signature file of the TCG standard certificate for the binding key does not exist.");
        }

        bindingKeyModulus = trustagentConfiguration.getBindingKeyModulusFile();
        if (bindingKeyModulus == null || !bindingKeyModulus.exists()) {
            validation("Public component of binding key does not exist.");
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0) { //Windows
            bindingKeyOpaqueBlob = trustagentConfiguration.getBindingKeyOpaqueBlobFile();
            if (bindingKeyOpaqueBlob == null || !bindingKeyOpaqueBlob.exists()) {
                validation("Opaque blob component of binding key does not exist.");
            }
        }
    }

    @Override
    protected void execute() throws Exception {
        try {

            log.info("Starting the process to create the TCG standard binding key certificate");

            String bindingKeySecretHex = RandomUtil.randomHexString(20);
            log.info("Generated random Binding key secret");

            getConfiguration().set(TrustagentConfiguration.BINDING_KEY_SECRET, bindingKeySecretHex);
            Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
            // Call into the TpmModule certifyKey function to create the binding key and certify the same using AIK to build the chain of trust.
            CertifiedKey certifiedKey = tpm.createAndCertifyKey(KeyType.BIND, trustagentConfiguration.getBindingKeySecret(), trustagentConfiguration.getAikSecret());

            // Store the public key modulus, tcg standard certificate (output of certifyKey) & the private key blob.
            bindingKeyBlob = trustagentConfiguration.getBindingKeyBlobFile();
            bindingKeyTCGCertificate = trustagentConfiguration.getBindingKeyTCGCertificateFile();
            bindingKeyModulus = trustagentConfiguration.getBindingKeyModulusFile();
            bindingKeyTCGCertificateSignature = trustagentConfiguration.getBindingKeyTCGCertificateSignatureFile();
            bindingKeyOpaqueBlob = trustagentConfiguration.getBindingKeyOpaqueBlobFile();
            bindingKeyFileName = trustagentConfiguration.getBindingKeyNameFile();

            log.debug("Blob path is : {}", bindingKeyBlob.getAbsolutePath());
            log.debug("TCG Cert path is : {}", bindingKeyTCGCertificate.getAbsolutePath());
            log.debug("TCG Cert signature path is : {}", bindingKeyTCGCertificateSignature.getAbsolutePath());
            log.debug("Public key modulus path is : {}", bindingKeyModulus.getAbsolutePath());

            FileUtils.writeByteArrayToFile(bindingKeyModulus, certifiedKey.getKeyModulus());
            FileUtils.writeByteArrayToFile(bindingKeyBlob, certifiedKey.getKeyBlob());
            FileUtils.writeByteArrayToFile(bindingKeyTCGCertificate, certifiedKey.getKeyData());
            FileUtils.writeByteArrayToFile(bindingKeyTCGCertificateSignature, certifiedKey.getKeySignature());
            // keyName is optional for 1.2
            if (certifiedKey.getKeyName() != null) {
                FileUtils.writeByteArrayToFile(bindingKeyFileName, certifiedKey.getKeyName());
            }

            String os = System.getProperty("os.name").toLowerCase();
            if (os.indexOf("win") >= 0) { //Windows
                FileUtils.writeByteArrayToFile(bindingKeyOpaqueBlob, certifiedKey.getKeyBlob());
                log.debug("Opaque blob path is : {}", bindingKeyOpaqueBlob.getAbsolutePath());
            }

            if (tpm.getTpmVersion().equals("1.2")) {
                TpmCertifyKey tpmCertifyKey = new TpmCertifyKey(certifiedKey.getKeyData());
                log.debug("TCG Binding Key contents: {} - {}", tpmCertifyKey.getKeyParms().getAlgorithmId(), tpmCertifyKey.getKeyParms().getTrouSerSmode());
            }
            log.info("Successfully created the Binding key TCG certificate and the same has been stored at {}.", bindingKeyTCGCertificate.getAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to create binding key");
            throw e;
        }
    }
}
