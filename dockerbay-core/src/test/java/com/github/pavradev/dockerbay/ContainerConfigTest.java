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
                .withAlias("name")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfWaitForUrlWithoutExposedPort() {
        ContainerConfig.builder()
                .withAlias("name")
                .withImage("image")
                .waitForUrl("/url")
                .build();
    }

    @Test
    public void shouldBuildContainerWithParameters() {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("name")
                .withImage("image")
                .withCmd(Arrays.asList("cmd"))
                .addToEnv("--param", "value")
                .withExposedTcpPort(1111)
                .withDebugPort(2222)
                .waitForLogEntry("success")
                .displayLogs(true)
                .waitForUrl("/url")
                .waitTimeoutSec(100)
                .build();

        assertThat(containerConfig.getAlias(), equalTo("name"));
        assertThat(containerConfig.getImage(), equalTo("image"));
        assertThat(containerConfig.getCmd().get(0), equalTo("cmd"));
        assertThat(containerConfig.getEnvVariables().get("--param"), equalTo("value"));
        assertThat(containerConfig.getExposedPort(), equalTo(1111));
        assertThat(containerConfig.getDebugPort(), equalTo(2222));
        assertThat(containerConfig.getWaitForLogEntry(), equalTo("success"));
        assertThat(containerConfig.getDisplayLogs(), equalTo(true));
        assertThat(containerConfig.getWaitForUrl(), equalTo("/url"));
        assertThat(containerConfig.getTimeoutSec(), equalTo(100));
    }

    @Test
    public void shouldSetDefaultTimeoutIfNotSpecified() {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .withAlias("name")
                .withImage("image")
                .build();

        assertThat(containerConfig.getTimeoutSec(), equalTo(ContainerConfig.DEFAULT_TIMEOUT_SEC));
    }
}
