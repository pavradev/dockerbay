package com.github.pavradev.dockerbay;

import java.util.ArrayList;
import java.util.List;

import com.github.pavradev.dockerbay.exceptions.EnvironmentException;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/**
 * Junit Rule to wire docker environment
 */
public class DockerRule implements TestRule {

    private EnvironmentFactory environmentFactory;
    private Environment environment;
    private List<ContainerConfig> containers = new ArrayList<>();

    private DockerRule(EnvironmentFactory environmentFactory) {
        this.environmentFactory = environmentFactory;
    }

    public void setContainers(List<ContainerConfig> containers) {
        if (containers != null) {
            this.containers.addAll(containers);
        }
    }

    public Environment getEnvironment() {
        return this.environment;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        String envId = description.getTestClass().getSimpleName();
        if (description.getMethodName() != null) {
            envId += ("-" + description.getMethodName());
        }
        environment = environmentFactory.getWithId(envId);
        environment.setContainers(containers);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                List<Throwable> errors = new ArrayList<>();
                environment.initialize();
                if (Environment.Status.INITIALIZED.equals(environment.getStatus())) {
                    try {
                        statement.evaluate();
                    } catch (Throwable e) {
                        errors.add(e);
                    }
                } else {
                    errors.add(new EnvironmentException("Failed to init environment"));
                }
                environment.cleanup();
                MultipleFailureException.assertEmpty(errors);
            }
        };
    }

    public static DockerRuleBuilder builder() {
        return new DockerRuleBuilder();
    }

    public static class DockerRuleBuilder {
        private EnvironmentFactory environmentFactory;
        private List<ContainerConfig> containers = new ArrayList<>();

        private DockerRuleBuilder() {
        }

        public DockerRuleBuilder withEnvironmentFactory(EnvironmentFactory envFactory) {
            this.environmentFactory = envFactory;
            return this;
        }

        public DockerRuleBuilder addContainer(ContainerConfig container) {
            if (container != null) {
                this.containers.add(container);
            }
            return this;
        }

        public DockerRule build() {
            DockerRule dockerRule = new DockerRule(this.environmentFactory);
            dockerRule.setContainers(this.containers);
            return dockerRule;
        }
    }
}