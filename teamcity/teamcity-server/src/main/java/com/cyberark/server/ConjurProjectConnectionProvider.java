package com.cyberark;

import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import java.util.Map;

import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;


public class ConjurProjectConnectionProvider extends OAuthProvider {
	final private PluginDescriptor descriptor;

    public ConjurProjectConnectionProvider(PluginDescriptor descriptor) {
        this.descriptor = descriptor;
	}

	@Override
	@NotNull
	public String getDisplayName() {
		return "Cyberark Conjur";
	}

	@Override
	@NotNull
	public String getType() { return "Connection"; }

	@Override
	public String getEditParametersUrl() {
		return this.descriptor.getPluginResourcesPath("conjurconnection.jsp");
	}

	@Override
	@NotNull
	public String describeConnection(OAuthConnectionDescriptor connection) {
    	ConjurJspKey keys = new ConjurJspKey();
    	Map<String, String> params = connection.getParameters();

    	String applianceUrl = params.get(keys.getApplianceUrl());
    	String authnLogin = params.get(keys.getAuthnLogin());

		return String.format("Connection to Cyberark Conjur server at '%s' with login '%s'",
				applianceUrl, authnLogin );
	}
}