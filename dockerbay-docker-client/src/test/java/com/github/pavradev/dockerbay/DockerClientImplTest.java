package com.github.pavradev.dockerbay;

import org.junit.Before;

public class DockerClientImplTest {

    DockerClientWrapper dockerClientWrapper;

    @Before
    public void beforeMethod() {
        dockerClientWrapper = new DockerClientImpl();
    }
}
