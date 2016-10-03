package com.github.pavradev.dockerbay;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 *
 */
public class EnvironmentFactoryTest {

    @Test
    public void shouldSetNetworkInEnvironment(){
        EnvironmentFactory environmentFactory = EnvironmentFactory.get();

        Environment environment = environmentFactory.makeEnvironment("123");

        assertThat(environment.getNetwork().getName(), is("123"));
    }

    @Test
    public void shouldSetContainersInEnvironment(){
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .build();
        EnvironmentFactory environmentFactory = EnvironmentFactory.get()
                .withContainers(containerConfig);

        Environment environment = environmentFactory.makeEnvironment("123");

        List<Container> containers = environment.getContainers();
        assertThat(containers.size(), is(1));
        assertThat(containers.get(0).getName(), is("alias-123"));
        assertThat(containers.get(0).getAlias(), is("alias"));
        assertThat(containers.get(0).getImage(), is("image"));
        assertThat(containers.get(0).getNetwork(), is(environment.getNetwork()));
    }

    @Test
    public void shouldSetVolumesInEnvironment(){
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .addBind(Bind.create("volume_name", "to/path"))
                .build();
        EnvironmentFactory environmentFactory = EnvironmentFactory.get()
                .withContainers(containerConfig);

        Environment environment = environmentFactory.makeEnvironment("123");

        Set<Volume> volumes = environment.getVolumes();
        assertThat(volumes.size(), is(1));
        assertThat(volumes.iterator().next().getName(), is("volume_name_123"));
    }

    @Test
    public void shouldSetBindsToContainers(){
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("alias")
                .withImage("image")
                .addBind(Bind.create("volume_name", "to/path"))
                .addBind(Bind.create("/from/private", "to/private"))
                .addSharedBind(Bind.create("/from/shared", "to/shared"))
                .build();
        EnvironmentFactory environmentFactory = EnvironmentFactory.get()
                .withContainers(containerConfig);

        Environment environment = environmentFactory.makeEnvironment("123");

        Container container = environment.findContainerByAlias("alias").get();
        assertThat(container.getBinds().size(), is(3));
        assertThat(container.getBinds().get("volume_name_123"), is("to/path"));
        assertThat(container.getBinds().get("/from/private_123"), is("to/private"));
        assertThat(container.getBinds().get("/from/shared"), is("to/shared"));
    }
}
