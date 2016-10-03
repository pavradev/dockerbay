package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        Network network = createNetwork(id);

        List<Container> containers = containerConfigList.stream()
                .map(conf -> createContainer(conf, id))
                .collect(Collectors.toList());

        Set<Volume> volumes = new HashSet<>();
        containers.forEach(c -> volumes.addAll(extractVolumes(c)));

        Environment environment = Environment.withNetwork(network);
        containers.forEach(environment::addContainer);
        volumes.forEach(environment::addVolume);
        return environment;
    }

    private Network createNetwork(String id) {
        Network network = Network.withName(id);
        network.setDockerClient(dockerClient);
        return network;
    }

    private Container createContainer(ContainerConfig containerConfig, String id){
        Container container = Container.withConfig(containerConfig);
        container.setName(containerConfig.getAlias() + "-" + id);
        container.setDockerClient(dockerClient);
        container.setHttpClient(httpClient);
        getBindsForEnv(containerConfig, id).forEach(container::addBind);
        return container;
    }

    private List<Bind> getBindsForEnv(ContainerConfig containerConfig, String id){
        List<Bind> binds = new ArrayList<>();
        containerConfig.getSharedBinds().forEach(binds::add);
        containerConfig.getBinds().stream()
                .map(b -> Bind.create(b.getFrom() + "_" + id, b.getTo()))
                .forEach(binds::add);
        return binds;
    }

    private Set<Volume> extractVolumes(Container container){
        return container.getBinds().stream()
                .filter(Bind::isFromVolume)
                .map(b -> createVolume(b.getFrom()))
                .collect(Collectors.toSet());
    }

    private Volume createVolume(String name) {
        Volume volume = Volume.withName(name);
        volume.setDockerClient(dockerClient);
        return volume;
    }

}
