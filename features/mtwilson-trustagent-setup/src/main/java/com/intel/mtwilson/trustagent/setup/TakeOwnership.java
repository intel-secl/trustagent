/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.core.tpm.Tpm;
import com.intel.mtwilson.core.tpm.Tpm.PcrBank;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import gov.niarl.his.privacyca.IdentityOS;
import gov.niarl.his.privacyca.TpmUtils;
import java.nio.file.Paths;
import java.util.Set;

/**
 * 
 * @author jbuhacoff
 */
public class TakeOwnership extends AbstractSetupTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TakeOwnership.class);
    private TrustagentConfiguration config;
    private String tpmOwnerSecret;

    @Override
    protected void configure() throws Exception {
        // tpm owner password must have already been generated
        config = new TrustagentConfiguration(getConfiguration());
        tpmOwnerSecret = config.getTpmOwnerSecretHex();
//        log.debug("TakeOwnership tpmOwnerSecret = {}", tpmOwnerSecret);
        if (tpmOwnerSecret == null || tpmOwnerSecret.isEmpty()) {
            configuration("TPM owner secret must be configured to take ownership");
        }
    }

    @Override
    protected void validate() throws Exception {
        Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
        if (!tpm.isOwnedWithAuth(config.getTpmOwnerSecret())) {
            validation("Trust Agent is not the TPM owner");
        }
    }

    @Override
    protected void execute() throws Exception {
        // Take Ownership
        if (IdentityOS.isWindows()) { 
            /* return for now since Windows usually take the ownership of TPM be default 
             * need to check later for exceptions
            */
            log.info("Windows manages TPM ownership. No need to take ownership");
        } 
        else { /* for Linux TPM 1.2 and TPM 2.0 */         
            try {
                Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
                tpm.takeOwnership(config.getTpmOwnerSecret());
            } catch (Tpm.TpmOwnershipAlreadyTakenException e) {
                log.info("Ownership is already taken");
            } catch(Tpm.TpmException e) {
                throw e;
            }
        }
    }
}