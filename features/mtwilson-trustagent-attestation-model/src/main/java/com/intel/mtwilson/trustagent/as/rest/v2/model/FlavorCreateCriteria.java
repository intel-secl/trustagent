/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.as.rest.v2.model;

import java.util.List;

/**
 *
 * @author hmgowda
 */
public class FlavorCreateCriteria {

    private String connectionString;
    private String tlsPolicyId;
    private String flavorgroupName;
    private List<String> partialFlavorTypes;

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
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

    public List<String> getPartialFlavorTypes() {
        return partialFlavorTypes;
    }

    public void setPartialFlavorTypes(List<String> partialFlavorTypes) {
        this.partialFlavorTypes = partialFlavorTypes;
    }

}
