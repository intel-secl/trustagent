/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import com.intel.dcsg.cpg.io.PropertiesUtil;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.mtwilson.trustagent.attestation.client.jaxrs.HostTpmPassword;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.trustagent.as.rest.v2.model.TpmPassword;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Properties;

/**
 *
 * @author jbuhacoff
 */
public class RegisterTpmPassword extends AbstractSetupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegisterTpmPassword.class);

    private String tpmOwnerSecretHex;
    private UUID hostHardwareId;
    private File etagCacheFile;
    private Properties etagCache;
    private TlsConnection tlsConnection;
    private Properties clientConfiguration = new Properties();
    TrustagentConfiguration trustagentConfiguration;

    @Override
    protected void configure() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        
        String url = trustagentConfiguration.getMtWilsonApiUrl();
        if (url == null || url.isEmpty()) {
            configuration("Mt Wilson URL [mtwilson.api.url] must be set");
        }

        String bearerToken = System.getenv(TrustagentConfiguration.BEARER_TOKEN_ENV);
        if (bearerToken == null || bearerToken.isEmpty()) {
            configuration("BEARER_TOKEN not set in the environment");
        }

        tpmOwnerSecretHex = trustagentConfiguration.getTpmOwnerSecretHex();
        if( tpmOwnerSecretHex == null || tpmOwnerSecretHex.isEmpty()) {
            configuration("TPM Owner Secret [tpm.owner.secret] must be set");
        }

        etagCacheFile = trustagentConfiguration.getTrustagentEtagCacheFile();
        if( etagCacheFile.exists() ) {
            etagCache = PropertiesUtil.loadExisting(etagCacheFile);
        }
        else {
            etagCache = new Properties();
        }
        String hostHardwareIdHex = trustagentConfiguration.getHardwareUuid();
        if( hostHardwareIdHex == null || hostHardwareIdHex.isEmpty() || !UUID.isValid(hostHardwareIdHex) ) {
            configuration("Host hardware UUID [hardware.uuid] must be set");
        }
        else {
            hostHardwareId = UUID.valueOf(hostHardwareIdHex);
        }
        TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustagentConfiguration.getTrustagentTruststoreFile(),
            trustagentConfiguration.getTrustagentTruststorePassword()).build();
        tlsConnection = new TlsConnection(new URL(url), tlsPolicy);

        clientConfiguration.setProperty(TrustagentConfiguration.BEARER_TOKEN, bearerToken);
    }

    @Override
    protected void validate() throws Exception {
        //        by saving the ETag we get from Mt Wilson and then
        //        looking for the same ETag from here.
        //        until that is done, user should always run this setup task
        //        with --force

        // check if mt wilson already knows the tpm owner secret
        HostTpmPassword client = new HostTpmPassword(clientConfiguration, tlsConnection);
        TpmPassword tpmPassword;
        try {
            tpmPassword = client.retrieveTpmPassword(hostHardwareId);
            if(tpmPassword == null){
                validation("TPM Owner Secret is not registered with Mt Wilson");
                return;
            }
        } catch (Exception e) {
            validation(e, "Cannot determine if TPM Owner Secret is registered with Mt Wilson");
            return;
        }
        // mt wilson has a value for this, check if it's the same as ours
        if (etagCache.containsKey(TrustagentConfiguration.TPM_OWNER_SECRET)) {
            String previousEtag = etagCache.getProperty(TrustagentConfiguration.TPM_OWNER_SECRET);
            log.debug("The previous tag is {}", previousEtag);
            String currentEtag = tpmPassword.getEtag();
            if (currentEtag != null && !currentEtag.equalsIgnoreCase(previousEtag)) {
                validation("TPM Owner Secret was updated and should be re-registered in Mt Wilson");
            }
        }
    }

    @Override
    protected void execute() throws Exception {

        HostTpmPassword client = new HostTpmPassword(clientConfiguration, tlsConnection);
        String etag = client.storeTpmPassword(hostHardwareId, tpmOwnerSecretHex);
        if( etag != null && !etag.isEmpty() ) {
            etagCache.setProperty(TrustagentConfiguration.TPM_OWNER_SECRET, etag);
            try(FileOutputStream out = new FileOutputStream(etagCacheFile)) {
                etagCache.store(out, "automatically generated by setup");
            }
        }
    }
    
}
