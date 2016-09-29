package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
class Network {
    private static final Logger log = LoggerFactory.getLogger(Network.class);

    private DockerClientWrapper dockerClient;

    private String name;
    private List<Container> containers = new ArrayList<>();

    private Network(String name) {
        this.name = name;
    }

    public static Network withName(String name){
        return new Network(name);
    }

    public void setDockerClient(DockerClientWrapper dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String getName() {
        return this.name;
    }

    public void create() {
        log.info("Creating network {}", getName());
        dockerClient.createNetwork(this);
    }

    public void delete() {
        if (!this.containers.isEmpty()) {
            throw new IllegalStateException("Cannot delete network " + getName() + ". It has attached containers.");
        }
        log.info("Delete network {}", getName());
        dockerClient.deleteNetwork(this);
    }

    public void addContainer(Container container) {
        this.containers.add(container);
    }

    public void removeContainer(Container container) {
        this.containers.remove(container);
    }

    public List<Container> getContainers() {
        return this.containers;
    }
}
