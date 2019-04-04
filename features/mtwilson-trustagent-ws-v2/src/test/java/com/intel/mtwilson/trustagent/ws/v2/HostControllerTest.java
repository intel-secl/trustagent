/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.intel.mtwilson.core.common.model.HostInfo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author purvades
 */

public class HostControllerTest {
    
    public HostControllerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testGetContextLinux() throws Exception {
        String osName = "linux";
        HostController hostcntrl = new HostController();
        hostcntrl.setbuildandexecute(new CommandLineRunnerTest(osName));
        hostcntrl.setOsName(osName);
        hostcntrl.execute();
        HostInfo host = hostcntrl.getContext();
        assertEquals("RedHatEnterpriseServer", host.getOsName());
        assertEquals("7.3", host.getOsVersion());
        assertEquals("F2 06 03 00 FF FB EB BF", host.getProcessorInfo());
        assertEquals("Intel Corporation", host.getBiosName());
        assertEquals("SE5C610.86B.01.01.1008.031920151331", host.getBiosVersion());
        assertEquals("801E2ABC-CB28-E411-906E-0012795D96DD", host.getHardwareUuid());
        assertEquals("fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca "+
                "cmov pat pse36 clflush dts acpi mmx fxsr sse sse2 ss ht tm pbe "+
                "syscall nx pdpe1gb rdtscp lm constant_tsc arch_perfmon pebs bts "+
                "rep_good nopl xtopology nonstop_tsc aperfmperf eagerfpu pni "+
                "pclmulqdq dtes64 monitor ds_cpl vmx smx est tm2 ssse3 fma cx16 "+
                "xtpr pdcm pcid dca sse4_1 sse4_2 x2apic movbe popcnt tsc_deadline_timer "+
                "aes xsave avx f16c rdrand lahf_lm abm ida arat epb pln pts dtherm "+
                "tpr_shadow vnmi flexpriority ept vpid fsgsbase tsc_adjust bmi1 avx2 "+
                "smep bmi2 erms invpcid cqm xsaveopt cqm_llc cqm_occup_llc", host.getProcessorFlags());
        assertEquals("1.2", host.getTpmVersion());
        assertEquals("purva-host", host.getHostName());
        assertEquals("2", host.getNoOfSockets());
        assertEquals("true", host.getTpmEnabled());
        assertEquals("true", host.getTxtEnabled());
    }
    
    @Test
    public void testGetContextWindows() throws Exception {
        String osName = "windows";
        HostController hostcntrl = new HostController();
        hostcntrl.setbuildandexecute(new CommandLineRunnerTest(osName));
        hostcntrl.setOsName(osName);
        hostcntrl.execute();
        HostInfo host = hostcntrl.getContext();
        assertEquals("Microsoft Windows 10 Enterprise", host.getOsName().trim().replaceAll("[\u0000-\u001f]", ""));
        assertEquals("10.0.10586", host.getOsVersion().trim().replaceAll("[\u0000-\u001f]", ""));
        assertEquals("HP", host.getBiosName().trim().replaceAll("[\u0000-\u001f]", ""));
        assertEquals("N75 Ver. 01.13", host.getBiosVersion().trim().replaceAll("[\u0000-\u001f]", ""));
        assertEquals("BFEBFBFF000406E3", host.getProcessorInfo().trim().replaceAll("[\u0000-\u001f]", ""));
        assertEquals("C8C8411F-F0CB-11E5-8343-9025330C6062", host.getHardwareUuid().trim().replaceAll("[\u0000-\u001f]", ""));
        assertEquals("1.2", host.getTpmVersion().trim().replaceAll("[\u0000-\u001f]", ""));
        assertEquals("WIN-GLU9NEPGT1L", host.getHostName().trim().replaceAll("[\u0000-\u001f]", ""));
        assertEquals("2", host.getNoOfSockets());
        assertEquals("true", host.getTpmEnabled());
        assertEquals("false", host.getTxtEnabled());
    }
}
