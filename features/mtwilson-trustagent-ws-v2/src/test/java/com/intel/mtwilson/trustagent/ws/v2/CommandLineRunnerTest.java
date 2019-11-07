/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.intel.mtwilson.core.common.PlatformInfoException;
import com.intel.mtwilson.core.platform.info.CommandLineRunner;
import com.intel.mtwilson.util.exec.Result;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author purvades
 */
public class CommandLineRunnerTest extends CommandLineRunner {
    String osType = null;
    Result result = null;

    /**
     *
     * @param osType
     */
    public CommandLineRunnerTest(String osType){
        this.osType = osType;
    }
    
    private String readSampleInput(String filename) throws IOException {
        if (filename == null) {
            return null;
        }
        try(InputStream in = getClass().getResourceAsStream(String.format("/" + osType + "/%s", filename))) {
            return IOUtils.toString(in, Charset.forName("UTF-8"));
        }
    }
    
    private String getWinFileName(String command){
        switch(command){
            case "wmic os get caption" :
                return "os-name";
            case "wmic os get version" :
                return "os-version";
            case "wmic path WIN32_ServerFeature get ID" :
                return "vmm-name";
                
            //TO DO find actual path
            case "cmd.exe /c" :
                return "vmm-version";
            case "wmic bios get manufacturer" :
                return "bios-name";
            case "wmic bios get smbiosbiosversion" :
                return "bios-version";
            case "wmic cpu get ProcessorId" :
                return "processor-info";
            case "wmic path Win32_ComputerSystemProduct get uuid" :
                return "hardware-uuid";
            case "wmic /namespace:\\\\root\\CIMV2\\Security\\MicrosoftTpm path Win32_Tpm get /value" :
                return "tpm-version";
            case "wmic computersystem get Name" :
                return "host-name";
            case "wmic cpu get SocketDesignation" :
                return "no-of-sockets";
        }
        return null;
    }
    
    @Override
    public Result executeCommand(String baseCmd, String... args) throws PlatformInfoException, IOException {
        if("windows".equals(osType)){
            String command = baseCmd;
            for(String arg:args){
                command = command + " " + arg;
            }
            return (new Result(0, readSampleInput(getWinFileName(command)).getBytes(), new byte[0]));
        }
        else if("linux".equals(osType)){
            return (new Result(0, readSampleInput("platform-info").getBytes(), new byte[0]));
        }
        return null;
    }
}
