package com.github.pavradev.dockerbay;

import com.github.pavradev.dockerbay.exceptions.EnvironmentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Docker Container abstraction
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
    private String name;
    private Integer localPort;
    private Integer localDebugPort;
    private List<Bind> binds = new ArrayList<>();

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

    public void setName(String name){
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public void addBind(Bind bind) {
        binds.add(bind);
    }

    public List<Bind> getBinds() {
        return binds;
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

    public Integer getLocalDebugPort() {
        return this.localDebugPort;
    }

    public ContainerStatus getStatus() {
        return this.status;
    }

    public void setStatus(ContainerStatus status) {
        this.status = status;
    }

    public void create() {
        switch (getStatus()) {
            case NOT_CREATED:
                log.info("Pulling image {}", getImage());
                dockerClient.pullImage(getImage());
                log.info("Creating container {}", getName());
                setStatus(ContainerStatus.CREATED_OR_STOPPED);
                dockerClient.createContainer(this);
                break;
            case CREATED_OR_STOPPED:
            case RUNNING:
                log.info("Container {} already created or running", getName());
                break;
        }
    }

    public void start() {
        switch (getStatus()) {
            case CREATED_OR_STOPPED:
                setStatus(ContainerStatus.RUNNING);
                log.info("Starting container {}", getName());
                dockerClient.startContainer(this);
                assignLocalPortsIfNeeded();
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

    private void assignLocalPortsIfNeeded() {
        if (this.config.getExposedPort() != null || this.config.getDebugPort() != null) {
            Map<Integer, Integer> portMappings = dockerClient.getPortMappings(this);
            this.localPort = portMappings.get(this.config.getExposedPort());
            this.localDebugPort = portMappings.get(this.config.getDebugPort());
            if (this.localPort != null) {
                log.info("Container {} port is {}", getName(), getLocalPort());
            }
            if (this.localDebugPort != null) {
                log.info("Container {} debug port is {}", getName(), getLocalDebugPort());
            }
        }
    }

    public void stop() {
        switch (getStatus()) {
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
        switch (getStatus()) {
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

    public boolean isRunning() {
        return ContainerStatus.RUNNING.equals(this.status);
    }

    private void waitUntilReady() {
        waitForLogEntryIfNeeded();
        waitForUrlIfNeeded();
    }

    private void waitForUrlIfNeeded() {
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

    private void waitForLogEntryIfNeeded() {
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
