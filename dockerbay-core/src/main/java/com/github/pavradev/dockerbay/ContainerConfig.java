package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container configuration that is environment independent
 */
public class ContainerConfig {
    public static final int DEFAULT_TIMEOUT_SEC = 60;

    private String alias;
    private String image;

    private List<String> cmd;
    private Integer exposedPort;
    private Integer debugPort;
    private Map<String, String> envVariables = new HashMap<>();
    private List<Bind> binds = new ArrayList<>();
    private List<Bind> sharedBinds = new ArrayList<>();

    private Boolean displayLogs = false;
    private String waitForLogEntry;
    private String waitForUrl;
    private Integer timeoutSec = DEFAULT_TIMEOUT_SEC;

    private ContainerConfig() {
    }

    public String getAlias() {
        return alias;
    }

    public String getImage() {
        return image;
    }

    public List<String> getCmd() {
        return cmd;
    }

    public Integer getExposedPort() {
        return exposedPort;
    }

    public Integer getDebugPort() {
        return debugPort;
    }

    public Map<String, String> getEnvVariables() {
        return envVariables;
    }

    public List<Bind> getBinds(){
        return binds;
    }

    public List<Bind> getSharedBinds() {
        return sharedBinds;
    }

    public Boolean getDisplayLogs() {
        return displayLogs;
    }

    public String getWaitForLogEntry() {
        return waitForLogEntry;
    }

    public String getWaitForUrl() {
        return waitForUrl;
    }

    public Integer getTimeoutSec() {
        return timeoutSec;
    }

    public static ContainerConfigBuilder builder() {
        return new ContainerConfigBuilder();
    }

    public static class ContainerConfigBuilder {
        private ContainerConfig container;

        private ContainerConfigBuilder() {
            container = new ContainerConfig();
        }

        public ContainerConfigBuilder withAlias(String alias) {
            container.alias = alias;
            return this;
        }

        public ContainerConfigBuilder withImage(String image) {
            container.image = image;
            return this;
        }

        public ContainerConfigBuilder withCmd(List<String> cmd) {
            container.cmd = cmd;
            return this;
        }

        public ContainerConfigBuilder withExposedTcpPort(Integer port) {
            container.exposedPort = port;
            return this;
        }

        public ContainerConfigBuilder withDebugPort(Integer port) {
            container.debugPort = port;
            return this;
        }

        public ContainerConfigBuilder addToEnv(String param, String value) {
            container.envVariables.put(param, value);
            return this;
        }

        public ContainerConfigBuilder addBind(Bind bind){
            container.binds.add(bind);
            return this;
        }

        public ContainerConfigBuilder addSharedBind(Bind bind){
            container.sharedBinds.add(bind);
            return this;
        }

        public ContainerConfigBuilder waitForUrl(String url) {
            container.waitForUrl = url;
            return this;
        }

        public ContainerConfigBuilder waitForLogEntry(String logEntry) {
            container.waitForLogEntry = logEntry;
            return this;
        }

        public ContainerConfigBuilder waitTimeoutSec(Integer timeoutSec) {
            container.timeoutSec = timeoutSec;
            return this;
        }

        public ContainerConfigBuilder displayLogs(Boolean displayLogs) {
            container.displayLogs = displayLogs;
            return this;
        }

        public ContainerConfig build() {
            if (container.image == null) {
                throw new IllegalArgumentException("Container image cannot be empty");
            }
            if (container.alias == null) {
                throw new IllegalArgumentException("Container alias cannot be empty");
            }
            if (container.waitForUrl != null && container.exposedPort == null) {
                throw new IllegalArgumentException("You cannot wait for URL without exposing a port");
            }
            return container;
        }

    }
}
