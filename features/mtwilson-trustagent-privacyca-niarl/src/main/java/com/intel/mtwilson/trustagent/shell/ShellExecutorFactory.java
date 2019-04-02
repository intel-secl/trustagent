/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.shell;

/**
 *
 * @author dczech
 */
public class ShellExecutorFactory {
    public enum OS {
        Unix,
        Windows
    }
    
    private ShellExecutorFactory() { }
    
    static final ShellExecutor WINDOWSSHELL = new WindowsShellExecutor(), UNIXSHELL = new UnixShellExecutor();    
    
    public static ShellExecutor getInstance(OS operatingSystem) {
        switch(operatingSystem) {
            case Windows:
                return WINDOWSSHELL;
            default:
                return UNIXSHELL;                
        }
    }
}
