/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.as.rest.v2.model;

import com.intel.mtwilson.jaxrs2.DocumentCollection;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author jbuhacoff
 */
@JacksonXmlRootElement(localName="host_collection")
public class HostCollection extends DocumentCollection<Host> {
    private final ArrayList<Host> hosts = new ArrayList<Host>();
    
    // using the xml annotations we get output like <hosts><host>...</host><host>...</host></hosts> , without them we would have <hosts><hosts>...</hosts><hosts>...</hosts></hosts> and it looks strange
    @JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS) // jackson 1.9
    @JsonInclude(JsonInclude.Include.ALWAYS)                // jackson 2.0
    @JacksonXmlElementWrapper(localName="hosts")
    @JacksonXmlProperty(localName="host")    
    public List<Host> getHosts() { return hosts; }

    @Override
    public List<Host> getDocuments() {
        return getHosts();
    }
    
}
