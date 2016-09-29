package com.github.pavradev.dockerbay;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import javax.ws.rs.client.Client;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import jersey.repackaged.com.google.common.collect.ImmutableMap;

/**
 *
 */
public class ContainerTest {

    private Container container;
    private DockerClientWrapper dockerClientMock;
    private Client httpClientMock;

    @Before
    public void beforeMethod(){
        dockerClientMock = mock(DockerClientWrapper.class);
        httpClientMock = mock(Client.class);

        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .build();
        container = buildContainer(containerConfig);
    }

    private Container buildContainer(ContainerConfig containerConfig){
        container = Container.withConfig(containerConfig);
        container.setDockerClient(dockerClientMock);
        container.setHttpClient(httpClientMock);
        return container;
    }

    @Test
    public void shouldAttachItselfToNetwork(){
        Network network = Network.withName("net");
        container.attachToNetwork(network);

        assertThat(container.getNetwork(), is(network));
        assertThat(container.getNetwork().getContainers().size(), is(1));
    }

    @Test
    public void shouldGetNameIncludingNetworkName(){
        Network network = Network.withName("net");
        container.attachToNetwork(network);

        assertThat(container.getName(), is("alias-net"));
    }

    @Test
    public void shouldGetAliasAsNameWithoutNetwork(){
        assertThat(container.getName(), is("alias"));
    }

    @Test
    public void shouldGetAlias(){
        assertThat(container.getAlias(), is("alias"));
    }

    @Test
    public void shouldGetImageName(){
        assertThat(container.getImage(), is("image"));
    }

    @Test
    public void shouldGetExposedPort(){
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .withExposedTcpPort(9999)
                .build();
        container = buildContainer(containerConfig);
        assertThat(container.getExposedPort(), is(9999));
    }

    @Test
    public void shouldGetCmd(){
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .withCmd(Arrays.asList("-p", "-q"))
                .build();
        container = buildContainer(containerConfig);
        assertThat(container.getCmd(), CoreMatchers.hasItems("-p", "-q"));
    }

    @Test
    public void shouldGetEnvVariables(){
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .addToEnv("user", "me")
                .build();
        container = buildContainer(containerConfig);
        assertThat(container.getEnvVariables().get("user"), is("me"));
    }

    @Test
    public void shouldPullImageBeforeCreate(){
        container.create();

        InOrder inOrder = Mockito.inOrder(dockerClientMock);
        inOrder.verify(dockerClientMock).pullImage(eq("image"));
        inOrder.verify(dockerClientMock).createContainer(eq(container));
    }

    @Test
    public void shouldChangeStatusOnStart(){
        container.start();

        assertThat(container.getStatus(), is(Container.ContainerStatus.STARTED));
        verify(dockerClientMock).startContainer(eq(container));
    }

    @Test
    public void shouldMarkStartedEvenOnError(){
        doThrow(new RuntimeException("Fail!")).when(dockerClientMock).startContainer(anyObject());

        try {
            container.start();
            assertFalse("Should throw exception", true);
        } catch (Exception e){}

        assertThat(container.getStatus(), is(Container.ContainerStatus.STARTED));
    }

    @Test
    public void shouldAssignLocalPortOnStart(){
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .withExposedTcpPort(9999)
                .build();
        container = buildContainer(containerConfig);
        doReturn(ImmutableMap.of(9999, 4444)).when(dockerClientMock).getPortMappings(container);

        container.start();

        assertThat(container.getLocalPort(), is(4444));
    }

    @Test
    public void shouldWaitForLogEntryOnStart(){
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .waitForLogEntry("started")
                .build();
        container = buildContainer(containerConfig);
        doReturn("Container is started!").when(dockerClientMock).getContainerLogs(anyObject());

        container.start();

        assertThat(container.getStatus(), is(Container.ContainerStatus.STARTED));
        verify(dockerClientMock).getContainerLogs(eq(container));
    }

    public void shouldWaitForUrlOnStart(){

    }
}
