package com.github.pavradev.dockerbay;

import javax.ws.rs.client.ClientBuilder;

/**
 * Facade class for dockerbay library
 */
public class Dockerbay {

    public static DockerRule getRuleWithContainers(ContainerConfig ... containerConfigs) {
        EnvironmentFactory environmentFactory = EnvironmentFactory.get()
                .withDockerClient(new DockerClientImpl())
                .withHttpClient(ClientBuilder.newClient())
                .withContainers(containerConfigs);
        return DockerRule.withEnvironmentFactory(environmentFactory);
    }
}