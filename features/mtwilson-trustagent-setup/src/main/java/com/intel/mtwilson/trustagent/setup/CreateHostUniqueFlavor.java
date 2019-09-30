/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.trustagent.setup;

import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.mtwilson.core.common.utils.AASTokenFetcher;
import com.intel.mtwilson.jaxrs2.client.MtWilsonClient;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import com.intel.mtwilson.trustagent.as.rest.v2.model.FlavorCreateCriteria;
import com.intel.mtwilson.core.flavor.model.Flavor;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author hmgowda
 */
public class CreateHostUniqueFlavor extends AbstractSetupTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateHostUniqueFlavor.class);

    private String currentIp;
    private String url;
    private String username;
    private String password;
    private String aasApiUrl;
    private TrustagentConfiguration trustagentConfiguration;

    @Override
    protected void configure() throws Exception {
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        url = trustagentConfiguration.getMtWilsonApiUrl();
        if (url == null || url.isEmpty()) {
            configuration("Mt Wilson URL is not set");
        }
        username = trustagentConfiguration.getTrustAgentAdminUserName();
        if (username == null || username.isEmpty()) {
            configuration("TA admin username is not set");
        }
        password = trustagentConfiguration.getTrustAgentAdminPassword();
        if (password == null || password.isEmpty()) {
            configuration("TA admin password is not set");
        }
        aasApiUrl = trustagentConfiguration.getAasApiUrl();
        if (aasApiUrl == null || aasApiUrl.isEmpty()) {
            configuration("AAS API URL is not set");
        }
        currentIp = trustagentConfiguration.getCurrentIp();
        if (currentIp == null || currentIp.isEmpty()) {
            configuration("Current IP is not set");
        }
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
        TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustagentConfiguration.getTrustagentTruststoreFile(),
            trustagentConfiguration.getTrustagentTruststorePassword()).build();
        TlsConnection tlsConnection = new TlsConnection(new URL(url), tlsPolicy);
        Properties clientConfiguration = new Properties();

        clientConfiguration.setProperty(TrustagentConfiguration.BEARER_TOKEN, new AASTokenFetcher().getAASToken(username, password, new TlsConnection(new URL(aasApiUrl), tlsPolicy)));

        List<String> partialFlavorTypes = new ArrayList<>();
        partialFlavorTypes.add("HOST_UNIQUE");
        FlavorCreateCriteria flavorCreateCriteria = new FlavorCreateCriteria();
        flavorCreateCriteria.setConnectionString(connectionString);
        flavorCreateCriteria.setPartialFlavorTypes(partialFlavorTypes);

        MtWilsonClient client = new MtWilsonClient(clientConfiguration, tlsConnection);
        client.getTarget().path("flavors").request().accept(MediaType.APPLICATION_JSON).post(Entity.json(flavorCreateCriteria), Flavor.class);

    }

}
