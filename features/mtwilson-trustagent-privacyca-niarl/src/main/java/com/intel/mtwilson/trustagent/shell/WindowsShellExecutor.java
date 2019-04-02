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
class WindowsShellExecutor extends GenericShellExecutor {

    @Override
    void prepareCommandOverride(List<String> cmd) {        
        // add each to front, which means it will be cmd.exe /c TPMTool.exe
        cmd.add(0, Folders.application() + File.separator + "bin" + File.separator + "TPMTool.exe");
        cmd.add(0, "/c");
        cmd.add(0, "cmd.exe");
    }
    
}
