/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.client.jaxrs;

import com.intel.dcsg.cpg.extensions.Extensions;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateDigestTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.creator.impl.InsecureTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.creator.impl.InsecureTrustFirstPublicKeyTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.creator.impl.PublicKeyDigestTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyCreator;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hmgowda
 */
public class TrustagentClientTest {
    
    public TrustagentClientTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        Extensions.register(TlsPolicyCreator.class, InsecureTlsPolicyCreator.class); // required for testInsecureV1 and testInsecureV2
        Extensions.register(TlsPolicyCreator.class, PublicKeyDigestTlsPolicyCreator.class); // required for testPublicKeyDigestTlsPolicyV2
        Extensions.register(TlsPolicyCreator.class, InsecureTrustFirstPublicKeyTlsPolicyCreator.class); // required for testTrustFirstPublicKeyTlsPolicyV2
        Extensions.register(TlsPolicyCreator.class, CertificateDigestTlsPolicyCreator.class);
        Extensions.register(TlsPolicyCreator.class, CertificateTlsPolicyCreator.class);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    
    @Test
    public void clientTest() throws Exception{
       
       Properties properties = new Properties();
       properties.put("mtwilson.api.baseurl", "https://192.168.0.2:1443/v2");
       properties.put("mtwilson.api.username", "tagentadmin");
       properties.put("mtwilson.api.password", "TAgentAdminPassword");
       properties.put("mtwilson.api.tls.policy.certificate.sha256", "003da58915bde878516b315f8fde9277d20e4df71b68602fbcfe0ebfda0e7afe");

       TrustAgentClient client = new TrustAgentClient(properties);
       client.getAik();
       
    }
}
