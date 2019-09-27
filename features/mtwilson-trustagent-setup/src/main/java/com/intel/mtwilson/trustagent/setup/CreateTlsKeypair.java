/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import com.intel.kms.setup.JettyTlsKeystore;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;

/**
 *
 * @author rawatar
 */
public class CreateTlsKeypair extends JettyTlsKeystore {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateTlsKeypair.class);

    @Override
    protected void configure() throws Exception {
        TrustagentConfiguration config = new TrustagentConfiguration(getConfiguration());

        username = config.getTrustAgentAdminUserName();
        if (username == null || username.isEmpty()) {
            configuration("TA admin username is not set");
        }

        password = config.getTrustAgentAdminPassword();
        if (password == null || password.isEmpty()) {
            configuration("TA admin password is not set");
        }

        super.configure();
    }

    @Override
    protected void validate() throws Exception {
        super.validate();
    }

    @Override
    protected void execute() throws Exception {
        super.execute();
    }
}
