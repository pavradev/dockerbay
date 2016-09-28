package com.github.pavradev.dockerbay;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.InOrder;

public class DockerRuleTest {

    private DockerRule dockerRule;
    private Environment env;

    Description description;
    Statement baseStatement;

    @Before
    public void beforeEachTest(){
        env = mock(Environment.class);
        EnvironmentFactory envFactoryMock = mock(EnvironmentFactory.class);
        doReturn(env).when(envFactoryMock).makeEnvironment(anyString());

        dockerRule = DockerRule.withEnvironmentFactory(envFactoryMock);

        description = mock(Description.class);
        doReturn(DockerRuleTest.class).when(description).getTestClass();
        doReturn("dummyMethod").when(description).getMethodName();
        baseStatement = mock(Statement.class);
    }

    @Test
    public void shouldInitializeEnvBeforeTest() throws Throwable {
        doReturn(Environment.EnvironmentState.INITIALIZED).when(env).getState();
        Statement statement = dockerRule.apply(baseStatement, description);
        statement.evaluate();

        InOrder inOrder = inOrder(env, baseStatement);
        inOrder.verify(env).initialize();
        inOrder.verify(baseStatement).evaluate();
    }

    @Test
    public void shouldCleanupEnvAfterTest() throws Throwable {
        doReturn(Environment.EnvironmentState.INITIALIZED).when(env).getState();
        Statement statement = dockerRule.apply(baseStatement, description);
        statement.evaluate();

        InOrder inOrder = inOrder(env, baseStatement);
        inOrder.verify(baseStatement).evaluate();
        inOrder.verify(env).cleanup();
    }

    @Test
    public void shouldSkipTestIfEnvFailedToInitialize() throws Throwable {
        doReturn(Environment.EnvironmentState.PARTIALLY_INITIALIZED).when(env).getState();
        Statement statement = dockerRule.apply(baseStatement, description);

        try {
            statement.evaluate();
            assertTrue("Should produce exception", false);
        } catch (Throwable e){
        }

        verify(baseStatement, never()).evaluate();
        verify(env).cleanup();
    }
}
