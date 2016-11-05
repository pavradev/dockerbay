package com.github.pavradev.dockerbay;

import com.github.khazrak.jdocker.DefaultDockerClient;
import com.github.khazrak.jdocker.DockerClient;
import com.github.khazrak.jdocker.model.api124.HostConfig;
import com.github.khazrak.jdocker.model.api124.requests.ContainerCreationRequest;
import com.github.khazrak.jdocker.model.api124.requests.NetworkCreateRequest;
import com.github.khazrak.jdocker.utils.DockerImageName;
import com.github.pavradev.dockerbay.exceptions.DockerClientWrapperException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Docker client implementation based on Java DockerClient https://github.com/docker-java/docker-java
 */
public class DockerClientImpl implements DockerClientWrapper {

    private DockerClient dockerClient = new DefaultDockerClient();

    /**
     * If you need to configure DockerClient programmatically
     */
    public void setDockerClient(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void createContainer(CreateContainerRequest createContainerRequest) {
        try {
            HostConfig hostConfig = HostConfig.builder()
                    .networkMode(createContainerRequest.getNetworkName())
                    .build();


//            if (!createContainerRequest.getLinks().isEmpty()) {
//                List<Link> links = createContainerRequest.getLinks().stream().map(l -> {
//                    String[] link = l.split(":");
//                    return new Link(link[0], link[1]);
//                }).collect(Collectors.toList());
//                hostConfig.withLinks(new Links(links));
//            }
//            if (createContainerRequest.getExposedPort() != null) {
//                PortBinding portBinding = new PortBinding(
//                        Ports.Binding.bindIp("0.0.0.0"),
//                        ExposedPort.tcp(createContainerRequest.getExposedPort()));
//                hostConfig.withPortBindings(new Ports(portBinding));
//            }

            List<String> env = new ArrayList<>();
            createContainerRequest.getEnvVariables().entrySet().forEach(e -> {
                env.add(e.getKey());
                env.add(e.getValue());
            });

            ContainerCreationRequest.ContainerCreationRequestBuilder createContainerRequestBuilder = ContainerCreationRequest.builder()
                    .image(createContainerRequest.getImage())
                    .name(createContainerRequest.getName())
                    .hostConfig(hostConfig)
                    .environmentVariables(env);

            if (createContainerRequest.getCmd() != null) {
                createContainerRequestBuilder.commands(createContainerRequest.getCmd());
            }
            if (createContainerRequest.getExposedPort() != null) {
                //createContainerRequestBuilder.exposedPort(ExposedPort.tcp(createContainerRequest.getExposedPort()));
            }

            dockerClient.createContainer(createContainerRequestBuilder.build());

        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to create container " + createContainerRequest.getName(), e);
        }
    }

    @Override
    public void startContainer(String containerName) {
        try {
            dockerClient.start(containerName);
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to start container " + containerName, e);
        }
    }

    @Override
    public void stopContainer(String containerName) {
        try {
            dockerClient.kill(containerName); //Killing is faster and more reliable
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to stop container " + containerName, e);
        }
    }

    @Override
    public void removeContainer(String containerName) {
        try {
            dockerClient.remove(containerName);
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to remove container " + containerName, e);
        }
    }

    @Override
    public Map<Integer, Integer> getPortMappings(String containerName) {
        try {
//            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerName).exec();
//            Ports ports = containerInfo.getNetworkSettings().getPorts();
//            Map<Integer, Integer> result = new HashMap<>();
//            ports.getBindings().entrySet().forEach(e -> {
//                if (e.getValue() != null) {
//                    Integer containerPort = e.getKey().getPort();
//                    Integer localPort = Integer.parseInt(e.getValue()[0].getHostPortSpec());
//                    result.put(containerPort, localPort);
//                }
//            });
//            return result;
            return null;
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to inspect container " + containerName, e);
        }
    }

    @Override
    public String getContainerLogs(String containerName) {
        try {
//            LogContainerResultCallback loggingCallback = new LogContainerResultCallback() {
//                private final StringBuffer log = new StringBuffer();
//
//                @Override
//                public void onNext(Frame frame) {
//                    log.append(new String(frame.getPayload()));
//                }
//
//                @Override
//                public String toString() {
//                    return log.toString();
//                }
//            };
//
//            dockerClient.logContainerCmd(containerName)
//                    .withStdOut(true)
//                    .withStdErr(true)
//                    .exec(loggingCallback);
//            loggingCallback.awaitCompletion();
//            loggingCallback.close();
//            return loggingCallback.toString();
            return "";
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to read container logs from " + containerName, e);
        }
    }

    @Override
    public void createNetwork(String networkName) {
        try {
            NetworkCreateRequest networkCreateRequest = NetworkCreateRequest.builder()
                    .driver("bridge")
                    .name(networkName)
                    .checkDuplicate(true)
                    .build();
            dockerClient.createNetwork(networkCreateRequest);
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to create network " + networkName, e);
        }
    }

    @Override
    public void deleteNetwork(String networkName) {
        try {
            dockerClient.removeNetwork(networkName);
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to delete network " + networkName, e);
        }
    }

    @Override
    public void pullImage(String imageName) {
        try {
            DockerImageName dockerImageName = new DockerImageName(imageName);
            dockerClient.pullImage(dockerImageName);
        } catch (Exception e) {
            throw new DockerClientWrapperException("Failed to pull image " + imageName, e);
        }
    }
}
