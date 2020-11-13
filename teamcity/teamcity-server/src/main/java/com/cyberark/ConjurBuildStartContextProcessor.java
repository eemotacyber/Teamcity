package com.cyberark;

import com.cyberark.conjur.api.Conjur;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.SRunningBuild;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class ConjurBuildStartContextProcessor implements BuildStartContextProcessor {

    public ByteArrayInputStream getInputStreamFromString(String input) throws IOException {
        return new ByteArrayInputStream(input.getBytes());
    }

    // TODO: I think this method works.
    private SSLContext getSSLContext(String certContents) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(getInputStreamFromString(certContents));

        final KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null);
        ks.setCertificateEntry("conjurTlsCaPath", cert);
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext conjurSSLContext = SSLContext.getInstance("TLS");
        conjurSSLContext.init(null, tmf.getTrustManagers(), null);
        return conjurSSLContext;
    }

    // This method will turn a map of SOMETHING = %conjur:some/secret% into
    // SOMETHING = some/secret
    // input == {
    //   "env.SECRET": "%conjur:super/secret%",
    //   "env.DB_PASS": "%conjur:db/mysql/username%",
    //   "TEAMCITY_BUILD": "22"
    // }
    //
    // All non-conjur variables should not be returned
    // Also the %conjur: and % should be removed from the value
    // The key should remain the exact same
    // output == {
    //   "env.SECRET": "super/secret",
    //   "env.DB_PASS": "db/mysql/username"
    // }
    private Map<String, String> getVariableIdsFromBuildParameters(Map<String, String> parameters) {
        Map<String, String> variableIds = Collections.<String, String>emptyMap();

        for (Map.Entry<String, String> kv : parameters.entrySet() ) {
            String variableIdPrefix = "%conjur:";
            String variableIdSuffix = "%";

            if (kv.getValue().startsWith(variableIdPrefix) && kv.getValue().endsWith(variableIdSuffix)) {
                // This value represents that this parameter needs to be replaced
                String id = kv.getValue().trim();
                id = id.substring(variableIdPrefix.length());
                id = id.substring(0, id.length()-variableIdSuffix.length());

                variableIds.put(kv.getKey(), id);
            }
        }

        return variableIds;
    }

    @Override
    public void updateParameters(BuildStartContext context) {
        // TODO: For now we are going to implement all the logic on the Teamcity server rather than the agent
        // This means that this method will retrieve the secrets and then set them for the actual agent
        // However when we implement secret retrieval on the agent we will need to get the `Connection` info
        // And pass that to the agent
        // the agent will then use that `Connection` info to establish a connection to the Conjur REST API and
        // retrieve the secrets on the agent
        // This will allow the ability to put CIDR restrictions on an API key so it can only run on specific
        // Teamcity agents.
        SRunningBuild build = context.getBuild();
        Iterator<SProjectFeatureDescriptor> it = build.getBuildType().getProject().getAvailableFeaturesOfType(OAuthConstants.FEATURE_TYPE).iterator();
        SProjectFeatureDescriptor connectionFeatures = null;

        System.out.println("Starting to look at Feature descriptions for connection");
        while(it.hasNext()) {
            SProjectFeatureDescriptor desc = it.next();
            String providerType = desc.getParameters().get(OAuthConstants.OAUTH_TYPE_PARAM);

            // TODO: "Connection" should probably not be hardedcoded. Also this connection
            // is different in the hashi implemention. Seems like it should be like `conjur-connection` or something
            if (providerType.equals("Connection")) {
                // TODO: Some of these print statements should be logged via the Teamcity logger (If its possible)
                System.out.printf("Found connection feature for TYPE '%s'\n", providerType);
                connectionFeatures = desc;
                break;
            }
        }

        // TODO: This should be done through a class (This logic will have to be included on the agent at some point)
        ConjurJspKey conjurKeys = new ConjurJspKey();
        String account = connectionFeatures.getParameters().get(conjurKeys.getAccount());
        String apiKey = connectionFeatures.getParameters().get(conjurKeys.getApiKey());
        String authnLogin = connectionFeatures.getParameters().get(conjurKeys.getAuthnLogin());
        String applianceUrl = connectionFeatures.getParameters().get(conjurKeys.getApplianceUrl());
        String certFile = connectionFeatures.getParameters().get(conjurKeys.getCertFile());
        String failOnError = connectionFeatures.getParameters().get(conjurKeys.getFailOnError());

        System.setProperty("CONJUR_ACCOUNT", account);
        System.setProperty("CONJUR_APPLIANCE_URL", applianceUrl);


//        Map<String, String> params = context.getSharedParameters();
//        System.out.println("Starting to List parameters");
//        for (Map.Entry<String, String> kv : params.entrySet()) {
//            System.out.printf("Context Parameter: %s = %s \n", kv.getKey(), kv.getValue());
//        }

        Map<String, String> buildParams = context.getBuild().getBuildOwnParameters();
//        for(Map.Entry<String, String> kv : buildParams.entrySet()) {
//            System.out.printf("Build Parameter: %s = %s \n", kv.getKey(), kv.getValue());
//        }

        Map<String, String> conjurVariables = getVariableIdsFromBuildParameters(buildParams);

        try {
            Conjur conjur = new Conjur(authnLogin, apiKey, getSSLContext(certFile));

            for(Map.Entry<String, String> kv : conjurVariables.entrySet()) {
                String value = conjur.variables().retrieveSecret(kv.getValue());
                // TODO: I think this will work? I do not know if I need to create another map or do a different way of replacing the value
                kv.setValue(value);
            }

            System.out.println("THIS IS THE VALUE: " + value);
        } catch (Exception e) {
            // TODO: Gotta figure out how to make this look prettier
            // I think it is okay to catch all exceptions here, as long as we can forward the exception
            // To some type of log messages and to the build.

            // Map create an exception that wraps all of these exceptions called something like
            // ConjurBuildStartUpdateParametersException, just make sure we can include an inner exception
            e.printStackTrace();
            System.out.println("AN ERROR HAS OCCURED: " + e.toString());
            return;
        }

        // If we make it here `conjurVariables` Map<String, String> should contain the parameters names and the actual values.
        for(Map.Entry<String, String> kv : conjurVariables.entrySet()) {
            context.addSharedParameter(kv.getKey(), kv.getValue());
        }
    }
}
