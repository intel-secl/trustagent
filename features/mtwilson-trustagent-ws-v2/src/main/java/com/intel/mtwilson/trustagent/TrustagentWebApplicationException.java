/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.trustagent;

import com.intel.mtwilson.i18n.HostState;
import javax.ws.rs.WebApplicationException;

/**
 *
 * @author rksavino
 */
public class TrustagentWebApplicationException extends WebApplicationException {
    
    public TrustagentWebApplicationException(HostState hostState, Throwable cause) {
        super(hostState.getHostStateText(), cause);
    }
    
    public TrustagentWebApplicationException(HostState hostState, int status) {
        super(hostState.getHostStateText(), status);
    }
    
    public TrustagentWebApplicationException(HostState hostState, Throwable cause, int status) {
        super(hostState.getHostStateText(), cause, status);
    }
}
