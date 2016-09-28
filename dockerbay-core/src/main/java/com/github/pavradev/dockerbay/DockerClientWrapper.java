package com.github.pavradev.dockerbay;

import java.util.Map;

/**
 * Wrapper around a specific docker client implementation.
 */
public interface DockerClientWrapper {

    void createContainer(Container container);

    void startContainer(Container container);

    void stopContainer(Container container);

    void removeContainer(Container container);

    Map<Integer, Integer> getPortMappings(Container container);

    String getContainerLogs(Container container);

    void createNetwork(Network network);

    void deleteNetwork(Network network);

    void pullImage(String imageName);
}
