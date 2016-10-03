package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
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
    public void createContainer(Container container) {
        try {
            HostConfig hostConfig = new HostConfig()
                    .withNetworkMode(container.getNetwork().getName());

            List<Link> links = container.getNetwork().getContainers().stream()
                    .map(c -> new Link(c.getName(), c.getAlias()))
                    .collect(Collectors.toList());
            hostConfig.withLinks(new Links(links));

            if (container.getExposedPort() != null) {
                PortBinding portBinding = new PortBinding(
                        Ports.Binding.bindIp("0.0.0.0"),
                        ExposedPort.tcp(container.getExposedPort()));
                hostConfig.withPortBindings(new Ports(portBinding));
            }

            List<String> env = new ArrayList<>();
            container.getEnvVariables().entrySet().forEach(e -> {
                env.add(e.getKey());
                env.add(e.getValue());
            });

            CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(container.getImage())
                    .withName(container.getName())
                    .withHostConfig(hostConfig)
                    .withEnv(env);

            if (container.getCmd() != null) {
                createContainerCmd.withCmd(container.getCmd());
            }
            if (container.getExposedPort() != null) {
                createContainerCmd.withExposedPorts(ExposedPort.tcp(container.getExposedPort()));
            }

            createContainerCmd.exec();

        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to create container " + container.getName(), e);
        }
    }

    @Override
    public void startContainer(Container container) {
        try {
            dockerClient.startContainerCmd(container.getName()).exec();
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to start container " + container.getName(), e);
        }
    }

    @Override
    public void stopContainer(Container container) {
        try {
            dockerClient.killContainerCmd(container.getName()).exec(); //Killing is faster and more reliable
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to stop container " + container.getName(), e);
        }
    }

    @Override
    public void removeContainer(Container container) {
        try {
            dockerClient.removeContainerCmd(container.getName()).exec();
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to remove container " + container.getName(), e);
        }
    }

    @Override
    public boolean isContainerExist(Container container) {
        return false;
    }

    @Override
    public Map<Integer, Integer> getPortMappings(Container container) {
        try {
            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(container.getName()).exec();
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
            throw new DockerClientWrapperException("Failed to inspect container " + container.getName(), e);
        }
    }

    @Override
    public String getContainerLogs(Container container) {
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

            dockerClient.logContainerCmd(container.getName())
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(loggingCallback);
            loggingCallback.awaitCompletion();
            loggingCallback.close();
            return loggingCallback.toString();

        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to read container logs from " + container.getName(), e);
        }
    }

    @Override
    public boolean isNetworkExists(Network network) {
        try {
            dockerClient.inspectNetworkCmd()
                    .withNetworkId(network.getName())
                    .exec();
            return true;
        } catch (NotFoundException e){
            return false;
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to create network " + network, e);
        }
    }

    @Override
    public void createNetwork(Network network) {
        try {
            dockerClient.createNetworkCmd()
                    .withDriver("bridge")
                    .withName(network.getName())
                    .withCheckDuplicate(true)
                    .exec();
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to create network " + network, e);
        }
    }

    @Override
    public void deleteNetwork(Network network) {
        try {
            dockerClient.removeNetworkCmd(network.getName()).exec();
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to delete network " + network, e);
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

    @Override
    public void createVolume(Volume volume) {
        try {
            dockerClient.createVolumeCmd()
                    .withName(volume.getName())
                    .exec();
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to create volume " + volume, e);
        }
    }

    @Override
    public void deleteVolume(Volume volume) {
        try {
            dockerClient.removeVolumeCmd(volume.getName()).exec();
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to delete volume " + volume, e);
        }
    }
}
