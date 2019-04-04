/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mountwilson.trustagent.commands;

import com.intel.mountwilson.common.ErrorCode;
import com.intel.mountwilson.common.ICommand;
import com.intel.mountwilson.common.TAException;
import com.intel.mountwilson.trustagent.data.TADataContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;

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
        File tcbMeasurementFile = context.getTcbMeasurementXmlFile();
        if (!context.getTcbMeasurementXmlFile().exists()) {
            log.warn("TCB measurement XML file not present at {}.", context.getTcbMeasurementXmlFile().getAbsolutePath());
            // TODO: Need to make this configurable in tboot-xm so that user can make these changes directly and 
            // we do not need to check at multiple locations.
			
			String osName = System.getProperty("os.name");
			if (!osName.toLowerCase().contains("windows")) {
				tcbMeasurementFile = new File("/var/log/trustagent/measurement.xml");
			} else {
				tcbMeasurementFile = new File("C:\\Windows\\Logs\\MeasuredBoot\\measurement.xml");
			}

            log.warn("Checking to see if the log file exists at {}.", tcbMeasurementFile.getAbsolutePath());
            if (!tcbMeasurementFile.exists()) {
                log.warn("TCB measurement XML file does not exist at {}.", tcbMeasurementFile.getAbsolutePath());
                return;
            }  
        } 

        try {
            log.debug("Processing the TCB measurement XML file @ {}.", tcbMeasurementFile.getAbsolutePath());
            try (InputStream in = new FileInputStream(tcbMeasurementFile)) {
                String tcbMeasurementString = IOUtils.toString(in, Charset.forName("UTF-8"));
                log.info("TCB measurement XML string: {}", tcbMeasurementString);
                context.setTcbMeasurement(tcbMeasurementString);
            }
        } catch (IOException e) {
            log.warn("IOException, invalid measurement.xml: {}", e.getMessage());
            throw new TAException(ErrorCode.BAD_REQUEST, "Invalid measurement.xml file. Cannot unmarshal/marshal object using jaxb.");
        } catch (Exception e) {
            log.warn("Exception, invalid measurement.xml: {}", e.getMessage());
            throw new TAException(ErrorCode.BAD_REQUEST, "Invalid measurement.xml file. Cannot unmarshal/marshal object using jaxb.");
        }
    }
}
