/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.core.common.PlatformInfoException;
import com.intel.mtwilson.core.platform.info.PlatformInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Get host information from PlatformInfo library and store it in files while TA installation
 * 
 * @author purvades
 * @author dtiwari
 */
public class PlatformInfoCollector {
    public static void main(String[] args) throws PlatformInfoException, IOException {
        PlatformInfo context = new PlatformInfo();
        ObjectMapper mapper = new ObjectMapper();
        
        Map<String, String> platformInfoMap = new HashMap<>();
        platformInfoMap.put("host-name", context.getHostName());
        platformInfoMap.put("os-name", context.getOsName());
        platformInfoMap.put("os-version", context.getOsVersion());
        platformInfoMap.put("bios-name", context.getBiosName());
        platformInfoMap.put("bios-version", context.getBiosVersion());
        platformInfoMap.put("vmm-name", context.getVmmName());
        platformInfoMap.put("vmm-version", context.getVmmVersion());
        platformInfoMap.put("processor-info", context.getProcessorInfo());
        platformInfoMap.put("processor-flags", context.getProcessorFlags());
        platformInfoMap.put("hardware-uuid", context.getHardwareUuid());
        platformInfoMap.put("tpm-version", context.getTpmVersion());
        platformInfoMap.put("no-of-sockets", context.getNoOfSockets());
        platformInfoMap.put("tpm-enabled", context.getTpmEnabled());
        platformInfoMap.put("txt-status", context.getTxtStatus());
        platformInfoMap.put("tboot-status", context.getTbootStatus());
        platformInfoMap.put("installed-components", String.join(",", context.getInstalledComponents()));
        platformInfoMap.put("cbnt-status", context.getCbntStatus());
        platformInfoMap.put("cbnt-profile", context.getCbntProfile());
        platformInfoMap.put("suefi-status", context.getSuefiStatus());
        platformInfoMap.put("mktme-status", context.getMktmeStatus());
        platformInfoMap.put("mktme-encryption-algorithm", context.getMktmeEncryptionAlgorithm());
        platformInfoMap.put("mktme-max-keys-per-cpu", context.getMktmeMaxKeysPerCpu());

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(platformInfoMap);
        System.out.print(json);
    }
}
