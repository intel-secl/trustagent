/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.intel.dcsg.cpg.crypto.SimpleKeystore;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.mtwilson.i18n.HostState;
import com.intel.mtwilson.jaxrs2.mediatype.CryptoMediaType;
import com.intel.mtwilson.launcher.ws.ext.V2;
import com.intel.mtwilson.trustagent.TrustagentConfiguration;
import com.intel.mtwilson.trustagent.TrustagentRepository;
import com.intel.mtwilson.trustagent.TrustagentWebApplicationException;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * This was previously called create_identity
 *
 * @author jbuhacoff
 */
@V2
@Path("/aik")
public class Aik {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Aik.class);
    private static X509Certificate identity = null;
    private static X509Certificate identityIssuer = null;

    protected TrustagentConfiguration getConfiguration() throws IOException {
        return TrustagentConfiguration.loadConfiguration();
    }

    @GET
    @Produces({CryptoMediaType.APPLICATION_PKIX_CERT, CryptoMediaType.APPLICATION_X_PEM_FILE})
    public X509Certificate getIdentity() throws IOException, CertificateException {
        if (identity == null) {
            TrustagentConfiguration configuration = getConfiguration();
            if (configuration.isDaaEnabled()) {
                log.debug("daa is currently not supported");
                return null;
            } else {
                TrustagentRepository repository = new TrustagentRepository(configuration);
                X509Certificate aikCertificate = repository.getAikCertificate();
                if (aikCertificate == null) {
                    throw new TrustagentWebApplicationException(HostState.AIK_NOT_PROVISIONED, 500);
                }
                identity = aikCertificate;
            }
        }
        return identity;
    }

    @GET
    @Path("/ca")
    @Produces({CryptoMediaType.APPLICATION_PKIX_CERT, CryptoMediaType.APPLICATION_X_PEM_FILE})
    public X509Certificate getIdentityCA() throws IOException {
        if (identityIssuer == null) {
            TrustagentConfiguration configuration = getConfiguration();
            File truststoreFile = configuration.getTrustagentTruststoreFile();
            if (!truststoreFile.exists()) {
                log.error("Missing truststore file: {}", truststoreFile.getAbsolutePath());
                throw new WebApplicationException(Response.serverError().header("Error", "Missing CA keystore file").build());
            }
            try {
                SimpleKeystore keystore = new SimpleKeystore(new FileResource(truststoreFile), configuration.getTrustagentTruststorePassword());
                X509Certificate privacyCACertificate = keystore.getX509Certificate("privacy", SimpleKeystore.CA);
                identityIssuer = privacyCACertificate;
            } catch (KeyManagementException | NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException | CertificateEncodingException e) {
                log.error("Unable to load Privacy CA certificate from keystore file");
                log.debug("Unable to load Privacy CA certificate from keystore file", e);
                throw new WebApplicationException(Response.serverError().header("Error", "Cannot load Privacy CA certificate file").build());
            }
        }
        return identityIssuer;
    }
}
