/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mountwilson.common;

/**
 *
 * @author jbuhacoff
 */
public class CommandResult {
    protected String command;
    protected String stdout;
    protected String stderr;
    protected int exitcode;

    public String getCommand() {
        return command;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public int getExitcode() {
        return exitcode;
    }
    
    
}
