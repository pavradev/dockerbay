package com.github.pavradev.dockerbay;

import jersey.repackaged.com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 *
 */
public class ContainerTest {

    private Container container;
    private DockerClientWrapper dockerClientMock;
    private Client httpClientMock;

    @Before
    public void beforeMethod() {
        dockerClientMock = mock(DockerClientWrapper.class);
        httpClientMock = mock(Client.class);

        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .build();
        container = buildContainer(containerConfig);
    }

    @After
    public void afterMethod() {
        Mockito.verifyNoMoreInteractions(dockerClientMock);
    }

    private Container buildContainer(ContainerConfig containerConfig) {
        container = Container.withConfig(containerConfig);
        container.setName("dummy");
        container.setDockerClient(dockerClientMock);
        container.setHttpClient(httpClientMock);
        return container;
    }

    @Test
    public void shouldBeAddedToNetworkWhenAttached() {
        Network network = Network.withName("net");
        container.attachToNetwork(network);

        assertThat(container.getNetwork(), is(network));
        assertThat(container.getNetwork().getContainers().size(), is(1));
    }

    @Test
    public void shouldGetName() {
        assertThat(container.getName(), is("dummy"));
    }

    @Test
    public void shouldGetContainerBinds(){
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .build();
        container = buildContainer(containerConfig);
        container.addBind(Bind.create("/from/path", "/to/path"));

        List<Bind> containerBinds = container.getBinds();
        assertThat(containerBinds.size(), is(1));
        assertThat(containerBinds.get(0).toString(), is("/from/path:/to/path"));
    }

    @Test
    public void shouldGetAlias() {
        assertThat(container.getAlias(), is("alias"));
    }

    @Test
    public void shouldGetImageName() {
        assertThat(container.getImage(), is("image"));
    }

    @Test
    public void shouldGetExposedPort() {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .withExposedTcpPort(9999)
                .build();
        container = buildContainer(containerConfig);
        assertThat(container.getExposedPort(), is(9999));
    }

    @Test
    public void shouldGetCmd() {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .withCmd(Arrays.asList("-p", "-q"))
                .build();
        container = buildContainer(containerConfig);
        assertThat(container.getCmd(), hasItems("-p", "-q"));
    }

    @Test
    public void shouldGetEnvVariables() {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .addToEnv("user", "me")
                .build();
        container = buildContainer(containerConfig);
        assertThat(container.getEnvVariables().get("user"), is("me"));
    }

    //CREATE

    @Test
    public void shouldPullImageBeforeCreate() {
        container.create();

        InOrder inOrder = Mockito.inOrder(dockerClientMock);
        inOrder.verify(dockerClientMock).pullImage(eq("image"));
        inOrder.verify(dockerClientMock).createContainer(eq(container));
    }

    @Test
    public void shouldChangeContainerStatusToCreated() {
        container.create();

        assertThat(container.getStatus(), is(Container.ContainerStatus.CREATED_OR_STOPPED));
        verify(dockerClientMock).pullImage(eq("image"));
        verify(dockerClientMock).createContainer(eq(container));
    }

    @Test
    public void shouldNotCreateCreatedContainer() {
        container.setStatus(Container.ContainerStatus.CREATED_OR_STOPPED);

        container.create();
        verify(dockerClientMock, never()).createContainer(eq(container));
    }

    //START

    @Test
    public void shouldChangeStatusOnStart() {
        container.setStatus(Container.ContainerStatus.CREATED_OR_STOPPED);

        container.start();

        assertThat(container.getStatus(), is(Container.ContainerStatus.RUNNING));
        verify(dockerClientMock).startContainer(eq(container));
    }

    @Test
    public void shouldMarkStartedEvenOnError() {
        container.setStatus(Container.ContainerStatus.CREATED_OR_STOPPED);
        doThrow(new RuntimeException("Fail!")).when(dockerClientMock).startContainer(anyObject());

        try {
            container.start();
            assertFalse("Should throw exception", true);
        } catch (Exception e) {
        }

        assertThat(container.getStatus(), is(Container.ContainerStatus.RUNNING));
        verify(dockerClientMock).startContainer(eq(container));
    }

    @Test
    public void shouldNotStartRunningContainer() {
        container.setStatus(Container.ContainerStatus.RUNNING);

        container.start();

        verify(dockerClientMock, never()).startContainer(eq(container));
    }

