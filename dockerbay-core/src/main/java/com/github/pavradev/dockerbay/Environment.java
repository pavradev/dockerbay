package com.github.pavradev.dockerbay;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import com.github.pavradev.dockerbay.exceptions.EnvironmentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstraction of the docker environment for component test.
 * Stateful.
 */
class Environment {
    private static final Logger log = LoggerFactory.getLogger(Environment.class);

    private static final int BETWEEN_RETRY_MILLIS = 2000;
    private Status status;

    public enum Status {UNINITIALIZED, INITIALIZED, PARTIALLY_INITIALIZED, CLEANED}

    private DockerClientWrapper dockerClient;
    private Client httpClient;

    private String networkName;

    private List<ContainerConfig> containers = new ArrayList<>();
    private Deque<String> startedContainers = new LinkedList<>();

    private Map<String, ContainerConfig> containerConfigMap = new HashMap<>();
    private Map<String, Integer> allocatedPortsPerContainer = new HashMap<>();

    private Environment(String id) {
        this.networkName = id;
        this.status = Status.UNINITIALIZED;
    }

    public static Environment withId(String id) {
        return new Environment(id);
    }

    public void setDockerClient(DockerClientWrapper dockerClient) {
        this.dockerClient = dockerClient;
    }

    public void setHttpClient(Client httpClient) {
        this.httpClient = httpClient;
    }

    public void setContainers(List<ContainerConfig> containers) {
        this.containers = new ArrayList<>(containers);
    }

    public Status getStatus() {
        return this.status;
    }

    private void setStatus(Status status) {
        this.status = status;
    }

    public Integer getAllocatedPort(String containerName) {
        return this.allocatedPortsPerContainer.get(containerName);
    }

    public String buildUniqueContainerName(String name) {
        return this.networkName + "-" + name;
    }

    public void initialize() {
        validateStatus(Status.UNINITIALIZED);
        try {
            pullImages();
            createNetwork();
            createAndStartContainers();
            setStatus(Status.INITIALIZED);
        } catch (Exception e) {
            log.error("Failed to initialize environment {}" + this.networkName, e);
            setStatus(Status.PARTIALLY_INITIALIZED);
        }
    }

    private void validateStatus(Status... expectedStatuses) {
        if (!Arrays.stream(expectedStatuses).anyMatch(this.status::equals)) {
            throw new EnvironmentException(String.format("Invalid environment status. Expected %s but was %s", expectedStatuses, this.status));
        }
    }

    private void pullImages() {
        Set<String> uniqueImages = containers.stream()
                .map(ContainerConfig::getImage)
                .collect(Collectors.toSet());
        for (String image : uniqueImages) {
            dockerClient.pullImage(image);
        }
    }

    private void createNetwork() {
        log.info("Creating network {}", networkName);
        dockerClient.createNetwork(this.networkName);
    }

    private void createAndStartContainers() {
        for (ContainerConfig container : this.containers) {
            createAndStartContainer(container);
            waitForUrlIfNeeded(container);
            waitForLogEntryIfNeeded(container);
        }
    }

    private void waitForLogEntryIfNeeded(ContainerConfig container) {
        String containerName = buildUniqueContainerName(container.getName());
        if (container.getWaitForLogEntry() != null) {
            doWithTimeout(() -> {
                String containerLogs = dockerClient.getContainerLogs(containerName);
                return containerLogs.contains(container.getWaitForLogEntry());
            }, container.getTimeoutSec());
        }
    }

    private void waitForUrlIfNeeded(ContainerConfig container) {
        if (container.getWaitForUrl() != null) {
            Integer port = getAllocatedPort(container.getName());
            if (port == null) {
                throw new EnvironmentException("No allocated port for container" + container.getName());
            }
            final String target = "http://localhost:" + port;
            doWithTimeout(() -> {
                final Response response = httpClient.target(target).path(container.getWaitForUrl()).request().get();
                return Response.Status.Family.SUCCESSFUL.equals(Response.Status.Family.familyOf(response.getStatus()));
            }, container.getTimeoutSec());
        }
    }

