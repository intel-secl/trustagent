/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.util;

import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.impl.InsecureTlsPolicy;
import com.intel.mtwilson.core.common.utils.AASTokenFetcher;
import com.intel.mtwilson.jaxrs2.client.MtWilsonClient;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;

import java.net.URL;
import java.util.Properties;

/**
 * @author arijitgh
 */

public class VSClientCreatorUtil {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VSClientCreatorUtil.class);
    private String username;
    private String password;
    private String url;
    private String aasApiUrl;

    private void setProperties() throws Exception {
        TrustagentConfiguration trustagentConfiguration  = TrustagentConfiguration.loadConfiguration();
        url = trustagentConfiguration.getMtWilsonApiUrl();
        username = trustagentConfiguration.getTrustAgentAdminUserName();
        password = trustagentConfiguration.getTrustAgentAdminPassword();
        aasApiUrl = trustagentConfiguration.getAasApiUrl();

    }

    public MtWilsonClient createVSClient() throws Exception {
        setProperties();
        log.debug("Using AAS API URL to fetch token - {}", aasApiUrl);
        TlsConnection tlsConnection = new TlsConnection(new URL(url), new InsecureTlsPolicy());
        Properties clientConfiguration = new Properties();
        clientConfiguration.setProperty(TrustagentConfiguration.BEARER_TOKEN, new AASTokenFetcher().getAASToken(aasApiUrl, username, password));
        return new MtWilsonClient(clientConfiguration, tlsConnection);
    }
}
