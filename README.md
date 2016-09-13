# dockerbay [![Build Status](https://travis-ci.org/pavradev/dockerbay.svg?branch=master)](https://travis-ci.org/pavradev/dockerbay)
**It is** Java library for component testing  
**It does** orchestrate docker environment  
**It uses** JUnit and [Java Docker Client](https://github.com/docker-java/docker-java)  
Tested with docker version 1.11 on Linux
##Building
```bash
mvn clean install
```
Requires Java 8
##Using
With maven:
```xml
    <dependency>
        <groupId>com.github.pavradev</groupId>
        <artifactId>dockerbay</artifactId>
          <version>1.0.0</version>
        <scope>test</scope>
    </dependency>
```
```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

##Examples
Check [dockerbay-demo](https://github.com/pavradev/dockerbay-demo) project
