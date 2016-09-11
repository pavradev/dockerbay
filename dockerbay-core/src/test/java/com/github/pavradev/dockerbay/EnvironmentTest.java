package com.github.pavradev.dockerbay;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.github.pavradev.dockerbay.exceptions.EnvironmentException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class EnvironmentTest {

    private Environment environment;

    private DockerClientWrapper dockerClientWrapperMock;
    private Client httpClientMock;

    @Before
    public void beforeMethod() {
        dockerClientWrapperMock = mock(DockerClientWrapper.class);
        httpClientMock = mock(Client.class);

        environment = new Environment(dockerClientWrapperMock, httpClientMock);
        environment.setNetworkName("net");
    }

    @Test
    public void shouldReturnUniqueContainerName() {
        String name = environment.buildUniqueContainerName("container");
        assertThat(name, is("net-container"));
    }

    @Test
    public void shouldCreateNetwork() {
        environment.initialize();
        verify(dockerClientWrapperMock).createNetwork("net");
    }

    @Test
    public void shouldPullRequiredImages() {
        environment.setContainers(Arrays.asList(ContainerConfig.builder()
                .withName("dummyContainer")
                .withImage("requiredImage")
                .build()));

        environment.initialize();

        verify(dockerClientWrapperMock).pullImage("requiredImage");
    }

    @Test
    public void shouldStartContainersInOrder() {
        environment.setContainers(Arrays.asList(
                ContainerConfig.builder()
                        .withName("firstContainer")
                        .withImage("requiredImage")
                        .build(),
                ContainerConfig.builder()
                        .withName("secondContainer")
                        .withImage("requiredImage")
                        .build()));

        environment.initialize();

        InOrder inOrder = Mockito.inOrder(dockerClientWrapperMock);
        inOrder.verify(dockerClientWrapperMock).createContainer(anyObject());
        inOrder.verify(dockerClientWrapperMock).startContainer(eq("net-firstContainer"));
        inOrder.verify(dockerClientWrapperMock).createContainer(anyObject());
        inOrder.verify(dockerClientWrapperMock).startContainer(eq("net-secondContainer"));
    }

    @Test
    public void shouldStartContainersUntilFirstFailure() throws Throwable {
        doThrow(new RuntimeException("Fail!")).when(dockerClientWrapperMock).startContainer(eq("net-firstContainer"));
        environment.setContainers(Arrays.asList(
                ContainerConfig.builder()
                        .withName("firstContainer")
                        .withImage("requiredImage")
                        .build(),
                ContainerConfig.builder()
                        .withName("secondContainer")
                        .withImage("requiredImage")
                        .build()));

        environment.initialize();

        verify(dockerClientWrapperMock).startContainer(eq("net-firstContainer"));
        verify(dockerClientWrapperMock, never()).startContainer(eq("net-secondContainer"));
    }

    @Test
    public void shouldWaitForUrlIfNeeded() {
        Map<Integer, Integer> portMapping = new HashMap<>();
        portMapping.put(1111, 2222);
        doReturn(portMapping).when(dockerClientWrapperMock).getPortMappings(anyString());
        Response response = mock(Response.class);
        doReturn(200).when(response).getStatus();
        mockHttpResponse(response);

        environment.setContainers(Arrays.asList(ContainerConfig.builder()
                .withName("dummyContainer")
                .withImage("requiredImage")
                .withExposedTcpPort(1111)
                .waitForUrl("/some/path")
                .build()));

        environment.initialize();

        verify(httpClientMock).target("http://localhost:2222");
    }

    private WebTarget mockHttpResponse(Response responseMock) {
        WebTarget webTargetMock = mock(WebTarget.class);
        doReturn(webTargetMock).when(httpClientMock).target(anyString());
        doReturn(webTargetMock).when(webTargetMock).path(anyString());
        Invocation.Builder builderMock = mock(Invocation.Builder.class);
        doReturn(builderMock).when(webTargetMock).request();
        doReturn(responseMock).when(builderMock).get();
        return webTargetMock;
    }

    @Test
    public void shouldWaitForLogIfNeeded() {
        doReturn("Service Started!").when(dockerClientWrapperMock).getContainerLogs(anyString());

        environment.setContainers(Arrays.asList(ContainerConfig.builder()
                .withName("dummyContainer")
                .withImage("requiredImage")
                .waitForLogEntry("Started!")
                .build()));

        environment.initialize();

        verify(dockerClientWrapperMock).getContainerLogs("net-dummyContainer");
    }

    @Test
    public void shouldPullImagesThenCreateNetworkThenStartContainers() {
        environment.setContainers(Arrays.asList(ContainerConfig.builder()
                .withName("dummyContainer")
                .withImage("requiredImage")
                .build()));

        environment.initialize();

        InOrder inOrder = Mockito.inOrder(dockerClientWrapperMock);
        inOrder.verify(dockerClientWrapperMock).pullImage(anyObject());
        inOrder.verify(dockerClientWrapperMock).createNetwork(anyObject());
        inOrder.verify(dockerClientWrapperMock).createContainer(anyObject());
        inOrder.verify(dockerClientWrapperMock).startContainer(anyObject());
    }

    @Test(expected = EnvironmentException.class)
    public void shouldNotAllowCallingInitializeTwice() {
        environment.initialize();
        environment.initialize();
    }

    @Test(expected = EnvironmentException.class)
    public void shouldNotAllowCallingCleanupIfNotInitialized() {
        environment.cleanup();
    }

    @Test
    public void shouldDeleteNetwork() {
        environment.initialize();
        environment.cleanup();
        verify(dockerClientWrapperMock).deleteNetwork("net");
    }

    @Test
    public void shouldStopAllStartedContainers() throws Throwable {
        environment.setContainers(Arrays.asList(ContainerConfig.builder()
                .withName("dummyContainer")
                .withImage("requiredImage")
                .build()));

        environment.initialize();
        environment.cleanup();

        verify(dockerClientWrapperMock).stopContainer("net-dummyContainer");
        verify(dockerClientWrapperMock).removeContainer("net-dummyContainer");
    }

    @Test
    public void shouldStopContainersThenDeleteNetwork() {
        environment.setContainers(Arrays.asList(
                ContainerConfig.builder()
                        .withName("firstContainer")
                        .withImage("requiredImage")
                        .build()));

        environment.initialize();
        environment.cleanup();

        InOrder inOrder = Mockito.inOrder(dockerClientWrapperMock);
        inOrder.verify(dockerClientWrapperMock).stopContainer(anyString());
        inOrder.verify(dockerClientWrapperMock).removeContainer(anyString());
        inOrder.verify(dockerClientWrapperMock).deleteNetwork(anyString());
    }

    @Test
    public void shouldStopContainersDespiteFailures() throws Throwable {
        doThrow(new RuntimeException("Fail!")).when(dockerClientWrapperMock).stopContainer(anyString());
        environment.setContainers(Arrays.asList(
                ContainerConfig.builder()
                        .withName("firstContainer")
                        .withImage("requiredImage")
                        .build(),
                ContainerConfig.builder()
                        .withName("secondContainer")
                        .withImage("requiredImage")
                        .build()));

        environment.initialize();
        environment.cleanup();

        verify(dockerClientWrapperMock).stopContainer(eq("net-secondContainer"));
        verify(dockerClientWrapperMock).removeContainer(eq("net-secondContainer"));
        verify(dockerClientWrapperMock).stopContainer(eq("net-firstContainer"));
        verify(dockerClientWrapperMock).removeContainer(eq("net-firstContainer"));
    }


}
