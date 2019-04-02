/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.as.rest.v2.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.jaxrs2.Document;

/**
 *
 * @author hmgowda
 */
@JacksonXmlRootElement(localName="host_create_criteria")
public class HostCreateCriteria extends Document{
    private String connectionString;
    private String hostName;
    private UUID tlsPolicyId;

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public UUID getTlsPolicyId() {
        return tlsPolicyId;
    }

    public void setTlsPolicyId(UUID tlsPolicyId) {
        this.tlsPolicyId = tlsPolicyId;
    }
    
}
