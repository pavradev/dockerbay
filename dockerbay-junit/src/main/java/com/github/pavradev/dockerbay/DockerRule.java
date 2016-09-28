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

    private DockerRule(EnvironmentFactory dockerFactory) {
        this.environmentFactory = dockerFactory;
    }

    public static DockerRule withEnvironmentFactory(EnvironmentFactory environmentFactory){
        return new DockerRule(environmentFactory);
    }

    public Environment getEnvironment() {
        return this.environment;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        String id = extractId(description);
        environment = environmentFactory.makeEnvironment(id);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                List<Throwable> errors = new ArrayList<>();
                environment.initialize();
                if (Environment.EnvironmentState.INITIALIZED.equals(environment.getState())) {
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

    private String extractId(Description description) {
        String id = description.getTestClass().getSimpleName();
        if (description.getMethodName() != null) {
            id += ("-" + description.getMethodName());
        }
        return id;
    }

}