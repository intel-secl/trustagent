/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent;

import com.intel.dcsg.cpg.console.HyphenatedCommandFinder;

/**
 *
 * @author jbuhacoff
 */
public class TrustagentCommandFinder extends HyphenatedCommandFinder {

    public TrustagentCommandFinder() {
        super("com.intel.mtwilson.trustagent.cmd");
    }
}
