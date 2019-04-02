/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mountwilson.trustagent.commands;

import com.intel.mountwilson.common.ErrorCode;
import com.intel.mountwilson.common.ICommand;
import com.intel.mountwilson.common.TAException;
import com.intel.mountwilson.trustagent.data.TADataContext;

import java.io.IOException;
import java.nio.file.Paths;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.core.tpm.Tpm;

/**
 *
 * @author rksavino
 */
public class RetrieveTcbMeasurement implements ICommand {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RetrieveTcbMeasurement.class);
    private TADataContext context;

    public RetrieveTcbMeasurement(TADataContext context) {
        this.context = context;
    }

    /**
     * Retrieves the measurement log from the TA node.
     *
     * @throws TAException
     */
    @Override
    public void execute() throws TAException {
        try {
            Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
            context.setTcbMeasurement(tpm.getTcbMeasurements());
        } catch (IOException | Tpm.TpmException e) {
            log.warn("IOException, invalid measurement.xml: {}", e.getMessage());
            throw new TAException(ErrorCode.BAD_REQUEST, "Invalid measurement.xml file. Cannot unmarshal/marshal object using jaxb.");
        }
    }
}
