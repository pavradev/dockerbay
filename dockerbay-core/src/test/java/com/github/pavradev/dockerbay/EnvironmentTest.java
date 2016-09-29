package com.github.pavradev.dockerbay;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.github.pavradev.dockerbay.exceptions.EnvironmentException;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class EnvironmentTest {

    @Test
    public void shouldAttachContainersToTheNetwork(){
        Network networkMock = mock(Network.class);
        Container containerMock = mock(Container.class);
        Environment environment = Environment.withNetwork(networkMock);
        environment.addContainer(containerMock);

        assertThat(environment.getContainers().size(), is(1));
        verify(containerMock).attachToNetwork(eq(networkMock));
    }

    @Test
    public void shouldCreateNetwork() {
        Network networkMock = mock(Network.class);
        Environment environment = Environment.withNetwork(networkMock);

        environment.initialize();

        verify(networkMock).create();
    }

    @Test
    public void shouldStartContainersInOrder() {
        Network networkMock = mock(Network.class);
        Container containerMock1 = mock(Container.class);
        Container containerMock2 = mock(Container.class);
        Environment environment = Environment.withNetwork(networkMock);
        environment.addContainer(containerMock1);
        environment.addContainer(containerMock2);

        environment.initialize();

        InOrder inOrder = Mockito.inOrder(containerMock1, containerMock2);
        inOrder.verify(containerMock1).create();
        inOrder.verify(containerMock2).create();
        inOrder.verify(containerMock1).start();
        inOrder.verify(containerMock2).start();
    }

    @Test
    public void shouldStartContainersUntilFirstFailure() throws Throwable {
        Network networkMock = mock(Network.class);
        Container containerMock1 = mock(Container.class);
        Container containerMock2 = mock(Container.class);
        doThrow(new RuntimeException("Fail!")).when(containerMock1).start();

        Environment environment = Environment.withNetwork(networkMock);
        environment.addContainer(containerMock1);
        environment.addContainer(containerMock1);

        environment.initialize();

        verify(containerMock1).start();
        verify(containerMock2, never()).start();
    }

    @Test(expected = EnvironmentException.class)
    public void shouldNotAllowCallingInitializeTwice() {
        Network networkMock = mock(Network.class);
        Environment environment = Environment.withNetwork(networkMock);

        environment.initialize();
        environment.initialize();
    }

    @Test(expected = EnvironmentException.class)
    public void shouldNotAllowCallingCleanupIfNotInitialized() {
        Network networkMock = mock(Network.class);
        Environment environment = Environment.withNetwork(networkMock);

        environment.cleanup();
    }

    @Test
    public void shouldDeleteNetwork() {
        Network networkMock = mock(Network.class);
        Environment environment = Environment.withNetwork(networkMock);

        environment.initialize();
        environment.cleanup();

        verify(networkMock).delete();
    }

    @Test
    public void shouldStopContainersThenDeleteNetwork() {
        Network networkMock = mock(Network.class);
        Container containerMock = mock(Container.class);
        Environment environment = Environment.withNetwork(networkMock);
        environment.addContainer(containerMock);

        environment.initialize();
        environment.cleanup();

        InOrder inOrder = Mockito.inOrder(networkMock, containerMock);
        inOrder.verify(containerMock).stop();
        inOrder.verify(containerMock).remove();
        inOrder.verify(networkMock).delete();
    }

    @Test
    public void shouldStopContainersInReverseOrder() throws Throwable {
        Network networkMock = mock(Network.class);
        Container containerMock1 = mock(Container.class);
        Container containerMock2 = mock(Container.class);
        Environment environment = Environment.withNetwork(networkMock);
        environment.addContainer(containerMock1);
        environment.addContainer(containerMock2);

        environment.initialize();
        environment.cleanup();

        InOrder inOrder = Mockito.inOrder(containerMock1, containerMock2);
        inOrder.verify(containerMock2).stop();
        inOrder.verify(containerMock1).stop();
    }

    @Test
    public void shouldStopContainersDespiteFailures() throws Throwable {
        Network networkMock = mock(Network.class);
        Container containerMock1 = mock(Container.class);
        Container containerMock2 = mock(Container.class);
        doThrow(new RuntimeException("Fail!")).when(containerMock2).stop();
        Environment environment = Environment.withNetwork(networkMock);
        environment.addContainer(containerMock1);
        environment.addContainer(containerMock2);

        environment.initialize();
        environment.cleanup();

        verify(containerMock1).stop();
        verify(containerMock2).stop();
    }


}
