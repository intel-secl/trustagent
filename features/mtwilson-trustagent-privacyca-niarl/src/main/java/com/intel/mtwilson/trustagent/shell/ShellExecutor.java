/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.shell;

import java.io.IOException;

/**
 *
 * @author dczech
 */
public interface ShellExecutor {
    CommandLineResult executeTpmCommand(String command, String[] args, int returnCount) throws IOException;
    
}
