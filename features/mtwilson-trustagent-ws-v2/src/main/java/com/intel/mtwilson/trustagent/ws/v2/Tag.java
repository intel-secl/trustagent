/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.intel.mountwilson.common.ErrorCode;
import com.intel.mountwilson.common.TAConfig;
import com.intel.mountwilson.common.TAException;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.launcher.ws.ext.V2;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import com.intel.mtwilson.trustagent.model.TagWriteRequest;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.codec.binary.Hex;
import com.intel.mtwilson.core.tpm.Tpm;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import java.nio.file.Paths;
/**
 *
 * @author jbuhacoff
 */
@V2
@Path("/tag")
public class Tag {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Tag.class);
    
    @POST
    @Consumes({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
    @RequiresPermissions("deploy_tag:create")
    public void writeTag(TagWriteRequest tagInfo, @Context HttpServletResponse response) throws IOException, TAException {
        try {
            log.debug("writeTag uuid {} sha384 {}", tagInfo.getHardwareUuid(), Hex.encodeHexString(tagInfo.getTag()));
            TrustagentConfiguration config = new TrustagentConfiguration(TAConfig.getConfiguration());
            Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
            tpm.setAssetTag(config.getTpmOwnerSecret(), tagInfo.getTag());
            log.debug("writeTag returning 204 status");
            response.setStatus(Response.Status.NO_CONTENT.getStatusCode());
            log.debug("writeTag done");
        } catch (Tpm.TpmException ex) {
            log.error("Error setting asset tag {}", ex);
            throw new TAException(ErrorCode.ERROR, "Error setting asset tag " + ex);
        }
    }
    
}
