/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mountwilson.trustagent.commands;

import com.intel.mountwilson.common.ErrorCode;
import com.intel.mountwilson.common.ICommand;
import com.intel.mountwilson.common.TAException;
import com.intel.mountwilson.trustagent.data.TADataContext;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.core.tpm.Tpm;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Paths;

/**
 *
 * @author skaja
 */
public class GenerateModulesCmd implements ICommand {

    Logger log = LoggerFactory.getLogger(getClass().getName());
    private final TADataContext context;

    public GenerateModulesCmd(TADataContext context) {
        this.context = context;
    }

    @Override
    public void execute() throws TAException {
        try {
            Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
            context.setModules(tpm.getModuleLog());
        } catch (Tpm.TpmException | IOException ex) {
            throw new TAException(ErrorCode.ERROR, "Error while getting Module details.", ex);
        }

    }
}
