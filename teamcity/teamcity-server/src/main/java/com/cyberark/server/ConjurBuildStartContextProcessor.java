package com.cyberark.server;

import com.cyberark.common.exceptions.ConjurApiAuthenticateException;
import com.cyberark.common.exceptions.MissingMandatoryParameterException;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import com.cyberark.common.*;

public class ConjurBuildStartContextProcessor implements BuildStartContextProcessor {

    // TODO: Currently when retrieving the connection type, we find the first connection that meets the `providerType`
    //   and then return that object. It is possible to define multiple connections. I think if multiples are defined
    //   an error should be returned. And only accept 1 connection per project for the time being.
    private SProjectFeatureDescriptor getConnectionType(SProject project, String providerType) {
        Iterator<SProjectFeatureDescriptor> it = project.getAvailableFeaturesOfType(OAuthConstants.FEATURE_TYPE).iterator();
        while(it.hasNext()) {
            SProjectFeatureDescriptor desc = it.next();
            String connectionType = desc.getParameters().get(OAuthConstants.OAUTH_TYPE_PARAM);

            if (connectionType.equals(providerType)) {
                // TODO: Some of these print statements should be logged via the Teamcity logger (If its possible)
                // System.out.printf("Found connection feature for TYPE '%s'\n", providerType);
                return desc;
            }
        }
        return null;
    }

    private BuildProblemData createBuildProblem(SBuild build, String message) {
        return BuildProblemData.createBuildProblem(build.getBuildNumber(), ConjurSettings.getFeatureType(), message);
    }

    @Override
    public void updateParameters(BuildStartContext context) {
        SRunningBuild build = context.getBuild();

        SBuildType buildType = build.getBuildType();
        if (buildType == null) {
            // It is possible of build type to be null, if this is the case lets return and not retrieve conjur secrets
            return;
        }

        SProject project = buildType.getProject();
        SProjectFeatureDescriptor connectionFeatures = getConnectionType(project, ConjurSettings.getFeatureType());
        if (connectionFeatures == null) {
            // If connection feature cannot be found (no connection has been configured on this project)
            // then return and do not perform conjur secret retrieval actions
            return;
        }

        ConjurConnectionParameters conjurConfig = new ConjurConnectionParameters(connectionFeatures.getParameters());

        System.out.println(conjurConfig.toString());

        try {
            for(Map.Entry<String, String> kv : conjurConfig.getAgentSharedParameters().entrySet()) {
                System.out.println(String.format("%s: %s", kv.getKey(), kv.getValue()));
                context.addSharedParameter(kv.getKey(), kv.getValue());
            }
        } catch (MissingMandatoryParameterException e) {
            BuildProblemData buildProblem = createBuildProblem(build,
                    String.format("ERROR: Setting agent's shared parameters. %s. %s",
                            e.getMessage(), conjurConfig.toString()));
            build.addBuildProblem(buildProblem);
        }
    }
}
