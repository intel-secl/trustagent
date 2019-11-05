/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.core.common.PlatformInfoException;
import com.intel.mtwilson.core.common.model.HardwareFeature;
import com.intel.mtwilson.launcher.ws.ext.V2;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.intel.mtwilson.core.common.model.HostInfo;
import com.intel.mtwilson.core.tpm.Tpm;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;


/**
 * Previously called host_info
 * 
 * @author jbuhacoff
 */
@V2
@Path("/host")
public class Host {
    private static HostInfo hostInfo = null;
    
    private final static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Host.class);
    
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @RequiresPermissions("host_info:retrieve")
    public HostInfo getHostInformation() throws PlatformInfoException, IOException, Tpm.TpmException {
        if( hostInfo == null ) {
            HostController hostCntrl = new HostController();
            hostCntrl.execute();
            Tpm tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
            List<String> pcrBanks = new ArrayList();
            for (Enum pcrBank : tpm.getPcrBanks()){
        	pcrBanks.add(pcrBank.toString());
            }
            HostInfo host = new HostInfo();
            host.setHostName(hostCntrl.getContext().getHostName());
            host.setOsName(hostCntrl.getContext().getOsName());
            host.setOsVersion(hostCntrl.getContext().getOsVersion());
            host.setBiosName(hostCntrl.getContext().getBiosName());
            host.setBiosVersion(hostCntrl.getContext().getBiosVersion());
            host.setVmmName(hostCntrl.getContext().getVmmName());
            host.setVmmVersion(hostCntrl.getContext().getVmmVersion());
            host.setProcessorInfo(hostCntrl.getContext().getProcessorInfo());
            host.setProcessorFlags(hostCntrl.getContext().getProcessorFlags());
            host.setHardwareUuid(hostCntrl.getContext().getHardwareUuid());
            host.setTpmVersion(hostCntrl.getContext().getTpmVersion());
            host.setNoOfSockets(hostCntrl.getContext().getNoOfSockets());
            host.setPcrBanks(pcrBanks);
            host.setTpmEnabled(hostCntrl.getContext().getTpmEnabled());
            host.setTxtEnabled(hostCntrl.getContext().getTxtEnabled());
            host.setTbootInstalled(hostCntrl.getContext().getTbootInstalled());
            host.setInstalledComponents(hostCntrl.getContext().getInstalledComponents());
            hostCntrl.getContext().getHardwareFeatures().get(HardwareFeature.TPM).getMeta().put("pcr_banks",StringUtils.join(pcrBanks, '_'));
            host.setHardwareFeatures(hostCntrl.getContext().getHardwareFeatures());
            host.setIsDockerEnv(hostCntrl.getContext().getIsDockerEnv());
            /*
            String responseXML =
                    "<host_info>"
                    + "<timeStamp>" + new Date(System.currentTimeMillis()).toString() + "</timeStamp>"
                    + "<clientIp>" + CommandUtil.getHostIpAddress() + "</clientIp>"
                    + "<errorCode>" + context.getErrorCode().getErrorCode() + "</errorCode>"
                    + "<errorMessage>" + context.getErrorCode().getMessage() + "</errorMessage>"
                    + "<osName>" + context.getOsName() + "</osName>"
                    + "<osVersion> " + context.getOsVersion() + "</osVersion>"
                    + "<biosOem>" + context.getBiosOem() + "</biosOem>"
                    + "<biosVersion> " + context.getBiosVersion()+ "</biosVersion>"
                    + "<vmmName>" + context.getVmmName() + "</vmmName>"
                    + "<vmmVersion>" + context.getVmmVersion() + "</vmmVersion>"
                    + "<processorInfo>" + context.getProcessorInfo() + "</processorInfo>"
                    +"<hostUUID>" + context.getHostUUID() + "</hostUUID>"
                    + "</host_info>";
            return responseXML;
            */
            hostInfo = host;
        }
        return hostInfo;
    }
}
