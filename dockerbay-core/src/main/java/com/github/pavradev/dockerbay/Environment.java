package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstraction of the unique environment for component test. Stateful.
 * Orchestrates underlying components: network, containers, volumes
 */
public class Environment {
    private static final Logger log = LoggerFactory.getLogger(Environment.class);

    private Network network;
    private List<Container> containers = new ArrayList<>();

    private Environment(Network network) {
        this.network = network;
    }

    public static Environment withNetwork(Network network) {
        return new Environment(network);
    }

    public void addContainer(Container container) {
        this.containers.add(container);
        container.attachToNetwork(network);
    }

    public boolean isInitialized() {
        return this.containers.stream().allMatch(Container::isRunning);
    }

    public List<Container> getContainers() {
        return this.containers;
    }

    public Optional<Container> findContainerByAlias(String alias){
        return this.containers.stream().filter(c -> c.getAlias().equals(alias)).findAny();
    }

    public Integer getAllocatedPortByAlias(String alias){
        return findContainerByAlias(alias).map(Container::getLocalPort).orElse(null);
    }

    public void tryCleanupFromPreviousRun(){
        if(network.isExist()){
            log.info("Network {} already exist. Will try to cleanup", this.network.getName());
            this.containers.forEach(c -> c.setStatus(Container.ContainerStatus.RUNNING));//assume that all containers running
            tearDown();
        }
    }

    public void initialize() {
        try {
            network.create();
            containers.forEach(Container::create);
            containers.forEach(Container::start);
        } catch (Exception e) {
            log.error("Failed to initialize environment", e);
        }
    }

    public void tearDown() {
        stopAndRemoveContainersQuietly(this.containers);
        deleteNetworkQuietly(this.network);
    }

    private void stopAndRemoveContainersQuietly(List<Container> containers) {
        List<Container> reversed = new ArrayList<>(containers);
        Collections.reverse(reversed);
        reversed.forEach(this::stopAndRemoveContainerQuietly);
    }

    private void stopAndRemoveContainerQuietly(Container container) {
        try {
            container.stop();
        } catch (Exception e) {
            log.error("Failed to stop container ", container.getName(), e);
        }
        try {
            container.remove();
        } catch (Exception e) {
            log.error("Failed to remove container ", container.getName(), e);
        }
    }

    private void deleteNetworkQuietly(Network network) {
        try {
            network.delete();
        } catch (Exception e) {
            log.error("Failed to delete network " + network.getName(), e);
        }
    }
}
