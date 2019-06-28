/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.intel.mtwilson.Folders;
import com.intel.mountwilson.common.ErrorCode;
import com.intel.mountwilson.common.TAException;

import com.intel.mtwilson.core.common.utils.ManifestUtils;
import com.intel.mtwilson.launcher.ws.ext.V2;
import com.intel.mtwilson.core.common.utils.MeasurementUtils;
import com.intel.wml.manifest.xml.Manifest;
import com.intel.wml.measurement.xml.Measurement;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Map;

@V2
@Path("/host/application-measurement")
public class Measure {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Measure.class);
    private final static String measureBinaryPath = "/opt/tbootxm/bin/measure";
    private final static String WmlMeasureLog = Folders.log() + File.separator + "trustagent.log";

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Measurement measure(Manifest manifest, @Context HttpServletResponse response) throws IOException, JAXBException, TAException, InterruptedException {
        Measurement measurement;
        String manifestString = ManifestUtils.getManifestString(manifest);
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            manifestString = manifestString.replaceAll("\'", "\"");
        } else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            manifestString = manifestString.replaceAll("\"", "\'");
        }
        log.debug("measure(): Manifest is {}", manifest);
        try {
            measurement = getMeasurement(manifestString);
            response.setStatus(Response.Status.OK.getStatusCode());
        } catch (IOException exception) {
            throw new IOException("Cannot get the measurement from measure binary: " + exception);
        }
        return measurement;
    }

    private Measurement getMeasurement(String manifest) throws IOException, InterruptedException, TAException {
        String line;
        Measurement measurement;
        BufferedReader stdInput;
        ProcessBuilder pb = new ProcessBuilder(measureBinaryPath, manifest, "/");
        //Set the WML_LOG_FILE variable value to TA log file path
        Map<String, String> envVariable = pb.environment();
        envVariable.put("WML_LOG_FILE", WmlMeasureLog);
        Process process = pb.start();
        //Read the measure binary output from console
        StringBuilder readMeasurement = new StringBuilder();
        InputStreamReader getInputStream = new InputStreamReader(process.getInputStream());
        try {
            stdInput = new BufferedReader(getInputStream);
            while ((line = stdInput.readLine()) != null) {
                readMeasurement.append(line);
            }
            int errCode = process.waitFor();
            if (errCode != 0) {
                log.error("getMeasurement(): Measure binary execution failed with error code {}", String.valueOf(errCode));
                throw new TAException(ErrorCode.ERROR, "Error getting measurement from manifest");
            }
        } finally {
            getInputStream.close();
        }
        try {
            measurement = MeasurementUtils.parseMeasurementXML(readMeasurement.toString());
        } catch (JAXBException | XMLStreamException exception) {
            throw new WebApplicationException("Measurement generated is invalid ", exception);
        }
        log.debug("measure(): Measure command output: {}", measurement);
        return measurement;
    }
}
