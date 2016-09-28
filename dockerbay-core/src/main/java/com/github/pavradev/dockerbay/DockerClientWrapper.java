package com.github.pavradev.dockerbay;

import java.util.Map;

/**
 * Wrapper around a specific docker client implementation.
 */
public interface DockerClientWrapper {

    void createContainer(Container containerConfig);

    void startContainer(String containerName);

    void stopContainer(String containerName);

    void removeContainer(String containerName);

    Map<Integer, Integer> getPortMappings(String containerName);

    String getContainerLogs(String containerName);

    void createNetwork(String networkName);

    void deleteNetwork(String networkName);

    void pullImage(String imageName);
}
