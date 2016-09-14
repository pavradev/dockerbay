package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Links;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;

import com.github.pavradev.dockerbay.exceptions.DockerClientWrapperException;

/**
 * Docker client implementation based on Java DockerClient https://github.com/docker-java/docker-java
 */
public class DockerClientImpl implements DockerClientWrapper {

    private DockerClient dockerClient = DockerClientBuilder.getInstance().build();

    /**
     * If you need to configure DockerClient programmatically
     */
    public void setDockerClient(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void createContainer(CreateContainerRequest createContainerRequest) {
        try {
            HostConfig hostConfig = new HostConfig()
                    .withNetworkMode(createContainerRequest.getNetworkName());

            if (!createContainerRequest.getLinks().isEmpty()) {
                List<Link> links = createContainerRequest.getLinks().stream().map(l -> {
                    String[] link = l.split(":");
                    return new Link(link[0], link[1]);
                }).collect(Collectors.toList());
                hostConfig.withLinks(new Links(links));
            }
            if (createContainerRequest.getExposedPort() != null) {
                PortBinding portBinding = new PortBinding(
                        Ports.Binding.bindIp("0.0.0.0"),
                        ExposedPort.tcp(createContainerRequest.getExposedPort()));
                hostConfig.withPortBindings(new Ports(portBinding));
            }

            List<String> env = new ArrayList<>();
            createContainerRequest.getEnvVariables().entrySet().forEach(e -> {
                env.add(e.getKey());
                env.add(e.getValue());
            });

            CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(createContainerRequest.getImage())
                    .withName(createContainerRequest.getName())
                    .withHostConfig(hostConfig)
                    .withEnv(env);

            if (createContainerRequest.getCmd() != null) {
                createContainerCmd.withCmd(createContainerRequest.getCmd());
            }
            if (createContainerRequest.getExposedPort() != null) {
                createContainerCmd.withExposedPorts(ExposedPort.tcp(createContainerRequest.getExposedPort()));
            }

            createContainerCmd.exec();

        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to create container " + createContainerRequest.getName(), e);
        }
    }

    @Override
    public void startContainer(String containerName) {
        try {
            dockerClient.startContainerCmd(containerName).exec();
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to start container " + containerName, e);
        }
    }

    @Override
    public void stopContainer(String containerName) {
        try {
            dockerClient.killContainerCmd(containerName).exec(); //Killing is faster and more reliable
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to stop container " + containerName, e);
        }
    }

    @Override
    public void removeContainer(String containerName) {
        try {
            dockerClient.removeContainerCmd(containerName).exec();
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to remove container " + containerName, e);
        }
    }

    @Override
    public Map<Integer, Integer> getPortMappings(String containerName) {
        try {
            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerName).exec();
            Ports ports = containerInfo.getNetworkSettings().getPorts();
            Map<Integer, Integer> result = new HashMap<>();
            ports.getBindings().entrySet().forEach(e -> {
                if (e.getValue() != null) {
                    Integer containerPort = e.getKey().getPort();
                    Integer localPort = Integer.parseInt(e.getValue()[0].getHostPortSpec());
                    result.put(containerPort, localPort);
                }
            });
            return result;
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to inspect container " + containerName, e);
        }
    }

    @Override
    public String getContainerLogs(String containerName) {
        try {
            LogContainerResultCallback loggingCallback = new LogContainerResultCallback() {
                private final StringBuffer log = new StringBuffer();

                @Override
                public void onNext(Frame frame) {
                    log.append(new String(frame.getPayload()));
                }

                @Override
                public String toString() {
                    return log.toString();
                }
            };

            dockerClient.logContainerCmd(containerName)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(loggingCallback);
            loggingCallback.awaitCompletion();
            loggingCallback.close();
            return loggingCallback.toString();

        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to read container logs from " + containerName, e);
        }
    }

    @Override
    public void createNetwork(String networkName) {
        try {
            dockerClient.createNetworkCmd()
                    .withDriver("bridge")
                    .withName(networkName)
                    .withCheckDuplicate(true)
                    .exec();
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to create network " + networkName, e);
        }
    }

    @Override
    public void deleteNetwork(String networkName) {
        try {
            dockerClient.removeNetworkCmd(networkName).exec();
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to delete network " + networkName, e);
        }
    }

    @Override
    public void pullImage(String imageName) {
        try {
            PullImageResultCallback pullCallback = new PullImageResultCallback();
            //AuthConfig authConfig = new AuthConfig().withUsername("user").withPassword("pass").withRegistryAddress("your-private-registry.com");
            dockerClient.pullImageCmd(imageName).exec(pullCallback).awaitCompletion();
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to pull image " + imageName, e);
        }
    }
}
