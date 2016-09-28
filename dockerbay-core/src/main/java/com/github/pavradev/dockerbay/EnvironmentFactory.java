package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.Client;

/**
 * Creates instances of Environment
 */
public class EnvironmentFactory {
    private DockerClientWrapper dockerClient;
    private Client httpClient;

    private List<ContainerConfig> containerConfigList = new ArrayList<>();

    private EnvironmentFactory() {
    }

    public static EnvironmentFactory get() {
        return new EnvironmentFactory();
    }

    public EnvironmentFactory withDockerClient(DockerClientWrapper dockerClient) {
        this.dockerClient = dockerClient;
        return this;
    }

    public EnvironmentFactory withHttpClient(Client httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public EnvironmentFactory withContainers(ContainerConfig... containers) {
        this.containerConfigList = Arrays.asList(containers);
        return this;
    }

    public Environment makeEnvironment(String id) {
        Network network = Network.withName(id);
        network.setDockerClient(dockerClient);

        Environment environment = Environment.withNetwork(network);

        for (ContainerConfig containerConfig : this.containerConfigList) {
            Container container = Container.wihtConfig(containerConfig);
            container.setDockerClient(dockerClient);
            container.setHttpClient(httpClient);
            environment.addContainer(container);
        }

        return environment;
    }
}
