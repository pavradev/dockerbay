package com.github.pavradev.dockerbay;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import com.github.pavradev.dockerbay.exceptions.EnvironmentException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container implementation
 */
class Container {
    public enum ContainerStatus {NOT_CREATED, CREATED_OR_STOPPED, RUNNING}

    private static final Logger log = LoggerFactory.getLogger(Container.class);

    public static final Long BETWEEN_RETRY_MILLIS = 2000L;

    private Client httpClient;
    private DockerClientWrapper dockerClient;

    private ContainerStatus status = ContainerStatus.NOT_CREATED;
    private ContainerConfig config;
    private Network network;

    private Integer localPort;

    private Container(ContainerConfig config) {
        this.config = config;
    }

    public static Container withConfig(ContainerConfig config) {
        return new Container(config);
    }

    public void setHttpClient(Client httpClient) {
        this.httpClient = httpClient;
    }

    public void setDockerClient(DockerClientWrapper dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String getName() {
        if (this.network != null) {
            return this.config.getAlias() + "-" + this.network.getName();
        } else {
            return this.config.getAlias();
        }
    }

    public String getImage() {
        return this.config.getImage();
    }

    public String getAlias() {
        return this.config.getAlias();
    }

    public Network getNetwork() {
        return this.network;
    }

    public Integer getLocalPort() {
        return this.localPort;
    }

    public ContainerStatus getStatus() {
        return this.status;
    }

    public void setStatus(ContainerStatus status){
        this.status = status;
    }

    public void create() {
        switch (getStatus()){
            case NOT_CREATED:
                log.info("Pulling image {}", getImage());
                dockerClient.pullImage(getImage());
                log.info("Creating container {}", getName());
                setStatus(ContainerStatus.CREATED_OR_STOPPED);
                dockerClient.createContainer(this);
                break;
            case CREATED_OR_STOPPED:
            case RUNNING:
                log.info("Container {} already running", getName());
                break;
        }
    }

    public void start() {
        switch (getStatus()){
            case CREATED_OR_STOPPED:
                setStatus(ContainerStatus.RUNNING);
                log.info("Starting container {}", getName());
                dockerClient.startContainer(this);
                assignLocalPortIfNeeded();
                waitUntilReady();
                break;
            case NOT_CREATED:
                log.warn("No container {} found to start!", getName());
                break;
            case RUNNING:
                log.info("Container {} is already running", getName());
                break;
        }
    }

    private void assignLocalPortIfNeeded() {
        if (this.config.getExposedPort() != null) {
            Map<Integer, Integer> portMappings = dockerClient.getPortMappings(this);
            this.localPort = portMappings.get(this.config.getExposedPort());
        }
    }

    public void stop() {
        switch (getStatus()){
            case RUNNING:
                log.info("Stopping container {}", getName());
                dockerClient.stopContainer(this);
                setStatus(ContainerStatus.CREATED_OR_STOPPED);
                break;
            case CREATED_OR_STOPPED:
            case NOT_CREATED:
                log.info("Container {} is already stopped or removed. Ignoring.", getName());
        }
    }

    public void remove() {
        switch (getStatus()){
            case CREATED_OR_STOPPED:
                displayLogsIfNeeded();
                log.info("Removing container {}", getName());
                dockerClient.removeContainer(this);
                setStatus(ContainerStatus.NOT_CREATED);
                break;
            case NOT_CREATED:
                log.info("Container {} is already stopped", getName());
                break;
            case RUNNING:
                log.warn("Container {} is running. Stop first", getName());
                break;
        }
    }

    private void displayLogsIfNeeded() {
        if (this.config.getDisplayLogs()) {
            String logs = dockerClient.getContainerLogs(this);
            log.debug(logs);
        }
    }

    public void attachToNetwork(Network network) {
        this.network = network;
        network.addContainer(this);
    }

//    public void detachFromNetwork() {
//        network.removeContainer(this);
//        this.network = null;
//    }

    public Integer getExposedPort() {
        return this.config.getExposedPort();
    }

    public Map<String, String> getEnvVariables() {
        return this.config.getEnvVariables();
    }

    public List<String> getCmd() {
        return this.config.getCmd();
    }

    public boolean isRunning(){
        return ContainerStatus.RUNNING.equals(this.status);
    }

    private void waitUntilReady() {
        waitForLogEntry();
        waitForUrl();
    }

    private void waitForUrl() {
        if (this.config.getWaitForUrl() != null) {
            if (this.localPort == null) {
                throw new EnvironmentException("No allocated port for container" + getName());
            }
            final String target = "http://localhost:" + this.localPort;
            withTimeout(() -> {
                final Response response = httpClient.target(target).path(this.config.getWaitForUrl()).request().get();
                return Response.Status.Family.SUCCESSFUL.equals(Response.Status.Family.familyOf(response.getStatus()));
            }, this.config.getTimeoutSec());
        }
    }

    private void waitForLogEntry() {
        if (this.config.getWaitForLogEntry() != null) {
            withTimeout(() -> {
                String containerLogs = dockerClient.getContainerLogs(this);
                return containerLogs.contains(this.config.getWaitForLogEntry());
            }, this.config.getTimeoutSec());
        }
    }

    private void withTimeout(Supplier<Boolean> command, Integer timeoutSec) {
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

}
