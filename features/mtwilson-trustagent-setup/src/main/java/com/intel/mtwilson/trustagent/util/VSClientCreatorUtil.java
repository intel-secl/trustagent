/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.util;

import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.mtwilson.jaxrs2.client.MtWilsonClient;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;

import java.net.URL;
import java.util.Properties;

/**
 * @author arijitgh
 */

public class VSClientCreatorUtil {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VSClientCreatorUtil.class);
    private TrustagentConfiguration trustagentConfiguration;
    private String username;
    private String password;
    private String url;
    private String currentIp;

    private void setProperties() throws Exception {
        trustagentConfiguration  = TrustagentConfiguration.loadConfiguration();
        url = trustagentConfiguration.getMtWilsonApiUrl();
        username = trustagentConfiguration.getMtWilsonApiUsername();
        password = trustagentConfiguration.getMtWilsonApiPassword();
        currentIp = trustagentConfiguration.getCurrentIp();

    }

    public MtWilsonClient createVSClient() throws Exception {
        setProperties();
        String distro = "intel";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            distro = "microsoft";
        }
        String connectionString = String.format("%s:https://%s:%s", distro, currentIp, trustagentConfiguration.getTrustagentHttpTlsPort());
        log.debug("connection string is {}", connectionString);
        TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustagentConfiguration.getTrustagentKeystoreFile(), trustagentConfiguration.getTrustagentKeystorePassword()).build();
        TlsConnection tlsConnection = new TlsConnection(new URL(url), tlsPolicy);
        Properties clientConfiguration = new Properties();
        clientConfiguration.setProperty(TrustagentConfiguration.MTWILSON_API_USERNAME, username);
        clientConfiguration.setProperty(TrustagentConfiguration.MTWILSON_API_PASSWORD, password);
        return new MtWilsonClient(clientConfiguration, tlsConnection);
    }
}
