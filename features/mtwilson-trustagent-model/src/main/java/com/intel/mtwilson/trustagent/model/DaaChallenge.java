/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 *
 * @author jbuhacoff
 */
@JacksonXmlRootElement(localName="daaChallenge")
public class DaaChallenge {
    private byte[] challenge;

    public byte[] getChallenge() {
        return challenge;
    }

    public void setChallenge(byte[] challenge) {
        this.challenge = challenge;
    }
    
    
}
