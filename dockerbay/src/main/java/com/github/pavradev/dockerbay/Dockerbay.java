package com.github.pavradev.dockerbay;

/**
 * Facade class for dockerbay library
 */
public class Dockerbay {

    public static DockerRule.DockerRuleBuilder getDockerRuleBuilder() {
        return DockerRule.builder()
                .withEnvironmentFactory(EnvironmentFactory.withDockerClientWrapper(new DockerClientImpl()));
    }
}