    @Test
    public void shouldNotStartNonCreatedContainer() {
        container.setStatus(Container.ContainerStatus.NOT_CREATED);

        container.start();

        verify(dockerClientMock, never()).startContainer(eq(container));
    }

    @Test
    public void shouldAssignLocalPortOnStart() {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .withExposedTcpPort(9999)
                .build();
        container = buildContainer(containerConfig);
        container.setStatus(Container.ContainerStatus.CREATED_OR_STOPPED);
        doReturn(ImmutableMap.of(9999, 4444)).when(dockerClientMock).getPortMappings(container);

        container.start();

        assertThat(container.getLocalPort(), is(4444));
        verify(dockerClientMock).startContainer(eq(container));
        verify(dockerClientMock).getPortMappings(eq(container));
    }

    @Test
    public void shouldAssignLocalDebugPortOnStart() {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .withDebugPort(7777)
                .build();
        container = buildContainer(containerConfig);
        container.setStatus(Container.ContainerStatus.CREATED_OR_STOPPED);
        doReturn(ImmutableMap.of(7777, 1234)).when(dockerClientMock).getPortMappings(container);

        container.start();

        assertThat(container.getLocalDebugPort(), is(1234));
        verify(dockerClientMock).startContainer(eq(container));
        verify(dockerClientMock).getPortMappings(eq(container));
    }

    @Test
    public void shouldWaitForLogEntryOnStart() {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .waitForLogEntry("started")
                .build();
        container = buildContainer(containerConfig);
        container.setStatus(Container.ContainerStatus.CREATED_OR_STOPPED);
        doReturn("Container is started!").when(dockerClientMock).getContainerLogs(anyObject());

        container.start();

        assertThat(container.getStatus(), is(Container.ContainerStatus.RUNNING));
        verify(dockerClientMock).getContainerLogs(eq(container));
        verify(dockerClientMock).startContainer(eq(container));
    }

    @Test
    public void shouldWaitForUrlOnStart() {

        Response response = mock(Response.class);
        doReturn(200).when(response).getStatus();
        mockHttpResponse(response);

        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .withExposedTcpPort(1111)
                .waitForUrl("/some/path")
                .build();
        container = buildContainer(containerConfig);
        container.setStatus(Container.ContainerStatus.CREATED_OR_STOPPED);
        doReturn(ImmutableMap.of(1111, 2222)).when(dockerClientMock).getPortMappings(eq(container));

        container.start();

        verify(dockerClientMock).startContainer(eq(container));
        verify(dockerClientMock).getPortMappings(eq(container));
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

    //STOP

    @Test
    public void shouldChangeStatusOnStop() {
        container.setStatus(Container.ContainerStatus.RUNNING);

        container.stop();

        assertThat(container.getStatus(), is(Container.ContainerStatus.CREATED_OR_STOPPED));
        verify(dockerClientMock).stopContainer(eq(container));
    }

    @Test
    public void shouldNotStopNotRunningContainer() {
        container.setStatus(Container.ContainerStatus.CREATED_OR_STOPPED);

        container.stop();

        verify(dockerClientMock, never()).stopContainer(eq(container));
    }

    @Test
    public void shouldNotStopNonExistingContainer() {
        container.setStatus(Container.ContainerStatus.NOT_CREATED);

        container.stop();

        verify(dockerClientMock, never()).stopContainer(eq(container));
    }

    //REMOVE

    @Test
    public void shouldChangeStatusOnRemove() {
        container.setStatus(Container.ContainerStatus.CREATED_OR_STOPPED);

        container.remove();

        assertThat(container.getStatus(), is(Container.ContainerStatus.NOT_CREATED));
        verify(dockerClientMock).removeContainer(eq(container));
    }

    @Test
    public void shouldDisplayLogsBeforeRemoving() {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .displayLogs(true)
                .build();
        container = buildContainer(containerConfig);
        container.setStatus(Container.ContainerStatus.CREATED_OR_STOPPED);

        container.remove();

        verify(dockerClientMock).removeContainer(eq(container));
        verify(dockerClientMock).getContainerLogs(eq(container));
    }

    @Test
    public void shouldNotRemoveRunningContainer() {
        container.setStatus(Container.ContainerStatus.RUNNING);

        container.remove();

        verify(dockerClientMock, never()).removeContainer(eq(container));
    }

    @Test
    public void shouldNotRemoveNonExistingContainer() {
        container.setStatus(Container.ContainerStatus.NOT_CREATED);

        container.remove();

        verify(dockerClientMock, never()).removeContainer(eq(container));
    }
}
