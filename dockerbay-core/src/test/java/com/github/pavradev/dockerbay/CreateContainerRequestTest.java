package com.github.pavradev.dockerbay;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class CreateContainerRequestTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfContainerNameEmpty() {
        Container.builder()
                .fromImage("image")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfImageEmpty() {
        Container.builder()
                .withName("name")
                .build();
    }

    @Test
    public void shouldBuildCreateContainerRequestWithParameters() {
        Map<String, String> env = new HashMap<>();
        env.put("--param", "value");

        Container createContainerRequest = Container.builder()
                .withName("name")
                .fromImage("image")
                .withAlias("aliasName")
                .inNetwork("network")
                .withCmd(Arrays.asList("cmd"))
                .withEnvVariables(env)
                .withExposedPort(1111)
                .build();

        assertThat(createContainerRequest.getName(), equalTo("name"));
        assertThat(createContainerRequest.getImage(), equalTo("image"));
        assertThat(createContainerRequest.getAlias(), equalTo("aliasName"));
        assertThat(createContainerRequest.getNetworkName(), equalTo("network"));
        assertThat(createContainerRequest.getCmd().get(0), equalTo("cmd"));
        assertThat(createContainerRequest.getEnvVariables().get("--param"), equalTo("value"));
        assertThat(createContainerRequest.getExposedPort(), equalTo(1111));
    }
}
