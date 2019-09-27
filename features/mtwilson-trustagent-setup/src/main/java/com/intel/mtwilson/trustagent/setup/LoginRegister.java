/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.impl.InsecureTlsPolicy;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.core.common.utils.AASTokenFetcher;
import com.intel.mtwilson.trustagent.attestation.client.jaxrs.Hosts;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author ssbangal
 */
public class LoginRegister extends AbstractSetupTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoginRegister.class);
    private static final String ipv6RegEx = "^([0-9A-Fa-f]{1,4}:){7}(.)*$";
    private String trustagentLoginUserName;
    private String trustagentLoginPassword;
    private String url;
    private String aasApiUrl;
    private File passwordFile;
    private boolean isRegistered;
    private String dn;
    private String[] ip;
    private String[] dns;

    @Override
    protected void configure() throws Exception {
        TrustagentConfiguration trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        isRegistered = false;

        trustagentLoginUserName = trustagentConfiguration.getTrustAgentAdminUserName();
        if (trustagentLoginUserName == null || trustagentLoginUserName.isEmpty()) {
            configuration("TrustAgent User name is not set. Please run the create-admin-user setup task first.");
        }

        File privateDir = new File(Folders.configuration() + File.separator + "private");
        passwordFile = privateDir.toPath().resolve("securestore.p12").toFile();

        if(!privateDir.exists()) {
           privateDir.mkdirs();
        }

        if(!passwordFile.exists()) {
           configuration("TrustAgent password is not set. Please run the create-admin-user setup task first.");
           return;
        }

        trustagentLoginPassword = trustagentConfiguration.getTrustAgentAdminPassword();
        if (trustagentLoginPassword == null || trustagentLoginPassword.isEmpty()) {
            configuration("TrustAgent password is not set. Please run the create-admin-user setup task first.");
        }

        url = trustagentConfiguration.getMtWilsonApiUrl();
        if (url == null || url.isEmpty()) {
            configuration("Mt Wilson URL is not set");
        }

        aasApiUrl = trustagentConfiguration.getAasApiUrl();
        if (aasApiUrl == null || aasApiUrl.isEmpty()) {
            configuration("AAS API URL is not set");
        }

        dn = trustagentConfiguration.getTrustagentTlsCertDn();
        ip = trustagentConfiguration.getTrustagentTlsCertIpArray();
        dns = trustagentConfiguration.getTrustagentTlsCertDnsArray();
        if (dn == null || dn.isEmpty()) {
            configuration("DN not configured");
        }

        if ((ip == null ? 0 : ip.length) + (dns == null ? 0 : dns.length) == 0) {
            configuration("At least one IP or DNS alternative name must be configured");
        }
    }

    @Override
    protected void validate() throws Exception {

        // No need to validate anything. There will not be any issue registering the same login id and password with MTW multiple times.
        if (!isRegistered) {
            validation("User has not been registered with attestation service: %s", trustagentLoginUserName);
        }
    }

    @Override
    protected void execute() throws Exception {
        log.info("Starting the process to pre-register the host login and password with attestation service.");

        if (System.getenv("TRUSTAGENT_LOGIN_REGISTER") != null && System.getenv("TRUSTAGENT_LOGIN_REGISTER").equalsIgnoreCase("false")) {
            log.info("The TA username and password are not registered with attestation service since user has not opted for pre-registration.");
            isRegistered = true;            
            return;
        }

        if (trustagentLoginUserName == null || trustagentLoginUserName.isEmpty() || trustagentLoginPassword == null || trustagentLoginPassword.isEmpty()) {
            log.error("The TA username or password is not set. Please run the create-admin-user first.");
            return;
        }
        
        TlsConnection tlsConnection = new TlsConnection(new URL(url), new InsecureTlsPolicy());
        Properties clientConfiguration = new Properties();
        clientConfiguration.setProperty(TrustagentConfiguration.BEARER_TOKEN, new AASTokenFetcher().getAASToken(aasApiUrl, trustagentLoginUserName, trustagentLoginPassword));

        Hosts hostClientObj = new Hosts(clientConfiguration, tlsConnection);
        List<String> hostNames = new ArrayList<>();
        hostNames.addAll(Arrays.asList(dns));
        hostNames.addAll(Arrays.asList(ip));
        // Remove the localhost and local ip from the list
        for (Iterator<String> iterator = hostNames.listIterator(); iterator.hasNext();) {
            String hostName = iterator.next();
            if (hostName.equalsIgnoreCase("localhost") || hostName.equalsIgnoreCase("127.0.0.1") || hostName.matches(ipv6RegEx)) {
                iterator.remove();
            }
        }
        hostClientObj.preRegisterHostDetails(hostNames, trustagentLoginUserName, trustagentLoginPassword);

        log.info("Successfully registered the host access information with attestation service.");

        FileUtils.deleteQuietly(passwordFile);

        isRegistered = true;
    }
}
