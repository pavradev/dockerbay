package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates all necessary information to start Docker container.
 * Environment specific.
 */
public class Container {
    private String alias;
    private String name;
    private String image;
    private String networkName;

    private List<String> cmd;
    private Map<String, String> envVariables = new HashMap<>();
    private List<String> links = new ArrayList<>();

    //so far only one port can be exposed
    private Integer exposedPort;

    private Container() {
    }

    public String getAlias() {
        return alias;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getNetworkName() {
        return networkName;
    }

    public List<String> getCmd() {
        return cmd;
    }

    public Map<String, String> getEnvVariables() {
        return envVariables;
    }

    public List<String> getLinks() {
        return links;
    }

    public Integer getExposedPort() {
        return exposedPort;
    }

    public static CreateContainerRequestBuilder builder() {
        return new CreateContainerRequestBuilder();
    }

    public static class CreateContainerRequestBuilder {
        private Container container = new Container();

        private CreateContainerRequestBuilder() {
        }

        public CreateContainerRequestBuilder withName(String name) {
            container.name = name;
            return this;
        }

        public CreateContainerRequestBuilder withAlias(String alias) {
            container.alias = alias;
            return this;
        }

        public CreateContainerRequestBuilder fromImage(String image) {
            container.image = image;
            return this;
        }

        public CreateContainerRequestBuilder inNetwork(String network) {
            container.networkName = network;
            return this;
        }

        public CreateContainerRequestBuilder withCmd(List<String> cmd) {
            if (cmd != null) {
                container.cmd = new ArrayList<>(cmd);
            }
            return this;
        }

        public CreateContainerRequestBuilder withEnvVariables(Map<String, String> envVariables) {
            HashMap<String, String> envVariablesCopy = new HashMap<>();
            for (Map.Entry<String, String> variable : envVariables.entrySet()) {
                envVariablesCopy.put(variable.getKey(), variable.getValue());
            }
            container.envVariables = envVariablesCopy;
            return this;
        }

        public CreateContainerRequestBuilder withLinks(List<String> links) {
            if (links != null) {
                container.links = new ArrayList<String>(links);
            }
            return this;
        }

        public CreateContainerRequestBuilder withExposedPort(Integer exposedPort) {
            container.exposedPort = exposedPort;
            return this;
        }

        Container build() {
            if (this.container.image == null) {
                throw new IllegalArgumentException("Container image cannot be empty");
            }
            if (this.container.name == null) {
                throw new IllegalArgumentException("Container name cannot be empty");
            }
            Container result = this.container;
            this.container = null;
            return result;
        }
    }
}
