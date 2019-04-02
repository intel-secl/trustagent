/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.measurement;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author rksavino
 */
@XmlRootElement(name = "dir")
public class DirectoryMeasurementEntry extends MeasurementEntry {
    private String path;
    private String exclude;
    private String include;
    
    @XmlAttribute
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    @XmlAttribute
    public String getExclude() {
        return exclude;
    }
    
    public void setExclude(String exclude) {
        this.exclude = exclude;
    }
    
    @XmlAttribute
    public String getInclude() {
        return include;
    }
    
    public void setInclude(String include) {
        this.include = include;
    }
}
