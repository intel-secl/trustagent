/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.jaxrs2.client.MtWilsonClient;
import com.intel.mtwilson.launcher.ws.ext.V2;
import com.intel.mtwilson.Folders;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import com.intel.mtwilson.core.common.utils.ManifestUtils;
import com.intel.mtwilson.trustagent.util.VSClientCreatorUtil;
import com.intel.wml.manifest.xml.Manifest;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import com.intel.mtwilson.core.common.model.SoftwareFlavorPrefix;

/**
 * @author arijitgh
 */
@V2
@Path("/deploy/manifest")
public class ManifestDeployer {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ManifestDeployer.class);
    private final static String manifestFileBasePath = Folders.application() + File.separator + "var" + File.separator;
    private final static String MANIFEST_RESOURCE = "manifests";

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public Response getPushedManifest(Manifest pushedManifest, @Context HttpServletResponse response) throws JAXBException {
        validate(pushedManifest);
        String manifestString = ManifestUtils.getManifestString(pushedManifest);
        manifestString = manifestString.trim();
        log.debug("getPushedManifest(): Manifest received is {}", pushedManifest);
        String manifestFilePath = manifestFileBasePath + "manifest_" + pushedManifest.getUuid() + ".xml";
        //Check if the manifest file already exists
        int status = checkManifestFileExists(new File(manifestFilePath));
        try {
            writeToFile(new File(manifestFilePath), manifestString);
        } catch (IOException exception) {
            log.error("getPushedManifest(): Cannot write to file : {}", exception);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return Response.status(status).build();
    }

    private void validate(Manifest manifest) {
        validateManifestUuid(manifest);
        validateManifestLabel(manifest);
        validateFlavorExists(manifest);
        validateDefaultManifest(manifest);
    }

    private void validateManifestUuid(Manifest manifest){
        if (manifest.getUuid() == null || manifest.getUuid().isEmpty()) {
            log.error("UUID is not provided in manifest");
            throw new WebApplicationException("UUID is not provided in manifest", 400);
        }
        else if (!UUID.isValid(manifest.getUuid())) {
            log.error("UUID provided in manifest is not valid");
            throw new WebApplicationException("UUID provided in manifest is not valid", 400);
        }
    }

    private void validateManifestLabel(Manifest manifest){
        if (manifest.getLabel() == null || manifest.getLabel().isEmpty()) {
            log.error("Label is not provided in manifest");
            throw new WebApplicationException("Label is not provided in manifest", 400);
        }
    }

    private void validateDefaultManifest(Manifest manifest){
        if (manifest.getLabel().contains(SoftwareFlavorPrefix.DEFAULT_APPLICATION_FLAVOR_PREFIX.getValue())
                || manifest.getLabel().contains(SoftwareFlavorPrefix.DEFAULT_WORKLOAD_FLAVOR_PREFIX.getValue())){
            log.error("Default flavor's manifest is part of installation, no need to deploy default flavor's manifest");
            throw new WebApplicationException("Default flavor's manifest is part of installation, no need to deploy default flavor's manifest", 400);
        }
    }

    private void validateFlavorExists(Manifest manifest) {
        Manifest pulledManifest;
        MtWilsonClient client;
        try {
            client = new VSClientCreatorUtil().createVSClient();
        } catch (Exception exc){
            log.error("Error creating client");
            throw new WebApplicationException("Cannot connect to VS", 500);
        }
        try {
            pulledManifest = client.getTarget().path(MANIFEST_RESOURCE).queryParam("id", UUID.valueOf(manifest.getUuid())).request().accept(MediaType.APPLICATION_XML).get(Manifest.class);
            if (!manifest.getLabel().equals(pulledManifest.getLabel())) {
                log.error("Software flavor label does not match the label of the manifest to be deployed");
                throw new WebApplicationException();
            }
        } catch (WebApplicationException webex) {
            throw new WebApplicationException("No matching software flavor found for the manifest to be deployed", 400);
        }
    }

    //Check if manifest file already exists
    private int checkManifestFileExists(File manifestPath) {
        boolean validateFilePath = manifestPath.exists();
        if (validateFilePath) {
            return Response.Status.OK.getStatusCode();
        } else {
            return Response.Status.CREATED.getStatusCode();
        }
    }



    private void writeToFile(File filePath, String data) throws IOException {
        FileUtils.write(filePath, data, "utf-8");
    }

}
