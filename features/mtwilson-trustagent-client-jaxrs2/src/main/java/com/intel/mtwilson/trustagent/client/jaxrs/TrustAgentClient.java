/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.client.jaxrs;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.jaxrs2.client.MtWilsonClient;
import com.intel.mtwilson.jaxrs2.mediatype.CryptoMediaType;
import com.intel.mtwilson.trustagent.model.*;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
/**
 * The Trust Agent resides on physical servers and enables both remote attestation and the extended chain of trust capabilities.
 * The Trust Agent acts as a primary interface between the host, TPM and the host verification server. It maintains ownership 
 * of the server's Trusted Platform Module, allowing secure attestation quotes to be sent to the Host Verification Service.  
 * Incorporating the HVS HostInfo, TPM Info and TpmProvider libraries, the Trust Agent serves to report on platform security 
 * capabilities and platform integrity measurements. The Trust Agent is supported for Windows Server and Red Hat Enterprise Linux (RHEL). 

 * @author ssbangal
 */
public class TrustAgentClient extends MtWilsonClient {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrustAgentClient.class);
    
    
    /**
     * This constructor is used only for API client only. 
     * @param properties client properties
     * <pre>
     * This java properties model must include server connection details for the API client initialization.
     * 
     * This constructor is used only for API client only.
     * 
     * For example it should include following details in properties:
     * 
     * mtwilson.api.url - trustagent URL
     * 
     * // basic authentication
     * 
     * mtwilson.api.username - trustagent api username
     * mtwilson.api.password - trustagent api password
     * mtwilson.api.tls.policy.certificate.sha256 - trustagent tls certificate sha256 value
     * </pre>
     * @throws Exception     
     * 
     * <b>Sample Java API call :</b><br>
     * <pre>
     * {
    *       Properties properties = new Properties();
    *       properties.put("mtwilson.api.baseurl", "https://trustagent.server.com:1443/v2");
    *       properties.put("mtwilson.api.username", "tagentadmin");
    *       properties.put("mtwilson.api.password", "TAgentAdminPassword");
    *       properties.put("mtwilson.api.tls.policy.certificate.sha256", "003da58915bde878516b315f8fde9277d20e4df71b68602fbcfe0ebfda0e7afe");
    *       TrustAgentClient client = new TrustAgentClient(properties);
    * }
    * </pre>
    */
    
    public TrustAgentClient(Properties properties) throws Exception {
        super(properties);
    }
    
     /**
     * Each server to be attested is registered with the Host Verification Server. This process includes setting the expected
     * values for future attestations and the generation of the Attestation Identity Key (AIK). The AIK is an asymmetric keypair
     * generated by the host's Trusted Platform Module for the purpose of cryptographically securing attestation quotes for 
     * transmission to the Host Verification Server. The getAik REST API is used to retrieve the public Attestation Identity Key (AIK) 
     * certificate for the host. The required content type can also be specified as an extension in the URL.
     * @return AIK x509 certificate
     * @since ISecL 1.0
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned PKIX_CERT/X_PEM_FILE
     * <br>The accept header should not be set for /v2/aik, this output is same as /v2/aik.cer 
     * @mtwMethodType GET
     * @mtwSampleRestCall      <pre>
     * https://trustagent.server.com:1443/v2/aik
     * https://trustagent.server.com:1443/v2/aik.pem
     * https://trustagent.server.com:1443/v2/aik.cer
     * 
     *
     * Output:
     * X509Certificate certificate in DER format or PEM format:
     *      -----BEGIN CERTIFICATE-----
     *      MIIDMjCCAhqgAwIBAgIGAU92j4SVMA0GCSqGSIb3DQEBBQUAMBsxGTAXBgNVBAMTEG10d2lsc29u
     *      LXBjYS1haWswHhcNMTUwODI4MjMwNjAxWhcNMjUwODI3MjMwNjAxWjAbMRkwFwYDVQQDExBtdHdp
     *      bHNvbi1wY2EtYWlrMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArRUrsziH8nIJWPtA
     *      CAXbugYI9yX/KmwtG2vdBFCon+FcT6zidynaUtUqTLPmMigVEsWiEhbVxNDPr+rKponkjDmeSn/w
     *      WGFp/dETtKLYLUTW1Aij7DFmz6+draAB6k4m0JcVvCM+Xevs2VG1kBOxC94GtKtO9ycLFzTGlxTJ
     *      FlRkoyd4qM45O8Xc/qS3xF2gNLNqhWzzQNWG/rJXK1o8k/7EIcvW9tRvGTBj+STKZiAG/gomSY8b
     *      0avhrtOIgFeV8oYbolPu7RaxuPbfXBoEpw7fnDwiCowm9dxAOQpJ02ZP5cj4ZbVHWULcBL/gY4T6
     *      AZvQ2EZAqRIJ3LX/7fsSewIDAQABo3wwejAdBgNVHQ4EFgQUW7eXsmNIQ4buvbJlWuOoTau3Pykw
     *      DwYDVR0TAQH/BAUwAwEB/zBIBgNVHSMEQTA/gBRbt5eyY0hDhu69smVa46hNq7c/KaEfpB0wGzEZ
     *      MBcGA1UEAxMQbXR3aWxzb24tcGNhLWFpa4IGAU92j4SVMA0GCSqGSIb3DQEBBQUAA4IBAQCUgor4
     *      oNnnqukBT0B8C+zAPUm0w0yrvxM8YmaAIodKOhFIF9OuR/gWzAi2lzxsGoaPKqYEeZFQpMlQ8AvK
     *      fZj6tBK7iUy0zFcuMqdvwMhXX2h3ryaw0Qslspy7HY3CIX6Qck5G2zAJBlHBd7ZXLVWcoTWa56o1
     *      mNqUhftOBLi+DlB8klD7Z6/Un+XVlBTk5uimgT42WF0XupHJrOF0tx767JcopZQSeYbdiugQEztz
     *      IKmdGysVyg+7F7hkhrQfLZsohLJ54Zvgrq5+nKF0Rj2zzoImlPtYUKV5EnQm2+SsLxr3GP1flm6M
     *      sHIC30ht3TBDoVw8vh80jxsu75afi4Al
     *      -----END CERTIFICATE-----
     * </pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre><xmp>
     *   TrustAgentClient client = new TrustAgentClient(properties);
     *   X509Certificate aik = client.getAik();
     * </xmp></pre></div>
     */        
    public X509Certificate getAik() {
        log.debug("target: {}", getTarget().getUri().toString());
        X509Certificate aik = getTarget()
                .path("/aik")
                .request()
                .accept(CryptoMediaType.APPLICATION_PKIX_CERT)
                .get(X509Certificate.class);
        return aik;
    }

     /**
     * Retrieves the CA certificate that signed the Attestation Identity Key (AIK) certificate for the host. 
     * @return AIK CA x509 certificate
     * @since ISecL 1.0
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned PKIX_CERT/X_PEM_FILE
     * <br>The accept header should not be set for /v2/aik/ca, this output is same as /v2/aik/ca.cer
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * <pre>
     * https://trustagent.server.com:1443/v2/aik/ca
     * https://trustagent.server.com:1443/v2/aik/ca.pem
     * https://trustagent.server.com:1443/v2/aik/ca.cer
     * 
     * Output:
     * X509Certificate certificate in DER format or PEM format:
     *  -----BEGIN CERTIFICATE-----
     *  MIICvTCCAaWgAwIBAgIGAU+KI+kQMA0GCSqGSIb3DQEBBQUAMBsxGTAXBgNVBAMTEG10d2lsc29u
     *  LXBjYS1haWswHhcNMTUwOTAxMTgyMDUzWhcNMjUwODMxMTgyMDUzWjAAMIIBIjANBgkqhkiG9w0B
     *  AQEFAAOCAQ8AMIIBCgKCAQEAqnT+nx5W0c3Hm5yFIfXYbaYi86wC1LDqqVCHRzeFlO07moZw1oV/
     *  ucwF/LOmepouxWRI7RVRdTZD6KV52O+Iu2kIHZ1UXWNmL+9BrGWufvByZy1f3u08TGl7WSuKVWFK
     *  UPsQ+5XITMaknZlK+ldog2VbyNNwvty8yo/mFx2fnVrMmDz03E+pE1zUyIgqKSomlyS+rGlAl8ZD
     *  1cKKiZc8ZCRh38lLGjTalRXPGCnOTi3uK/P7wut3yynJM1ZEr9Vc6QYxcX8O3vd/RIkF0GqPJrh+
     *  Xu0hWUPy1Eviz85NsHnQ2nZ79VC0VS0nqLIPKg5uqIyohGgppK41KWvC545nAQIDAQABoyIwIDAe
     *  BgNVHREBAf8EFDASgRBISVMgSWRlbnRpdHkgS2V5MA0GCSqGSIb3DQEBBQUAA4IBAQA6qJLucSWy
     *  dFb0BPvlsyYYFSdjPaGAFWFwh/lbHYI1Ouy3jw34gmZIR0xTSI/96NA5KO17bzhzvKg9+nsPIS5I
     *  81GBiIaPc4HPAuqi21jBCI/LZQIC61P1R6/Tmzosm8NrRX+VVn+NmBVp2rXFtBb6BmBmyx7D7cNZ
     *  b6+C6DQ+gg2PlU8qAjAzF0iQUqzELL8LIzIMtVDJYSdHe4kgyFom3mnBwfhpUmsnv0U2YAsdgcH5
     *  +uZPD/+j3en5u8O5rNY15onq+2pFIxA/F29DwWCuOlF4orc9ejPv5hdVqsHjUR0zPPj87gLeHUbj
     *  vDTmD6JzA3PbuypM/bFZrELA7oT0
     *  -----END CERTIFICATE-----
     * </pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre><xmp>
     *   TrustAgentClient client = new TrustAgentClient(properties);
     *   X509Certificate aikCa = client.getAikCa();
     * </xmp></pre></div>
     */        
    public X509Certificate getAikCa() {
        log.debug("target: {}", getTarget().getUri().toString());
        X509Certificate aik = getTarget()
                .path("/aik/ca")
                .request()
                .accept(CryptoMediaType.APPLICATION_PKIX_CERT)
                .get(X509Certificate.class);
        return aik;
    }
    
     /**
     * Retrieves the host specific information from the host.  
     * @return HostInfo java model object having the details of the host.
     * <pre>
     *  The HostInfo java model object has the following host related attributes:
     * 
     *       hostName               Name of the host.
     *       biosName               Name of the BIOS.
     *       hardwareUuid           The hardware UUID of the host.
     *       osName                 The name of the operating system installed on the host.
     *       osVersion              The verison of OS installed on the host.
     *       processorInfo          The processor ID of the host. 
     *       vmmName                The vmm name.
     *       vmmVersion             The vmm version
     *       processorFlags         Processor flags of the host. 
     *       tpmVersion             The version of the tpm on the host. 
     *       pcrBanks               PCR banks suported by the host. The host can support either sha1, sha256 or both.
     *       noOfSockets            The number of sockets in the host.
     *       tpmEnabled             Status of TPM.
     *       txtEnabled             Status of TXT.
     * 
     * </pre>
     * @since ISecL 1.0
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON/XML
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * <pre>
     * https://trustagent.server.com:1443/v2/host
     * https://trustagent.server.com:1443/v2/host.json
     * 
     *
     * Output:
     *   {
     *     "host_name"      : "purley3",
     *     "bios_name"      : "Intel Corporation",
     *     "bios_version"   : "SE5C620.86B.00.01.0004.071220170215",
     *     "hardware_uuid"  : "0005AE6E-36D6-E711-906E-001560A04062",
     *     "os_name"        : "RedHatEnterpriseServer",
     *     "os_version"     : "7.4",
     *     "processor_info" : "54 06 05 00 FF FB EB BF",
     *     "vmm_name"       : "",
     *     "vmm_version"    : "",
     *     "processor_flags": "fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush dts acpi mmx fxsr 
     *                        sse sse2 ss ht tm pbe syscall nx pdpe1gb rdtscp lm constant_tsc art arch_perfmon pebs bts rep_good
     *                        nopl xtopology nonstop_tsc aperfmperf eagerfpu pni pclmulqdq dtes64 monitor ds_cpl vmx smx est tm2
     *                        ssse3 fma cx16 xtpr pdcm pcid dca sse4_1 sse4_2 x2apic movbe popcnt tsc_deadline_timer aes xsave 
     *                        avx f16c rdrand lahf_lm abm 3dnowprefetch epb cat_l3 cdp_l3 invpcid_single intel_pt tpr_shadow vnmi
     *                        flexpriority ept vpid fsgsbase tsc_adjust bmi1 hle avx2 smep bmi2 erms invpcid rtm cqm mpx rdt_a 
     *                        avx512f avx512dq rdseed adx smap clflushopt clwb avx512cd avx512bw avx512vl xsaveopt xsavec xgetbv1
     *                        cqm_llc cqm_occup_llc cqm_mbm_total cqm_mbm_local dtherm ida arat pln pts hwp hwp_act_window hwp_epp
     *                        hwp_pkg_req",
     *     "tpm_version"    : "2.0",
     *     "pcr_banks"      : [
     *                          "SHA1",
     *                          "SHA256"
     *                        ],
     *     "no_of_sockets"  : "2",
     *     "tpm_enabled"    : "true",
     *     "txt_enabled"    : "true"
     *    }
     * </pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre><xmp>
     *   TrustAgentClient client = new TrustAgentClient(properties);
     *   HostInfo hostInfo = client.getHostInfo();
     * </xmp></pre></div>
     */        
    public HostInfo getHostInfo() {
        log.debug("target: {}", getTarget().getUri().toString());
        HostInfo hostInfo = getTarget()
                .path("/host")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get(HostInfo.class);
        return hostInfo;
    }
    
    
     /**
     * Creates a new Asset Tag certificate in x509 format.  Asset Tag certificates contain all key/value pairs to be tagged to a specific host,
     * and the subject of the certificate is the hardware UUID of the host to be tagged. In this way Asset Tag certificates are unique for each 
     * tagged host. The Asset tag provisioning on Linux and windows hosts involves writing the sha256 value of the asset tag certificate on to a particular NVRAM index. 
     * The trustagent library code uses 0x40000010 index in case of TPM1.2 hosts and 0x1c10110 index in case of TPM2.0 hosts. Please refer to the product 
     * guide for Asset Tag provisioning on ESXI hosts.   
     * @param tag The tag value is calculated by taking a base 64 encoded value of an Asset Tag certificate, which is then decoded using an Openssl library and 
     * finally get the a sha1 or sha256 digest of that decoded certificate.
     * @param hardwareUuid The hardware UUID of the target host on which the asset tag to be provisioned or deployed. 
     * @since ISecL 1.0
     * @mtwRequiresPermissions None
     * @mtwMethodType POST
     * @mtwSampleRestCall
     * <pre>
     * https://trustagent.server.com:1443/v2/tag
     * Input:
     *      { 
     *          "tag"             : "tHgfRQED1+pYgEZpq3dZC9ONmBCZKdx10LErTZs1k/k=", 
     *          "hardware_uuid"   : "7a569dad-2d82-49e4-9156-069b0065b262" 
     *      }
     *
     * Output: 
     *  This API does not return any output. 
     * 
     * </pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre><xmp>
     *   TrustAgentClient client = new TrustAgentClient(properties);
     *   //data = byte array of x509 certificate containing asset tags
     *   byte[] tag = Sha256Digest.digestOf(data).toByteArray();
     *   //Hardware UUID retrieved from dmidecode or host info API
     *   UUID hardwareUuid = UUID.valueOf("7a569dad-2d82-49e4-9156-069b0065b262");
     *   client.writeTag(tag, hardwareUuid);
     * </xmp></pre></div>
     */          
    public void writeTag(byte[] tag, UUID hardwareUuid) {
        TagWriteRequest tagWriteRequest = new TagWriteRequest();
        tagWriteRequest.setTag(tag);
        tagWriteRequest.setHardwareUuid(hardwareUuid);
        getTarget()
                .path("/tag")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.json(tagWriteRequest));
    }
    
     /**
     * The TPM quote operation returns signed data and a signature. The data that is signed contains the PCRs selected for the operation,
     * the composite hash for the selected PCRs, and a nonce provided as input, and used to prevent replay attacks. At provisioning time, 
     * the data that is signed is stored, not just the composite hash. The signature is discarded. 
     * This API is used to retrieve the AIK signed quote from TPM.  
     * @param tpmQuoteRequest is a java model object of TpmQuoteRequest.
     * 
     * <pre>
     *       The CertificateRequestLocator model class object contains the following attributes:
     *         - nonce      - The nonce value is 20 bytes base64-encoded. The client chooses the nonce. The trust agent will 
     *           (required)   automatically extend its IP address to the nonce before using it in the quote. The extend operation is 
     *                        nonce1 = sha1( nonce0 || ip-address ) where nonce0 is the original input nonce (20 bytes) and nonce1 
     *                        is the extended nonce used in the TPM quote (20 bytes), and the ip-address is the 4-byte encoding of 
     *                        the IP address.
     *         
     *         - pcrs       - List of PCRs for which the quote is needed.
     *           (required)
     * 
     *         - pcrBanks   - TPM PCR bank to read.
     *           (optional)
     * </pre>
     * 
     * 
     * @return TpmQuoteResponse object having the details of the current status of the TPM and its PCR values.
     * The output is base64-encoded in both XML and JSON output formats. 
     * @since ISecL 1.0
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON/XML
     * @mtwMethodType POST
     * @mtwSampleRestCall
     * <pre><xmp>
     * https://trustagent.server.com:1443/v2/tpm/quote
     * https://trustagent.server.com:1443/v2/tpm/quote.json
     * 
     * Input:
     * {
     *      "nonce":"tHgfRQED1+pYgEZpq3dZC9ONmBCZKdx10LErTZs1k/k=",
     *      "pcrs":[0,17,18,19],
     *      "pcrbanks": ["SHA1","SHA256"]
     *
     * }
     * 
     * Output:
     *   {
     *      "timestamp"         : 1520881400500,
     *      "client_ip"         : "192.168.122.1",
     *      "error_code"        : "0",
     *      "error_message"     : "OK",
     *      "aik"               : "MIICzjCCAbagAwIBAgIGAWIapM8RMA0GCSqGSIb3DQEBCwUAMBsxGTAXBgNVBAMTEG10d2lsc29uLXBjYS1haWswHhcNMTgwMzEyMTQzNzExWhcNMjgwMzExMTUzNzExWjAAMI
     *                            IBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn2pNNOb7uImBAr/mF3GlGfTLO961rCXVmcSEOKsmkjMvo50m94nOgU/heSPPWGDfe5/F/8bo5ld9W7RKpzvAMiLku60r
     *                            KVYi4Cw7tN2lm5I7WeVJaMZuJhu9I+idB0RkcrShJ2NY70qCtXsSE59TTRAJall6H+jgIRvWKzFMlO0YpUB9s3kseFZKYM4o/2HJFwxbdqPQl2H9j4Khg3LIWvMQgQdUIEdtSH
     *                            rT6wmuHqSbqTBzMk2l05iZhuzw5U9ADQNvnlxkan7TilZcAPzo/hCCMz+WWJnnPHejwin3+P2fWvgLCrAAH+HlPq0qnbOn3Z+JAN1vdT0NZg/cR9C0NwIDAQABozMwMTAvBgNV
     *                            HREBAf8EJTAjgSEACzf9ef39UwFybv0B/TE4/Tk6/f1SMlIRcf07/Y79/f0wDQYJKoZIhvcNAQELBQADggEBAD0/I0EzchjafJx495Zd03w68UzJ7U33QcwQGfURIecPL9Ftfq
     *                            5wCwYpnEcnrAG7KwBXgjs5h4F2UqySb7o8RsYXc7fkZzqxe+RQ3pCNOIy+wQK+I+f0kpHCJcKsuBXSN4T3lKqVUDR79K6NL2aAJBYEk416fQDg3Q1gul3qMvXl649n8eClErKw
     *                            DrBW+9fZe3t0+g+JRieZOUBSieQKHkAcY8uACp2dubvQN9R6XYEXx9lGGidyNiPTVGAxfTZNLBM6/5uoALds785s1lPCRUIczkb3ycCrZ6RvNUi671o4M09vxB1aKrKIjKypgx
     *                            fZqFpEuD4d7PpLag2ivECPeAU=",
     *      "quote"             : "iwD/VENHgBgAIgAL1sVy6dZuOwx9hyZsegfJT3jXqHaLKesPI2JKRpePbuYAFK7HVD6cACf8tf8gfEZRWx1T3vw6AAAAAAEZPpIAAAADAAAAAQEABwAoAAgyAAAAAAIABAMBA
     *                            A4ACwMBAA4AIHgIvSOvjqLgEGLzcH0HfqYQiR/qfVd9jE0ry2nk3LuRFAALAAABTqGUxOc+YFdb1E4Vomcnq2xuLmSiVDEhMex3EheEtHP0q4TX1s6QXrdhuRtvuHAzyqH/QW1
     *                            ZX/iOnDzjir07uYRuZHcKjb3RkDO6efKFGu5l8/q9oxKzYGn1pDlU1NlmVqiY/lA8IVFa24E5mQThXO+WlyAG1uGwwJPi7KJi+QPZGAVAV3q+D8odteMJFAyfKywuaiYY8RjkA
     *                            aL+icq+ru4JTID4h6sN7ZpNDQSPjUUUh4hXspjyyVReUJpWTT2Rw3+jEezYD3KRtg7QKJAfiHXTYiZdy8/b1otN2j90DJ5EczuFJeCr4p2hdzMp5kGtEBCUXgNiFgWfSxvSLqf
     *                            JXtLtEllCcmZBpyYMT5K+tn1TGg3vjAaWtvP7/h7L2qPK0qQdM9iK8YeYPsfbl17THiyF7443XAONbTB++wAAAAAAAAAAAAAAAAAAAAAAAAAA24Pw6KF3PCEWTBeYYDfN+K/Bu
     *                            9wbgVdyxtob77Gn+KPeFx81Xrla9Ab6EnynDSZ79o0DFfL5KK4pPukCxe+eF8H3v9rl8nDZ8TqpYguJd5Uda3WfETH+n5KJMX86Vu+hAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
     *                            AAAAAAAAAAAA=",
     *      "event_log"         : "PG1lYXN1cmVMb2c+PHR4dD48dHh0U3RhdHVzPjM8L3R4dFN0YXR1cz48bW9kdWxlcz48bW9kdWxlPjxwY3JCYW5rPlNIQTE8L3BjckJhbms+PHBjck51bWJlcj4xNzwvcGNyT
     *                            nVtYmVyPjxuYW1lPkhBU0hfU1RBUlQ8L25hbWU+PHZhbHVlPjJmYjdkNTdkY2M1NDU1YWY5YWMwOGQ4MmJkZjMxNWRiY2M1OWEwNDQ8L3ZhbHVlPjwvbW9kdWxlPjxtb2R1bGU
     *                            +PHBjckJhbms+U0hBMTwvcGNyQmFuaz48cGNyTnVtYmVyPjE3PC9wY3JOdW1iZXI+PG5hbWU+QklPU0FDX1JFR19EQVRBPC9uYW1lPjx2YWx1ZT5mZmIxODA2NDY1ZDJkZTFiN
     *                            zUzMWZkNWEyYTZlZmZhYWQ3YzVhMDQ3PC92YWx1ZT48L21vZHVsZT48bW9kdWxlPjxwY3JCYW5rPlNIQTE8L3BjckJhbms+PHBjck51bWJlcj4xNzwvcGNyTnVtYmVyPjxuYW1
     *                            lPkNQVV9TQ1JUTV9TVEFUPC9uYW1lPjx2YWx1ZT4zYzU4NTYwNGU4N2Y4NTU5NzM3MzFmZWE4M2UyMWZhYjkzOTJkMmZjPC92YWx1ZT48L21vZHVsZT48bW9kdWxlPjxwY3JCY
     *                            W5rPlNIQTE8L3BjckJhbms+PHBjck51bWJlcj4xNzwvcGNyTnVtYmVyPjxuYW1lPkxDUF9DT05UUk9MX0hBU0g8L25hbWU+PHZhbHVlPjkwNjljYTc4ZTc0NTBhMjg1MTczNDM
     *                            xYjNlNTJjNWMyNTI5OWU0NzM8L3ZhbHVlPjwvbW9kdWxlPjxtb2R1bGU+PHBjckJhbms+U0hBMTwvcGNyQmFuaz48cGNyTnVtYmVyPjE3PC9wY3JOdW1iZXI+PG5hbWU+TENQX
     *                            0RFVEFJTFNfSEFTSDwvbmFtZT48dmFsdWU+NWJhOTNjOWRiMGNmZjkzZjUyYjUyMWQ3NDIwZTQzZjZlZGEyNzg0ZjwvdmFsdWU+PC9tb2R1bGU+PG1vZHVsZT48cGNyQmFuaz5
     *                            TSEExPC9wY3JCYW5rPjxwY3JOdW1iZXI+MTc8L3Bjck51bWJlcj48bmFtZT5TVE1fSEFTSDwvbmFtZT48dmFsdWU+NWJhOTNjOWRiMGNmZjkzZjUyYjUyMWQ3NDIwZTQzZjZlZ
     *                            GEyNzg0ZjwvdmFsdWU+PC9tb2R1bGU+PG1vZHVsZT48cGNyQmFuaz5TSEExPC9wY3JCYW5rPjxwY3JOdW1iZXI+MTc8L3Bjck51bWJlcj48bmFtZT5PU1NJTklUREFUQV9DQVB
     *                            fSEFTSDwvbmFtZT48dmFsdWU+M2M1ODU2MDRlODdmODU1OTczNzMxZmVhODNlMjFmYWI5MzkyZDJmYzwvdmFsdWU+PC9tb2R1bGU+PG1vZHVsZT48cGNyQmFuaz5TSEExPC9wY
     *                            3JCYW5rPjxwY3JOdW1iZXI+MTc8L3Bjck51bWJlcj48bmFtZT5NTEVfSEFTSDwvbmFtasf8dmFsdWU+MWZlYjg1NmU5ZTQ2ZWQ3YjE5ZTkxMTUzMzJiYjZhMjVkYjA3ZDJhMjw
     *                            vdmFsdWU+PC9tb2R1bGU+PG1vZHVsZT48cGNyQmFuaz5TSEExPC9wY3JCYW5rPjxwY3JOdW1iZXI+MTc8.....bGVzPjwvdHh0PjwvbWVhc3VyZUxvZz4K",
     *      "selected_pcr_banks": [
     *                              "SHA1",
     *                               "SHA256"
     *                            ],
     *      "is_tag_provisioned": true,
     *      "asset_tag"         : "tHgfRQED1+pYgEZpq3dZC9ONmBCZKdx10LErTZs1k/k="
     *  }
     * </xmp></pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre><xmp>
     *   
     *   //Nonce should be randomly generated
     *   byte[] nonce = "tHgfRQED1+pYgEZpq3dZC9ONmBCZKdx10LErTZs1k/k=".getBytes();
     *   int[] pcrs = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23});
     *   TpmQuoteRequest tpmQuoteRequest = new TpmQuoteRequest();
     *   tpmQuoteRequest.setNonce(nonce);
     *   tpmQuoteRequest.setPcrs(pcrs);
     *   TrustAgentClient client = new TrustAgentClient(properties);
     *   TpmQuoteResponse tpmQuote = client.getTpmQuote(tpmQuoteRequest);
     * </xmp></pre></div>
     */
    
    public TpmQuoteResponse getTpmQuote(TpmQuoteRequest tpmQuoteRequest) {
        log.debug("target: {}", getTarget().getUri().toString());
        TpmQuoteResponse tpmQuoteResponse = getTarget()
                .path("/tpm/quote")
                .request()
                .accept(MediaType.APPLICATION_XML)
                .post(Entity.json(tpmQuoteRequest), TpmQuoteResponse.class);
        return tpmQuoteResponse;
    }
    
}
