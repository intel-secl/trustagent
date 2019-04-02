/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package test.diagnostic;

import com.intel.mountwilson.trustagent.Diagnostic;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class BouncycastleTest {
    @Test
    public void testBouncycastlePresent() {
        Diagnostic.main(null);
    }
}
