package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates all necessary information to start Docker container.
 * Environment specific.
 */
public class CreateContainerRequest {
    private String alias;
    private String name;
    private String image;
    private String networkName;

    private List<String> cmd;
    private Map<String, String> envVariables = new HashMap<>();
    private List<String> links = new ArrayList<>();

    //so far only one port can be exposed
    private Integer exposedPort;

    private CreateContainerRequest() {
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
        private CreateContainerRequest createContainerRequest = new CreateContainerRequest();

        private CreateContainerRequestBuilder() {
        }

        public CreateContainerRequestBuilder withName(String name) {
            createContainerRequest.name = name;
            return this;
        }

        public CreateContainerRequestBuilder withAlias(String alias) {
            createContainerRequest.alias = alias;
            return this;
        }

        public CreateContainerRequestBuilder fromImage(String image) {
            createContainerRequest.image = image;
            return this;
        }

        public CreateContainerRequestBuilder inNetwork(String network) {
            createContainerRequest.networkName = network;
            return this;
        }

        public CreateContainerRequestBuilder withCmd(List<String> cmd) {
            if (cmd != null) {
                createContainerRequest.cmd = new ArrayList<>(cmd);
            }
            return this;
        }

        public CreateContainerRequestBuilder withEnvVariables(Map<String, String> envVariables) {
            HashMap<String, String> envVariablesCopy = new HashMap<>();
            for (Map.Entry<String, String> variable : envVariables.entrySet()) {
                envVariablesCopy.put(variable.getKey(), variable.getValue());
            }
            createContainerRequest.envVariables = envVariablesCopy;
            return this;
        }

        public CreateContainerRequestBuilder withLinks(List<String> links) {
            if (links != null) {
                createContainerRequest.links = new ArrayList<String>(links);
            }
            return this;
        }

        public CreateContainerRequestBuilder withExposedPort(Integer exposedPort) {
            createContainerRequest.exposedPort = exposedPort;
            return this;
        }

        CreateContainerRequest build() {
            if (this.createContainerRequest.image == null) {
                throw new IllegalArgumentException("Container image cannot be empty");
            }
            if (this.createContainerRequest.name == null) {
                throw new IllegalArgumentException("Container name cannot be empty");
            }
            CreateContainerRequest result = this.createContainerRequest;
            this.createContainerRequest = null;
            return result;
        }
    }
}
