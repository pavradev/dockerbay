package com.github.pavradev.dockerbay;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Docker volume abstraction
 */
public class Volume {
    private static final Logger log = LoggerFactory.getLogger(Volume.class);

    private DockerClientWrapper dockerClient;

    private String name;

    private Volume(String name){
        this.name = name;
    }

    public static Volume withName(String name){
        return new Volume(name);
    }

    public void setDockerClient(DockerClientWrapper dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String getName(){
        return name;
    }

    public void create() {
        log.info("Creating volume {}", getName());
        dockerClient.createVolume(this);
    }

    public void delete() {
        log.info("Delete volume {}", getName());
        dockerClient.deleteVolume(this);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){
            return true;
        }
        if(!(obj instanceof Volume)){
            return false;
        }
        return Objects.equals(((Volume)obj).getName(), this.getName());
    }
}
