/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.intel.mtwilson.core.common.PlatformInfoException;
import com.intel.mtwilson.core.common.model.HostInfo;
import com.intel.mtwilson.core.platform.info.CommandLineRunner;
import com.intel.mtwilson.core.platform.info.PlatformInfo;

import java.io.IOException;
/**
 * Controller for Host
 * 
 * @author purvades
 * @author dtiwari 
 */
public class HostController {
    HostInfo context = new HostInfo();
    CommandLineRunner buildandexecute = new CommandLineRunner();
    String osName = System.getProperty("os.name");

    public void setOsName(String osName) {
        this.osName = osName;
    }
    
    public HostInfo getContext(){
        return context;
    }
    
    public void setbuildandexecute(CommandLineRunner buildandexecute){
        this.buildandexecute = buildandexecute;
    }
    
    public void execute() throws PlatformInfoException, IOException {

        if (osName.toLowerCase().contains("windows")) {
            PlatformInfo platformInfo = new PlatformInfo();
            context = platformInfo.getHostInfo();
        } else {
            GetHostInfo cmd = new GetHostInfo(context);
            cmd.setCmd(buildandexecute);
            cmd.execute();
        }
    }
}
