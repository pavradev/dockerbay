package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.pavradev.dockerbay.exceptions.EnvironmentException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstraction of the unique environment for component test. Stateful.
 */
public class Environment {
    private static final Logger log = LoggerFactory.getLogger(Environment.class);

    public enum EnvironmentState {UNINITIALIZED, INITIALIZED, PARTIALLY_INITIALIZED}

    private EnvironmentState state;
    private Network network;
    private List<Container> containers = new ArrayList<>();

    private Environment(Network network) {
        this.state = EnvironmentState.UNINITIALIZED;
        this.network = network;
    }

    public static Environment withNetwork(Network network) {
        return new Environment(network);
    }

    public void addContainer(Container container) {
        this.containers.add(container);
        container.attachToNetwork(network);
    }

    public EnvironmentState getState() {
        return this.state;
    }

    public List<Container> getContainers() {
        return this.containers;
    }

    public Optional<Container> findContainerByAlias(String alias){
        return this.containers.stream().filter(c -> c.getAlias().equals(alias)).findAny();
    }

    public void initialize() {
        validateState(EnvironmentState.UNINITIALIZED);
        try {
            network.create();
            containers.forEach(Container::create);
            containers.forEach(Container::start);
            setState(EnvironmentState.INITIALIZED);
        } catch (Exception e) {
            log.error("Failed to initialize environment", e);
            setState(EnvironmentState.PARTIALLY_INITIALIZED);
        }
    }

    private void setState(EnvironmentState state) {
        this.state = state;
    }

    private void validateState(EnvironmentState... expectedStates) {
        if (!Arrays.stream(expectedStates).anyMatch(this.state::equals)) {
            throw new EnvironmentException(String.format("Invalid environment state. Expected %s but was %s", expectedStates, this.state));
        }
    }

    public void cleanup() {
        validateState(EnvironmentState.INITIALIZED, EnvironmentState.PARTIALLY_INITIALIZED);
        stopAndRemoveContainersQuietly(this.containers);
        deleteNetworkQuietly(this.network);
        setState(EnvironmentState.UNINITIALIZED);
    }

    private void stopAndRemoveContainersQuietly(List<Container> containers) {
        List<Container> reversed = new ArrayList<>(this.containers);
        Collections.reverse(reversed);
        reversed.forEach(this::stopAndRemoveContainerQuietly);
    }

    private void stopAndRemoveContainerQuietly(Container container) {
        try {
            log.info("Stopping container {}", container.getName());
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
            log.info("Delete network {}", network.getName());
            network.delete();
        } catch (Exception e) {
            log.error("Failed to delete network " + network.getName(), e);
        }
    }
}
