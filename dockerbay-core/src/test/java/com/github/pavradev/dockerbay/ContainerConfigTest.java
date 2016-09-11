package com.github.pavradev.dockerbay;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;

public class ContainerConfigTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfContainerNameEmpty() {
        ContainerConfig.builder()
                .withImage("image")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfImageEmpty() {
        ContainerConfig.builder()
                .withName("name")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfWaitForUrlWithoutExposedPort() {
        ContainerConfig.builder()
                .withName("name")
                .withImage("image")
                .waitForUrl("/url")
                .build();
    }

    @Test
    public void shouldBuildContainerWithParameters() {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withName("name")
                .withImage("image")
                .withCmd(Arrays.asList("cmd"))
                .addToEnv("--param", "value")
                .withExposedTcpPort(1111)
                .waitForLogEntry("success")
                .displayLogs(true)
                .waitForUrl("/url")
                .waitTimeoutSec(100)
                .build();

        assertThat(containerConfig.getName(), equalTo("name"));
        assertThat(containerConfig.getImage(), equalTo("image"));
        assertThat(containerConfig.getCmd().get(0), equalTo("cmd"));
        assertThat(containerConfig.getEnvVariables().get("--param"), equalTo("value"));
        assertThat(containerConfig.getExposedPort(), equalTo(1111));
        assertThat(containerConfig.getWaitForLogEntry(), equalTo("success"));
        assertThat(containerConfig.getDisplayLogs(), equalTo(true));
        assertThat(containerConfig.getWaitForUrl(), equalTo("/url"));
        assertThat(containerConfig.getTimeoutSec(), equalTo(100));
    }

    @Test
    public void shouldSetDefaultTimeoutIfNotSpecified() {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withName("name")
                .withImage("image")
                .build();

        assertThat(containerConfig.getTimeoutSec(), equalTo(ContainerConfig.DEFAULT_TIMEOUT_SEC));
    }
}
