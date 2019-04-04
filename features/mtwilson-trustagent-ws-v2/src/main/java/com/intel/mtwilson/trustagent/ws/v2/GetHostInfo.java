/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.intel.mtwilson.core.common.PlatformInfoException;
import com.intel.mtwilson.core.platform.info.CommandLineRunner;
import com.intel.mtwilson.core.common.model.HostInfo;
import com.intel.mtwilson.core.common.Command;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.intel.mtwilson.core.common.ErrorCode;

import java.util.Map;

/**
 * Get host information from files generated during TA installation
 * 
 * @author purvades
 * @author dtiwari
 */
public class GetHostInfo implements Command {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CommandLineRunner.class);
    
    HostInfo context = new HostInfo();
    
    Map<String, Object> map;

    public GetHostInfo(HostInfo context) {
        this.context = context;
    }
    
    CommandLineRunner cmd = new CommandLineRunner();

    @Override
    public CommandLineRunner getCmd() {
        return cmd;
    }

    @Override
    public void setCmd(CommandLineRunner cmd) {
        this.cmd = cmd;
    }
    String cmd1 = "/opt/trustagent/bin/tagent";
    String cmd2 = "system-info";
    String cmd3 = "platform-info";
    
    @Override
    public void execute() throws PlatformInfoException{
        try {
            String result = getCmd().executeCommand(cmd1, cmd2, cmd3).getStdout();
            ObjectMapper mapper = new ObjectMapper();
            map = mapper.readValue(result, new TypeReference<Map<String, String>>(){});
            getOsName();
            getOsVersion();
            getBiosOem();
            getBiosVersion();
            getVmmName();
            getVmmVersion();
            getProcessorInfo();
            getProcessorFlags();
            getHardwareUuid();
            getTpmVersion();
            getHostName();
            getNoOfSockets();
            getTpmEnabled();
            getTxtEnabled();
        } catch (PlatformInfoException | IOException ex) {
            log.debug("Error while getting OS details", ex);
            throw new PlatformInfoException(ErrorCode.ERROR, "Error while getting OS details.", ex);
        }
    }
    
    private void getOsName() throws PlatformInfoException, IOException{
        context.setOsName((String) map.get("os-name"));
    }
    
    private void getOsVersion() throws PlatformInfoException, IOException{
        context.setOsVersion((String) map.get("os-version"));
    }
    
    private void getBiosOem() throws PlatformInfoException, IOException{
        context.setBiosName((String) map.get("bios-name"));
    }
    
    private void getBiosVersion() throws PlatformInfoException, IOException{
        context.setBiosVersion((String) map.get("bios-version"));
    }
    
    private void getVmmName() throws PlatformInfoException, IOException{
        context.setVmmName((String) map.get("vmm-name"));
    }
    
    private void getVmmVersion() throws PlatformInfoException, IOException{
        context.setVmmVersion((String) map.get("vmm-version"));
    }
    
    private void getProcessorInfo() throws PlatformInfoException, IOException{
        context.setProcessorInfo((String) map.get("processor-info"));
    }
    
    private void getProcessorFlags() throws PlatformInfoException, IOException{
        context.setProcessorFlags((String) map.get("processor-flags"));
    }
    
    private void getHardwareUuid() throws PlatformInfoException, IOException{
        context.setHardwareUuid((String) map.get("hardware-uuid"));
    }
    
    private void getTpmVersion() throws PlatformInfoException, IOException{
        context.setTpmVersion((String) map.get("tpm-version"));
    }
    
    private void getHostName() throws PlatformInfoException, IOException{
        context.setHostName((String) map.get("host-name"));
    }
    
    private void getNoOfSockets() throws PlatformInfoException, IOException{
        context.setNoOfSockets((String) map.get("no-of-sockets"));
    }
    
    private void getTpmEnabled() throws PlatformInfoException, IOException{
        context.setTpmEnabled((String) map.get("tpm-enabled"));
    }
    
    private void getTxtEnabled() throws PlatformInfoException, IOException{
        context.setTxtEnabled((String) map.get("txt-enabled"));
    }

    @Override
    public HostInfo getContext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}