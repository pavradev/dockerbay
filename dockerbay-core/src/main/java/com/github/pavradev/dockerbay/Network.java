package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Docker network abstraction
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
//        if (!this.containers.isEmpty()) {
//            throw new IllegalStateException("Cannot delete network " + getName() + ". It has attached containers.");
//        }
        log.info("Delete network {}", getName());
        dockerClient.deleteNetwork(this);
    }

    public boolean isExist() {
        return dockerClient.isNetworkExists(this);
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

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){
            return true;
        }
        if(!(obj instanceof Network)){
            return false;
        }
        return Objects.equals(((Network)obj).getName(), this.getName());
    }
}
