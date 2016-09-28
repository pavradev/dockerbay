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
    public enum ContainerStatus {UNKNOWN, STARTED, STOPPED, REMOVED}

    private static final Logger log = LoggerFactory.getLogger(Container.class);

    public static final Long BETWEEN_RETRY_MILLIS = 2000L;

    private Client httpClient;
    private DockerClientWrapper dockerClient;

    private ContainerStatus status = ContainerStatus.UNKNOWN;
    private ContainerConfig config;
    private Network network;

    private Integer localPort;

    private Container(ContainerConfig config) {
        this.config = config;
    }

    public static Container wihtConfig(ContainerConfig config) {
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

    public void create() {
        dockerClient.pullImage(getImage());
        dockerClient.createContainer(this);
    }

    public void start() {
        this.status = ContainerStatus.STARTED;
        dockerClient.startContainer(this);
        if (this.config.getExposedPort() != null) {
            Map<Integer, Integer> portMappings = dockerClient.getPortMappings(this);
            this.localPort = portMappings.get(this.config.getExposedPort());
        }
        waitUntilReady();
    }

    public void stop() {
        this.status = ContainerStatus.STOPPED;
        if (ContainerStatus.STARTED.equals(this.status)) {
            dockerClient.stopContainer(this);
        }
    }

    public void remove() {
        if (this.config.getDisplayLogs()) {
            String logs = dockerClient.getContainerLogs(this);
            log.debug(logs);
        }
        this.status = ContainerStatus.REMOVED;
        this.detachFromNetwork();
        dockerClient.removeContainer(this);
    }

    public void attachToNetwork(Network network) {
        this.network = network;
        network.addContainer(this);
    }

    public void detachFromNetwork() {
        network.removeContainer(this);
        this.network = null;
    }

    public Integer getExposedPort() {
        return this.config.getExposedPort();
    }


    public Map<String, String> getEnvVariables() {
        return this.config.getEnvVariables();
    }

    public List<String> getCmd() {
        return this.config.getCmd();
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
