/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.intel.mtwilson.core.common.PlatformInfoException;
import com.intel.mtwilson.core.common.model.*;
import com.intel.mtwilson.core.platform.info.CommandLineRunner;
import com.intel.mtwilson.core.common.model.HostInfo;
import com.intel.mtwilson.core.common.Command;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.intel.mtwilson.core.common.ErrorCode;

import java.util.HashMap;
import java.util.Map;

import static com.intel.mtwilson.core.common.model.HardwareFeature.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Get host information from files generated during TA installation
 *
 * @author purvades
 * @author dtiwari
 */
public class  GetHostInfo implements Command {

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
            getTbootInstalled();
            getHardwareFeatures();
            HardwareFeatureDetails suefi = context.getHardwareFeatures().get(HardwareFeature.SUEFI);
            String tbootStatus = context.getTbootInstalled(); // will be null in case of windows
            String txtStatus = context.getTxtEnabled();
            if((isTbootNotInstalled(tbootStatus) || !Boolean.valueOf(txtStatus)) && suefi == null) {
                throw new PlatformInfoException(ErrorCode.ERROR, "TXT should be enabled and tboot should be installed on this system");
            }
            if((isTbootNotInstalled(tbootStatus) || !Boolean.valueOf(txtStatus)) && !suefi.getEnabled()) {
                throw new PlatformInfoException(ErrorCode.ERROR, "SUEFI should be enabled as tboot is not installed");
            }
            getInstalledComponents();
        } catch (PlatformInfoException | IOException ex) {
            log.debug("Error while getting Host details", ex);
            throw new PlatformInfoException(ErrorCode.ERROR, "Error while getting Host details.", ex);
        }
    }

    private boolean isTbootNotInstalled(String tbootStatus) {
        return tbootStatus != null && !Boolean.valueOf(tbootStatus);
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
        String txtStatus = (String) map.get("txt-status");
        if ( txtStatus != null && !txtStatus.equals(FeatureStatus.UNSUPPORTED.getValue())) {
            context.setTxtEnabled(String.valueOf(txtStatus.equals(FeatureStatus.ENABLED.getValue())));
        }
    }
    private void getInstalledComponents() throws PlatformInfoException, IOException{
        String out = (String) map.get("installed-components");
        Set<String> installedComponents = new HashSet(Arrays.asList(out.split(",")));
        context.setInstalledComponents(installedComponents);
    }

    private void getTbootInstalled() throws PlatformInfoException, IOException{
        String tbootStatus = (String) map.get("tboot-status");
        if ( tbootStatus != null && !tbootStatus.equals(FeatureStatus.UNSUPPORTED.getValue())) {
            context.setTbootInstalled(String.valueOf(tbootStatus.equals(ComponentStatus.INSTALLED.getValue())));
        }
    }

    private void getHardwareFeatures() {
        Map<HardwareFeature, HardwareFeatureDetails> hardwareFeatureDetails = new HashMap<>();
        hardwareFeatureDetails.put(TPM, getTpmDetails());
        if (map.get("txt-status") != null && !map.get("txt-status").equals(FeatureStatus.UNSUPPORTED.getValue())) {
            hardwareFeatureDetails.put(TXT, getTxtDetails());
        }
        /*
        Add MKTME into HostInfo only if MKTME hardware feature is supported
         */
        if (map.get("suefi-status") != null && !map.get("suefi-status").equals(FeatureStatus.UNSUPPORTED.getValue())) {
            hardwareFeatureDetails.put(SUEFI, getSuefiDetails());
        }
        if (map.get("mktme-status") != null && !map.get("mktme-status").equals(FeatureStatus.UNSUPPORTED.getValue())) {
            hardwareFeatureDetails.put(MKTME, getMktmeDetails());
        }
        if (map.get("cbnt-status") != null && !map.get("cbnt-status").equals(FeatureStatus.UNSUPPORTED.getValue())) {
            hardwareFeatureDetails.put(CBNT, getCbntDetails());
        }
        context.setHardwareFeatures(hardwareFeatureDetails);
    }

    private HardwareFeatureDetails getSuefiDetails() {
        HardwareFeatureDetails suefi = new HardwareFeatureDetails();
        suefi.setEnabled(map.get("suefi-status").equals(FeatureStatus.ENABLED.getValue()));
        return suefi;
    }

    private HardwareFeatureDetails getCbntDetails() {
        HardwareFeatureDetails cbnt = new HardwareFeatureDetails();
        cbnt.setEnabled(map.get("cbnt-status").equals(FeatureStatus.ENABLED.getValue()));
        Map<String, String> meta = new HashMap<>();
        meta.put("profile", (String) map.get("cbnt-profile"));
        //TODO: Replace dummy values
        meta.put("force_bit", "true");
        meta.put("msr", "mk ris kfm");
        cbnt.setMeta(meta);
        return cbnt;
    }

    private HardwareFeatureDetails getMktmeDetails() {
        HardwareFeatureDetails mktme = new HardwareFeatureDetails();
        mktme.setEnabled(map.get("mktme-status").equals(FeatureStatus.ENABLED.getValue()));
        Map<String, String> meta = new HashMap<>();
        meta.put("encryption_algorithm", (String) map.get("mktme-encryption-algorithm"));
        meta.put("max_keys_per_cpu", (String) map.get("mktme-max-keys-per-cpu"));
        mktme.setMeta(meta);
        return mktme;
    }

    private HardwareFeatureDetails getTxtDetails() {
        HardwareFeatureDetails txt = new HardwareFeatureDetails();
        txt.setEnabled(map.get("txt-status").equals(FeatureStatus.ENABLED.getValue()));
        return txt;
    }

    private HardwareFeatureDetails getTpmDetails() {
        HardwareFeatureDetails tpm = new HardwareFeatureDetails();
        tpm.setEnabled(Boolean.valueOf((String) map.get("tpm-enabled")));
        Map<String, String> meta = new HashMap<>();
        meta.put("tpm_version", (String) map.get("tpm-version"));
        tpm.setMeta(meta);
        return tpm;
    }

    @Override
    public HostInfo getContext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
