/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dczech
 */
public class FixMakeCredentialWindowsTest {
    
    public FixMakeCredentialWindowsTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
        /* Untitled1 (8/24/2016 4:20:43 PM)
   StartOffset: 00000000, EndOffset: 00000137, Length: 00000138 */

/* Untitled1 (8/24/2016 4:21:26 PM)
   StartOffset: 00000000, EndOffset: 00000187, Length: 00000188 */

byte fromCit[] = {
	(byte)0x34, (byte)0x00, (byte)0x00, (byte)0x20, (byte)0x6D, (byte)0xBE,
	(byte)0x14, (byte)0x11, (byte)0x13, (byte)0x09, (byte)0xDA, (byte)0xA8,
	(byte)0x02, (byte)0xB5, (byte)0xF3, (byte)0xAC, (byte)0x20, (byte)0xCA,
	(byte)0x70, (byte)0xE8, (byte)0x02, (byte)0x7A, (byte)0xC1, (byte)0x1B,
	(byte)0x84, (byte)0xE0, (byte)0x36, (byte)0x42, (byte)0x10, (byte)0xB1,
	(byte)0xE6, (byte)0xD0, (byte)0x66, (byte)0xC0, (byte)0xE1, (byte)0x93,
	(byte)0xDB, (byte)0x23, (byte)0x02, (byte)0x0C, (byte)0x5E, (byte)0xA0,
	(byte)0xF5, (byte)0x8F, (byte)0x03, (byte)0x7A, (byte)0xD2, (byte)0x9B,
	(byte)0x87, (byte)0x54, (byte)0x4E, (byte)0x1E, (byte)0xC5, (byte)0x31,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x55, (byte)0xDE,
	(byte)0xC3, (byte)0x48, (byte)0xF4, (byte)0x59, (byte)0x68, (byte)0xD5,
	(byte)0xA5, (byte)0x1B, (byte)0x93, (byte)0xAC, (byte)0xCF, (byte)0x1E,
	(byte)0xBB, (byte)0x80, (byte)0xF8, (byte)0x73, (byte)0xB9, (byte)0x39,
	(byte)0x46, (byte)0x27, (byte)0xED, (byte)0xCC, (byte)0xA9, (byte)0xAF,
	(byte)0xCC, (byte)0xF2, (byte)0x5F, (byte)0xA4, (byte)0xEE, (byte)0x1C,
	(byte)0xBE, (byte)0xEF, (byte)0x4D, (byte)0xC7, (byte)0x8B, (byte)0xE1,
	(byte)0x5E, (byte)0xF9, (byte)0x5F, (byte)0x9F, (byte)0x00, (byte)0x5B,
	(byte)0x58, (byte)0xC8, (byte)0x10, (byte)0x0E, (byte)0xA9, (byte)0x1A,
	(byte)0xA3, (byte)0x4B, (byte)0x54, (byte)0x40, (byte)0x0F, (byte)0x3A,
	(byte)0x4E, (byte)0x97, (byte)0xFC, (byte)0x82, (byte)0x46, (byte)0x56,
	(byte)0x5C, (byte)0x37, (byte)0xF7, (byte)0x6F, (byte)0x58, (byte)0xE2,
	(byte)0x64, (byte)0x80, (byte)0x8A, (byte)0xE9, (byte)0x6E, (byte)0xE5,
	(byte)0xB3, (byte)0xBD, (byte)0x46, (byte)0xA0, (byte)0xBC, (byte)0x33,
	(byte)0xF3, (byte)0xED, (byte)0xD4, (byte)0xB4, (byte)0xFF, (byte)0x4E,
	(byte)0xE2, (byte)0x6B, (byte)0xD1, (byte)0x9E, (byte)0xA3, (byte)0x2D,
	(byte)0xC6, (byte)0xC7, (byte)0x77, (byte)0xA1, (byte)0x1C, (byte)0x9A,
	(byte)0xF0, (byte)0x39, (byte)0x21, (byte)0x73, (byte)0xF8, (byte)0xB9,
	(byte)0xCF, (byte)0x87, (byte)0xD2, (byte)0xE2, (byte)0x96, (byte)0xA5,
	(byte)0xE6, (byte)0xDC, (byte)0x3D, (byte)0x4A, (byte)0x71, (byte)0x88,
	(byte)0xA9, (byte)0xBF, (byte)0x52, (byte)0xFC, (byte)0x6E, (byte)0x3D,
	(byte)0xB1, (byte)0x64, (byte)0x81, (byte)0x7D, (byte)0x75, (byte)0x4F,
	(byte)0x73, (byte)0x66, (byte)0x48, (byte)0x35, (byte)0x5C, (byte)0xCF,
	(byte)0x39, (byte)0x1C, (byte)0x72, (byte)0x9A, (byte)0xDE, (byte)0x65,
	(byte)0xAD, (byte)0xD4, (byte)0xBC, (byte)0x41, (byte)0x6E, (byte)0x88,
	(byte)0x3E, (byte)0x3C, (byte)0x9A, (byte)0xB3, (byte)0x8E, (byte)0xBD,
	(byte)0xAE, (byte)0x0B, (byte)0x26, (byte)0x98, (byte)0x63, (byte)0xDA,
	(byte)0x93, (byte)0x63, (byte)0xE8, (byte)0xB5, (byte)0xB8, (byte)0x04,
	(byte)0xE3, (byte)0x20, (byte)0x9B, (byte)0x19, (byte)0x8E, (byte)0xCA,
	(byte)0x13, (byte)0x91, (byte)0xB1, (byte)0x27, (byte)0x9A, (byte)0x28,
	(byte)0x67, (byte)0xAF, (byte)0x41, (byte)0xB3, (byte)0x71, (byte)0x43,
	(byte)0xC0, (byte)0x62, (byte)0xD2, (byte)0xBD, (byte)0xF7, (byte)0xD2,
	(byte)0x5E, (byte)0xA8, (byte)0xDC, (byte)0x0B, (byte)0x97, (byte)0xF4,
	(byte)0x53, (byte)0x4B, (byte)0xDF, (byte)0x14, (byte)0x78, (byte)0x37,
	(byte)0xCA, (byte)0xC9, (byte)0x88, (byte)0x9C, (byte)0x28, (byte)0x90,
	(byte)0xAB, (byte)0xDA, (byte)0x17, (byte)0x7E, (byte)0x97, (byte)0xDE,
	(byte)0xA9, (byte)0xA2, (byte)0x63, (byte)0x2A, (byte)0x7D, (byte)0xE1,
	(byte)0xFE, (byte)0x3B, (byte)0xD0, (byte)0x15, (byte)0x26, (byte)0xFB,
	(byte)0x15, (byte)0x69, (byte)0x04, (byte)0x8F, (byte)0x39, (byte)0x75,
	(byte)0x93, (byte)0x4B, (byte)0xC7, (byte)0xAA, (byte)0x7D, (byte)0x19,
	(byte)0xCA, (byte)0x75, (byte)0xE8, (byte)0xB1, (byte)0xDA, (byte)0x89,
	(byte)0x6C, (byte)0x79, (byte)0x54, (byte)0xC9, (byte)0xA9, (byte)0xE9,
	(byte)0xD3, (byte)0xC0, (byte)0x4D, (byte)0x47, (byte)0xF5, (byte)0xFF,
	(byte)0xA7, (byte)0x63
};
 
    
byte expected[] = {
	(byte)0x00, (byte)0x34, (byte)0x00, (byte)0x20, (byte)0x6D, (byte)0xBE,
	(byte)0x14, (byte)0x11, (byte)0x13, (byte)0x09, (byte)0xDA, (byte)0xA8,
	(byte)0x02, (byte)0xB5, (byte)0xF3, (byte)0xAC, (byte)0x20, (byte)0xCA,
	(byte)0x70, (byte)0xE8, (byte)0x02, (byte)0x7A, (byte)0xC1, (byte)0x1B,
	(byte)0x84, (byte)0xE0, (byte)0x36, (byte)0x42, (byte)0x10, (byte)0xB1,
	(byte)0xE6, (byte)0xD0, (byte)0x66, (byte)0xC0, (byte)0xE1, (byte)0x93,
	(byte)0xDB, (byte)0x23, (byte)0x02, (byte)0x0C, (byte)0x5E, (byte)0xA0,
	(byte)0xF5, (byte)0x8F, (byte)0x03, (byte)0x7A, (byte)0xD2, (byte)0x9B,
	(byte)0x87, (byte)0x54, (byte)0x4E, (byte)0x1E, (byte)0xC5, (byte)0x31,
	(byte)0x01, (byte)0x00, (byte)0x55, (byte)0xDE, (byte)0xC3, (byte)0x48,
	(byte)0xF4, (byte)0x59, (byte)0x68, (byte)0xD5, (byte)0xA5, (byte)0x1B,
	(byte)0x93, (byte)0xAC, (byte)0xCF, (byte)0x1E, (byte)0xBB, (byte)0x80,
	(byte)0xF8, (byte)0x73, (byte)0xB9, (byte)0x39, (byte)0x46, (byte)0x27,
	(byte)0xED, (byte)0xCC, (byte)0xA9, (byte)0xAF, (byte)0xCC, (byte)0xF2,
	(byte)0x5F, (byte)0xA4, (byte)0xEE, (byte)0x1C, (byte)0xBE, (byte)0xEF,
	(byte)0x4D, (byte)0xC7, (byte)0x8B, (byte)0xE1, (byte)0x5E, (byte)0xF9,
	(byte)0x5F, (byte)0x9F, (byte)0x00, (byte)0x5B, (byte)0x58, (byte)0xC8,
	(byte)0x10, (byte)0x0E, (byte)0xA9, (byte)0x1A, (byte)0xA3, (byte)0x4B,
	(byte)0x54, (byte)0x40, (byte)0x0F, (byte)0x3A, (byte)0x4E, (byte)0x97,
	(byte)0xFC, (byte)0x82, (byte)0x46, (byte)0x56, (byte)0x5C, (byte)0x37,
	(byte)0xF7, (byte)0x6F, (byte)0x58, (byte)0xE2, (byte)0x64, (byte)0x80,
	(byte)0x8A, (byte)0xE9, (byte)0x6E, (byte)0xE5, (byte)0xB3, (byte)0xBD,
	(byte)0x46, (byte)0xA0, (byte)0xBC, (byte)0x33, (byte)0xF3, (byte)0xED,
	(byte)0xD4, (byte)0xB4, (byte)0xFF, (byte)0x4E, (byte)0xE2, (byte)0x6B,
	(byte)0xD1, (byte)0x9E, (byte)0xA3, (byte)0x2D, (byte)0xC6, (byte)0xC7,
	(byte)0x77, (byte)0xA1, (byte)0x1C, (byte)0x9A, (byte)0xF0, (byte)0x39,
	(byte)0x21, (byte)0x73, (byte)0xF8, (byte)0xB9, (byte)0xCF, (byte)0x87,
	(byte)0xD2, (byte)0xE2, (byte)0x96, (byte)0xA5, (byte)0xE6, (byte)0xDC,
	(byte)0x3D, (byte)0x4A, (byte)0x71, (byte)0x88, (byte)0xA9, (byte)0xBF,
	(byte)0x52, (byte)0xFC, (byte)0x6E, (byte)0x3D, (byte)0xB1, (byte)0x64,
	(byte)0x81, (byte)0x7D, (byte)0x75, (byte)0x4F, (byte)0x73, (byte)0x66,
	(byte)0x48, (byte)0x35, (byte)0x5C, (byte)0xCF, (byte)0x39, (byte)0x1C,
	(byte)0x72, (byte)0x9A, (byte)0xDE, (byte)0x65, (byte)0xAD, (byte)0xD4,
	(byte)0xBC, (byte)0x41, (byte)0x6E, (byte)0x88, (byte)0x3E, (byte)0x3C,
	(byte)0x9A, (byte)0xB3, (byte)0x8E, (byte)0xBD, (byte)0xAE, (byte)0x0B,
	(byte)0x26, (byte)0x98, (byte)0x63, (byte)0xDA, (byte)0x93, (byte)0x63,
	(byte)0xE8, (byte)0xB5, (byte)0xB8, (byte)0x04, (byte)0xE3, (byte)0x20,
	(byte)0x9B, (byte)0x19, (byte)0x8E, (byte)0xCA, (byte)0x13, (byte)0x91,
	(byte)0xB1, (byte)0x27, (byte)0x9A, (byte)0x28, (byte)0x67, (byte)0xAF,
	(byte)0x41, (byte)0xB3, (byte)0x71, (byte)0x43, (byte)0xC0, (byte)0x62,
	(byte)0xD2, (byte)0xBD, (byte)0xF7, (byte)0xD2, (byte)0x5E, (byte)0xA8,
	(byte)0xDC, (byte)0x0B, (byte)0x97, (byte)0xF4, (byte)0x53, (byte)0x4B,
	(byte)0xDF, (byte)0x14, (byte)0x78, (byte)0x37, (byte)0xCA, (byte)0xC9,
	(byte)0x88, (byte)0x9C, (byte)0x28, (byte)0x90, (byte)0xAB, (byte)0xDA,
	(byte)0x17, (byte)0x7E, (byte)0x97, (byte)0xDE, (byte)0xA9, (byte)0xA2,
	(byte)0x63, (byte)0x2A, (byte)0x7D, (byte)0xE1, (byte)0xFE, (byte)0x3B,
	(byte)0xD0, (byte)0x15, (byte)0x26, (byte)0xFB, (byte)0x15, (byte)0x69,
	(byte)0x04, (byte)0x8F, (byte)0x39, (byte)0x75, (byte)0x93, (byte)0x4B,
	(byte)0xC7, (byte)0xAA, (byte)0x7D, (byte)0x19, (byte)0xCA, (byte)0x75,
	(byte)0xE8, (byte)0xB1, (byte)0xDA, (byte)0x89, (byte)0x6C, (byte)0x79,
	(byte)0x54, (byte)0xC9, (byte)0xA9, (byte)0xE9, (byte)0xD3, (byte)0xC0,
	(byte)0x4D, (byte)0x47, (byte)0xF5, (byte)0xFF, (byte)0xA7, (byte)0x63
};

    
    @Test
    public void TestMakeCredentialFix() throws IOException {                        
        byte[] fixed = RequestAikCertificate.fixMakeCredentialBlobForWindows(fromCit);
        Assert.assertArrayEquals(expected, fixed);
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}
