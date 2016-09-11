package com.github.pavradev.dockerbay;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * Factory to produce Environment
 */
public class EnvironmentFactory {

    private Client httpClient;

    private DockerClientWrapper dockerClient;

    private EnvironmentFactory(DockerClientWrapper dockerClientWrapper){
        this(dockerClientWrapper,  ClientBuilder.newClient());
    }

    private EnvironmentFactory(DockerClientWrapper dockerClientWrapper, Client client){
        this.dockerClient = dockerClientWrapper;
        this.httpClient = client;
    }

    public static EnvironmentFactory withDockerClientWrapper(DockerClientWrapper dockerClientWrapper){
        return new EnvironmentFactory(dockerClientWrapper);
    }

    public static EnvironmentFactory withDockerClientWrapperAndHttpClient(DockerClientWrapper dockerClientWrapper, Client httpClient){
        return new EnvironmentFactory(dockerClientWrapper, httpClient);
    }

    public Environment getWithId(String id) {
        Environment environment = new Environment(dockerClient, httpClient);
        environment.setNetworkName(id);
        return environment;
    }
}
