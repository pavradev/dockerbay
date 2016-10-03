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

    boolean isContainerExist(Container container);

    Map<Integer, Integer> getPortMappings(Container container);

    String getContainerLogs(Container container);

    boolean isNetworkExists(Network network);

    void createNetwork(Network network);

    void deleteNetwork(Network network);

    void pullImage(String imageName);

    void createVolume(Volume volume);

    void deleteVolume(Volume volume);
}
