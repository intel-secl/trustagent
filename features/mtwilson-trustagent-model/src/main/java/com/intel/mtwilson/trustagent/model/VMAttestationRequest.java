/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.trustagent.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 *
 * @author hxia5
 */
@JacksonXmlRootElement(localName="vm_attestation_request")
public class VMAttestationRequest {

    private String vm_instance_id;
    private String nonce;
    
    public VMAttestationRequest() {
        this.vm_instance_id = null;
    }
    
    public VMAttestationRequest(String vm_instance_id) {
        this.vm_instance_id = vm_instance_id;
    }

    public void setVmInstanceId(String vm_instance_id) {
        this.vm_instance_id = vm_instance_id;
    }

    public String getVmInstanceId() {
        return vm_instance_id;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    
}