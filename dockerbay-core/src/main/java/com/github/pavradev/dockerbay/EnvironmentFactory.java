package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.Client;

/**
 * Defines how to create Environment
 * Contains information about:
 * 1. which DockerClientWrapper to use
 * 2. which javax.ws.rs.client.Client to use
 * 3. Container configurations for Environment
 */
public class EnvironmentFactory {
    private DockerClientWrapper dockerClient;
    private Client httpClient;

    private List<ContainerConfig> containerConfigList = new ArrayList<>();

    private EnvironmentFactory() {
    }

    public static EnvironmentFactory get() {
        return new EnvironmentFactory();
    }

    public EnvironmentFactory withDockerClient(DockerClientWrapper dockerClient) {
        this.dockerClient = dockerClient;
        return this;
    }

    public EnvironmentFactory withHttpClient(Client httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public EnvironmentFactory withContainers(ContainerConfig... containers) {
        this.containerConfigList = Arrays.asList(containers);
        return this;
    }

    public Environment makeEnvironment(String id) {
        Network network = Network.withName(id);
        network.setDockerClient(dockerClient);

        Environment environment = Environment.withNetwork(network);

        Set<Volume> volumes = new HashSet<>();
        for (ContainerConfig containerConfig : this.containerConfigList) {
            Container container = Container.withConfig(containerConfig);
            container.setDockerClient(dockerClient);
            container.setHttpClient(httpClient);
            containerConfig.getSharedBinds().forEach(container::addBind);
            for(Bind bind : containerConfig.getBinds()){
                Bind envBind = toEnvironmentBind(bind, id);
                container.addBind(envBind);
                if(envBind.isFromVolume()){
                    volumes.add(createVolume(envBind.getFrom()));
                }
            }
            environment.addContainer(container);
        }
        volumes.stream().forEach(environment::addVolume);

        return environment;
    }

    public Volume createVolume(String name) {
        Volume volume = Volume.withName(name);
        volume.setDockerClient(dockerClient);
        return volume;
    }

    private Bind toEnvironmentBind(Bind b, String id){
        return Bind.create(b.getFrom() + "_" + id, b.getTo());
    }

}
