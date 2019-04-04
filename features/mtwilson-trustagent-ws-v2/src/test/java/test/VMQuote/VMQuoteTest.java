/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package test.VMQuote;

import com.intel.dcsg.cpg.xml.JAXB;
import com.intel.mtwilson.vmquote.xml.TrustPolicy;
import com.intel.mtwilson.vmquote.xml.VMQuote;
import org.junit.Test;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;

/**
 *
 * @author ssbangal
 */
@Ignore
public class VMQuoteTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VMQuoteTest.class);
    private static final String instanceFolderPath = "c:/temp/vmquotetest/";
    private static final String measurementXMLFileName = "measurement.xml";
    private static final String trustPolicyFileName = "TrustPolicy-201503161031.xml";
    private static final String vmQuoteFileName = "VMQuote.xml";
    
    
    @Test
    public void CreateVMQuote() throws Exception {
    }
    
    @Test
    public void CreateVMQuoteResponse() throws Exception {

        VMQuote vmquote = new VMQuote();
        byte[] vmQuoteBytes = FileUtils.readFileToByteArray(new File(String.format("%s%s", instanceFolderPath, vmQuoteFileName)));
        String vmQuoteString = IOUtils.toString(vmQuoteBytes, "UTF-8");
        
        byte[] tpBytes = FileUtils.readFileToByteArray(new File(String.format("%s%s", instanceFolderPath, trustPolicyFileName)));
        String tpString = IOUtils.toString(tpBytes, "UTF-8");
        
        JAXB jaxb = new JAXB();
        VMQuote read = jaxb.read(vmQuoteString, VMQuote.class);
        TrustPolicy tp = jaxb.read(tpString, TrustPolicy.class);
        
        log.debug(tp.getLaunchControlPolicy());
        log.debug(read.getCumulativeHash());
        
    }  
}
