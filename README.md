# dockerbay [![Build Status](https://travis-ci.org/pavradev/dockerbay.svg?branch=master)](https://travis-ci.org/pavradev/dockerbay)
Docker based component test library

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
