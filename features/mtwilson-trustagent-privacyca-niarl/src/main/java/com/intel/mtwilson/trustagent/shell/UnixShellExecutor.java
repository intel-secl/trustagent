/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.shell;

import com.intel.mtwilson.Folders;
import java.io.File;
import java.util.List;

/**
 *
 * @author dczech
 */
class UnixShellExecutor extends GenericShellExecutor {

    @Override
    void prepareCommandOverride(List<String> cmd) {     
        if(cmd.size() > 0) {
            String bin = cmd.get(0);
            bin = Folders.application() + File.separator + "bin" + File.separator+ bin;
            cmd.set(0, bin);    
        }
    }
    
}