/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.as.rest.v2.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.jaxrs2.Document;

/**
 * Example serialization:
 * 
<host>
<id>34b1f684-7f71-48d3-a0a0-41768f9ed130</id>
<name>hostxyz</name>
<connection_url>http://1.2.3.4</connection_url>
<description>test host</description>
<bios_mle>bios-4.3.2</bios_mle>
</host>
 *
 * The JacksonXmlRootElement(localName="host") annotation is responsible
 * for the lowercase "host" tag, otherwise the default would be "Host"
 * 
 * @author jbuhacoff
 */
@JacksonXmlRootElement(localName="host")
public class Host extends Document {
    private String hostName;
    private String description;
    private String connectionString;
    private UUID hardwareUuid;
    private String tlsPolicyId;
    private String flavorgroupName;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public UUID getHardwareUuid() {
        return hardwareUuid;
    }

    public void setHardwareUuid(UUID hardwareUuid) {
        this.hardwareUuid = hardwareUuid;
    }

    public String getTlsPolicyId() {
        return tlsPolicyId;
    }

    public void setTlsPolicyId(String tlsPolicyId) {
        this.tlsPolicyId = tlsPolicyId;
    }

    public String getFlavorgroupName() {
        return flavorgroupName;
    }

    public void setFlavorgroupName(String flavorgroupName) {
        this.flavorgroupName = flavorgroupName;
    }

    
}
