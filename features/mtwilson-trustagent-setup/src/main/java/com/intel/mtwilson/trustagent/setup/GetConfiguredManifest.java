/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.setup;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.mtwilson.common.ErrorCode;
import com.intel.mtwilson.common.TAException;
import com.intel.mtwilson.jaxrs2.client.MtWilsonClient;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import com.intel.mtwilson.core.common.utils.AASTokenFetcher;
import com.intel.mtwilson.core.common.utils.ManifestUtils;
import com.intel.wml.manifest.xml.Manifest;
import org.apache.commons.io.FileUtils;
import com.intel.mtwilson.Folders;
import java.io.File;
import java.net.URL;
import java.util.*;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;

import com.intel.mtwilson.core.common.model.SoftwareFlavorPrefix;

/**
 * @author arijitgh
 */
public class GetConfiguredManifest extends AbstractSetupTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GetConfiguredManifest.class);
    private final static String MANIFEST_FILE_BASE_PATH = Folders.application() + File.separator + "var" + File.separator;
    private static final String FLAVOR_UUIDS = "FLAVOR_UUIDS";
    private static final String FLAVOR_LABELS = "FLAVOR_LABELS";
    private static final String MANIFEST_RESOURCE = "manifests";
    private String url;
    private String username;
    private String password;
    private String aasApiUrl;
    private String flavorUuids;
    private List<String> flavorList = new ArrayList<>();
    private List<String> flavorUuidList = new ArrayList<>();
    private boolean validated = false;
    private boolean duplicateDefaultSoftwareFlavorExists = false;
    private TrustagentConfiguration trustagentConfiguration;

    @Override
    protected void configure() throws Exception {
        String flavorLabels;
        trustagentConfiguration = new TrustagentConfiguration(getConfiguration());
        url = trustagentConfiguration.getMtWilsonApiUrl();
        if (url == null || url.isEmpty()) {
            configuration("Verification service URL is not set");
        }
        username = trustagentConfiguration.getTrustAgentAdminUserName();
        if (username == null || username.isEmpty()) {
            configuration("TA admin username is not set");
        }
        password = trustagentConfiguration.getTrustAgentAdminPassword();
        if (password == null || password.isEmpty()) {
            configuration("TA admin password is not set");
        }
        aasApiUrl = trustagentConfiguration.getAasApiUrl();
        if (aasApiUrl == null || aasApiUrl.isEmpty()) {
            configuration("AAS API URL is not set");
        }
        flavorUuids = System.getenv(FLAVOR_UUIDS);
        if (flavorUuids != null && !flavorUuids.isEmpty()) {
            for (String uuid : flavorUuids.split("\\s*,\\s*")) {
                if (!flavorList.contains(uuid)) {
                    flavorList.add(uuid);
                }
            }
        } else {
            flavorLabels = System.getenv(FLAVOR_LABELS);
            if (flavorLabels == null || flavorLabels.isEmpty()) {
                configuration("Neither flavor UUID nor flavor label is set");
            } else {
                for (String label : flavorLabels.split(",")) {
                    if (!flavorList.contains(label)) {
                        flavorList.add(label);
                    }
                }
            }
        }
    }

    @Override
    protected void validate() throws Exception {
        if (!verifyPulledManifestsExist()) {
            validation("All manifests specified have not been successfully pulled");
        }
        if (!verifyDefaultManifestExists()) {
            validation("Manifest for default flavor is not present on the host ");
        } else if (duplicateDefaultSoftwareFlavorExists) {
                throw new TAException(ErrorCode.ERROR, "Default software manifests are part of the setup. No action needed to add them.");
        }

    }

    @Override
    protected void execute() throws Exception {
        Manifest pulledManifest;
        String flavorUuid;
        String manifestString;
        MtWilsonClient client;
        log.info("Getting flavor for the host");
        try {
            TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustagentConfiguration.getTrustagentTruststoreFile(),
                trustagentConfiguration.getTrustagentTruststorePassword()).build();

            TlsConnection tlsConnection = new TlsConnection(new URL(url), tlsPolicy);
            Properties clientConfiguration = new Properties();

            clientConfiguration.setProperty(TrustagentConfiguration.BEARER_TOKEN, new AASTokenFetcher().getAASToken(username, password, new TlsConnection(new URL(aasApiUrl), tlsPolicy)));
            client = new MtWilsonClient(clientConfiguration, tlsConnection);
        } catch (IOException exception) {
            log.error("Cannot create Verification Service client : {}", exception);
            throw new IOException("Cannot create Verification Service client" + exception);
        }
        for (String flavorDetails : flavorList) {
            //Get the manifest by pulling it from HVS
            if (flavorUuids != null && !flavorUuids.isEmpty()) {
                log.info("Pulling flavor for UUID: {}", flavorDetails);
                pulledManifest = getManifestByUuid(client, flavorDetails);
                flavorUuid = flavorDetails;
            } else {
                log.info("Pulling flavor for label: {}", flavorDetails);
                pulledManifest = getManifestByLabel(client, flavorDetails);
                flavorUuid = pulledManifest.getUuid();
                flavorUuidList.add(flavorUuid);
            }
            if (pulledManifest.getLabel().contains(SoftwareFlavorPrefix.DEFAULT_APPLICATION_FLAVOR_PREFIX.getValue())
                    || pulledManifest.getLabel().contains(SoftwareFlavorPrefix.DEFAULT_WORKLOAD_FLAVOR_PREFIX.getValue())){
                log.error("Default flavor's manifest is part of installation, no need to pull default flavor's manifest with UUID : {}", flavorUuid);
                continue;
            }
            try {
                manifestString = ManifestUtils.getManifestString(pulledManifest);
            } catch (JAXBException exception) {
                throw new RuntimeException("Cannot convert manifest XML to string : ", exception);
            }
            manifestString = manifestString.trim();
            try {
                writeToFile(new File(MANIFEST_FILE_BASE_PATH + File.separator + "manifest_" + flavorUuid + ".xml"), manifestString);
            } catch (IOException exception) {
                throw new IOException("Error writing manifest to file : " + exception);
            }
        }
    }

    private boolean verifyPulledManifestsExist() {
        for (String flavorUuid : flavorUuidList) {
            if (!new File(MANIFEST_FILE_BASE_PATH + File.separator + "manifest_" + flavorUuid + ".xml").exists()) {
                validated = false;
                break;
            } else {
                validated = true;
            }
        }
        return validated;
    }

    private boolean verifyDefaultManifestExists() throws IOException, XMLStreamException, JAXBException {
        boolean defaultSoftwareFlavorExists = false;
        File[] files = new File(MANIFEST_FILE_BASE_PATH + File.separator).listFiles();
        Set<String> defaultManifestLabel = new HashSet<>();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("manifest_")) {
                    String readManifest = FileUtils.readFileToString(file, "utf-8");
                    Manifest manifest = ManifestUtils.parseManifestXML(readManifest);
                    if (manifest.getLabel().contains(SoftwareFlavorPrefix.DEFAULT_APPLICATION_FLAVOR_PREFIX.getValue())
                            || manifest.getLabel().contains(SoftwareFlavorPrefix.DEFAULT_WORKLOAD_FLAVOR_PREFIX.getValue())) {
                        if (!defaultManifestLabel.add(manifest.getLabel())) {
                            duplicateDefaultSoftwareFlavorExists = true;
                            break;
                        } else {
                            defaultSoftwareFlavorExists = true;
                        }
                    }
                }
            }
        }
        return defaultSoftwareFlavorExists;
    }

    private Manifest getManifestByUuid(MtWilsonClient client, String flavorDetails) {
        Manifest pulledManifest;
        flavorUuidList.add(flavorDetails);
        pulledManifest = client.getTarget().path(MANIFEST_RESOURCE).queryParam("id", UUID.valueOf(flavorDetails)).request().accept(MediaType.APPLICATION_XML).get(Manifest.class);
        return pulledManifest;
    }

    private Manifest getManifestByLabel(MtWilsonClient client, String flavorDetails) {
        Manifest pulledManifest;
        pulledManifest = client.getTarget().path(MANIFEST_RESOURCE).queryParam("key", "label").queryParam("value", flavorDetails).request().accept(MediaType.APPLICATION_XML).get(Manifest.class);
        return pulledManifest;
    }

    private void writeToFile(File fileObject, String data) throws IOException {
        FileUtils.write(fileObject, data, "utf-8");
    }
}
