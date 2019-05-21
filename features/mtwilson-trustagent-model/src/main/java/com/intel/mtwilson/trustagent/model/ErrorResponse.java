/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.intel.dcsg.cpg.net.InternetAddress;
import com.intel.mtwilson.i18n.ErrorCode;
import java.util.Date;

/**
 *
 * @author jbuhacoff
 */
@JacksonXmlRootElement(localName="error")
public class ErrorResponse {
    public Date timestamp;
    public InternetAddress clientIp;
    public ErrorCode errorCode;
    public String errorMessage;
}
