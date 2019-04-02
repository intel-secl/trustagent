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
import com.intel.mtwilson.jaxrs2.client.MtWilsonClient;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import com.intel.mtwilson.trustagent.as.rest.v2.model.FlavorCreateCriteria;
import com.intel.mtwilson.core.flavor.model.Flavor;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import com.intel.mtwilson.crypto.password.GuardedPassword;

/**
 *
 * @author hmgowda
 */
public class CreateHostUniqueFlavor extends AbstractSetupTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateHostUniqueFlavor.class);

    private String username;
    private GuardedPassword guardedPassword = new GuardedPassword();
    private String currentIp;
    private String url;
    private TrustagentConfiguration trustagentConfiguration;
    private File keystoreFile;
    private GuardedPassword keystoreGuardedPassword = new GuardedPassword();
    private SimpleKeystore keystore;

    @Override
    protected void configure() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        url = trustagentConfiguration.getMtWilsonApiUrl();
        if (url == null || url.isEmpty()) {
            configuration("Mt Wilson URL is not set");
        }
        username = trustagentConfiguration.getMtWilsonApiUsername();
        guardedPassword.setPassword(trustagentConfiguration.getMtWilsonApiPassword());
        currentIp = trustagentConfiguration.getCurrentIp();
        if (username == null || username.isEmpty()) {
            configuration("Mt Wilson username is not set");
        }
        if (!guardedPassword.isPasswordValid()) {
            configuration("Mt Wilson password is not set");
        }
        if (currentIp == null || currentIp.isEmpty()) {
            configuration("Current IP is not set");
        }
        keystoreFile = trustagentConfiguration.getTrustagentKeystoreFile();
        if (keystoreFile == null || !keystoreFile.exists()) {
            configuration("Trust Agent keystore does not exist");
        }
        keystoreGuardedPassword.setPassword(trustagentConfiguration.getTrustagentKeystorePassword());
        if (!keystoreGuardedPassword.isPasswordValid()) {
            configuration("Trust Agent keystore password is not set");
        }
        keystore = new SimpleKeystore(new FileResource(keystoreFile), keystoreGuardedPassword.getInsPassword());
        keystoreGuardedPassword.dispose();
    }

    @Override
    protected void validate() throws Exception {
    }

    @Override
    protected void execute() throws Exception {
        String distro = "intel";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            distro = "microsoft";
        }
        
        log.info("Creating a host_unique flavor for the host {}", currentIp);
        String connectionString = String.format("%s:https://%s:%s", distro, currentIp, trustagentConfiguration.getTrustagentHttpTlsPort());
        TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustagentConfiguration.getTrustagentKeystoreFile(), trustagentConfiguration.getTrustagentKeystorePassword()).build();
        TlsConnection tlsConnection = new TlsConnection(new URL(trustagentConfiguration.getMtWilsonApiUrl()), tlsPolicy);

        Properties clientConfiguration = new Properties();
        clientConfiguration.setProperty(TrustagentConfiguration.MTWILSON_API_USERNAME, username);
        clientConfiguration.setProperty(TrustagentConfiguration.MTWILSON_API_PASSWORD, guardedPassword.getInsPassword());
        guardedPassword.dispose();

        List<String> partialFlavorTypes = new ArrayList<>();
        partialFlavorTypes.add("HOST_UNIQUE");
        FlavorCreateCriteria flavorCreateCriteria = new FlavorCreateCriteria();
        flavorCreateCriteria.setConnectionString(connectionString);
        flavorCreateCriteria.setPartialFlavorTypes(partialFlavorTypes);

        MtWilsonClient client = new MtWilsonClient(clientConfiguration, tlsConnection);
        client.getTarget().path("flavors").request().accept(MediaType.APPLICATION_JSON).post(Entity.json(flavorCreateCriteria), Flavor.class);

    }

}
