package com.github.pavradev.dockerbay;

import com.github.khazrak.jdocker.DefaultDockerClient;
import com.github.khazrak.jdocker.DockerClient;
import com.github.khazrak.jdocker.model.api124.requests.ContainerCreationRequest;
import com.github.khazrak.jdocker.model.api124.requests.NetworkCreateRequest;
import org.junit.Before;
import org.junit.Test;

public class DockerClientImplTest {

    private DockerClientWrapper dockerClientWrapper;
    private DockerClient dockerClient = new DefaultDockerClient();

    @Before
    public void beforeMethod() {
        dockerClientWrapper = new DockerClientImpl();
    }

    @Test
    public void testNetwork(){
        NetworkCreateRequest networkCreateRequest = NetworkCreateRequest.builder()
                .name("test-net")
                .checkDuplicate(true)
                .driver("bridge")
                .build();
        dockerClient.createNetwork(networkCreateRequest);
        dockerClient.removeNetwork("test-net");
    }

    @Test
    public void testContainer(){
        ContainerCreationRequest containerCreationRequest = ContainerCreationRequest.builder()
                .name("test-container")
                .image("ekino/wiremock:2.1.11")
                .build();
        dockerClient.createContainer(containerCreationRequest);
        dockerClient.restart("test-container");
        dockerClient.stop("test-container");
    }
}