    private void createAndStartContainer(ContainerConfig container) {
        final Container createContainerRequest = getCreateContainerRequest(container);
        this.startedContainers.push(createContainerRequest.getName());
        this.containerConfigMap.put(createContainerRequest.getName(), container);
        dockerClient.createContainer(createContainerRequest);
        log.info("Starting container {}", createContainerRequest.getName());
        dockerClient.startContainer(createContainerRequest.getName());
        if (container.getExposedPort() != null) {
            Map<Integer, Integer> portMappings = dockerClient.getPortMappings(createContainerRequest.getName());
            Integer localPort = portMappings.get(container.getExposedPort());
            this.allocatedPortsPerContainer.put(container.getName(), localPort);
        }
    }

    private void doWithTimeout(Supplier<Boolean> command, Integer timeoutSec) {
        Duration timeout = Duration.ofSeconds(timeoutSec);
        final Instant start = Instant.now();
        while (true) {
            final Duration timeElapsed = Duration.between(start, Instant.now());
            if (timeout.minus(timeElapsed).isNegative()) {
                throw new EnvironmentException(String.format("Timeout exception"));
            }
            try {
                if (command.get()) {
                    return;
                }
            } catch (Exception e) {
                log.debug("Error when executing a command");
            }
            try {
                Thread.sleep(BETWEEN_RETRY_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private Container getCreateContainerRequest(ContainerConfig container) {
        Container.CreateContainerRequestBuilder containerCreateRequestBuilder = Container.builder();
        containerCreateRequestBuilder.withName(buildUniqueContainerName(container.getName()));
        containerCreateRequestBuilder.withAlias(container.getName());
        containerCreateRequestBuilder.fromImage(container.getImage());
        containerCreateRequestBuilder.inNetwork(this.networkName);
        List<String> links = this.containers.stream()
                .filter(c -> c.getName() != container.getName())
                .map(c -> String.format("%s:%s", buildUniqueContainerName(c.getName()), c.getName()))
                .collect(Collectors.toList());
        containerCreateRequestBuilder.withLinks(links);
        containerCreateRequestBuilder.withExposedPort(container.getExposedPort());

        containerCreateRequestBuilder.withCmd(container.getCmd());
        containerCreateRequestBuilder.withEnvVariables(container.getEnvVariables());
        return containerCreateRequestBuilder.build();
    }

    public void cleanup() {
        validateStatus(Status.INITIALIZED, Status.PARTIALLY_INITIALIZED);
        stopAndRemoveContainersQuietly();
        deleteNetworkQuietly();
        setStatus(Status.CLEANED);
    }

    private void stopAndRemoveContainersQuietly() {
        while (!startedContainers.isEmpty()) {
            String container = this.startedContainers.pop();
            stopAndRemoveContainerQuietly(container);
        }
    }

    private void stopAndRemoveContainerQuietly(String container) {
        if (this.containerConfigMap.get(container).getDisplayLogs()) {
            try {
                String logs = dockerClient.getContainerLogs(container);
                log.debug(logs);
            } catch (Exception e) {
                log.error(String.format("Failed to display logs for container %s in environment %s ", container, this.networkName), e);
            }
        }
        try {
            log.info("Stopping container {}", container);
            dockerClient.stopContainer(container);
        } catch (Exception e) {
            log.error(String.format("Failed to stop container %s in environment %s ", container, this.networkName), e);
        }
        try {
            dockerClient.removeContainer(container);
        } catch (Exception e) {
            log.error(String.format("Failed to remove container %s in environment %s ", container, this.networkName), e);
        }
    }

    private void deleteNetworkQuietly() {
        try {
            log.info("Delete network {}", networkName);
            dockerClient.deleteNetwork(this.networkName);
        } catch (Exception e) {
            log.error("Failed to delete network ", e);
        }
    }
}
