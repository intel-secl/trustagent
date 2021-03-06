/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mountwilson.trustagent;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
/**
 *
 * @author jbuhacoff
 */
public class Diagnostic {
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        checkBouncycastleAlgorithms();
    }

    public static void checkBouncycastleAlgorithms() {
        printAvailableAlgorithms();
        tryMacWithPassword("HmacSHA256", "hello world", "xyzzy");
        trySignature();
    }
    
    private static void printAvailableAlgorithms() {
        for (Provider provider: Security.getProviders()) {
          System.out.println(provider.getName());
          for (String key: provider.stringPropertyNames()) {
            System.out.println("\t" + key + "\t" + provider.getProperty(key));
          }
        }        
    }
    
    private static void tryMacWithPassword(String algorithmName, String message, String password) {
        try {
            SecretKeySpec key = new SecretKeySpec(password.getBytes(), algorithmName);
            Mac mac = Mac.getInstance(algorithmName, "BC"); // a string like "HmacSHA256"
            mac.init(key);
            byte[] digest = mac.doFinal(message.getBytes());
            System.out.println("Created "+algorithmName+" digest of length "+digest.length);
        }
        catch(NoSuchProviderException e) {
            System.err.println("Cannot use provider: BC: "+e.toString());
        }
        catch(NoSuchAlgorithmException e) {
            System.err.println("Cannot use algorithm: "+algorithmName+": "+e.toString());
        }
        catch(InvalidKeyException e) {
            System.err.println("Invalid key:"+e.toString());
        }
    }
    
    private static void trySignature() {
        String algorithmName = "SHA384withRSA";
        try {
            // generate keypair
            KeyPair keyPair = KeyPairGenerator.getInstance("RSA", "BC").generateKeyPair(); // NoSuchAlgorithmException, NoSuchProviderException
            PrivateKey privateKey = keyPair.getPrivate();
            String plaintext = "This is the message being signed";

            // generate signature
            Signature instance = Signature.getInstance("SHA384withRSA", "BC"); // NoSuchAlgorithmException, NoSuchProviderException
            instance.initSign(privateKey); // InvalidKeyException
            instance.update((plaintext).getBytes()); // SignatureException
            byte[] signature = instance.sign();

            System.out.println("Generated SHA384 with RSA signature of length: "+signature.length);
        }
        catch(NoSuchProviderException e) {
            System.err.println("Cannot use provider: BC: "+e.toString());
        }
        catch(NoSuchAlgorithmException e) {
            System.err.println("Cannot use algorithm: "+algorithmName+": "+e.toString());            
        }
        catch(InvalidKeyException e) {
            System.err.println("Cannot use key: "+e.toString());
        }
        catch(SignatureException e) {
            System.err.println("Cannot generate signature: "+e.toString());
        }
    }
}
