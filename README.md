# A Microservice to create Fractals

Currently, only the Mandelbrot set is supported. There's an endpoint that generates fractals,
and the UI allows you to zoom in, undo, and change color schemes.

This is intended to be a modern
[Pet Store](https://www.oracle.com/java/technologies/petstore-v1312.html)-type app which
will be the basis to explore modern best practices.

## Requirements

The app is written in Kotlin on JDK 21, using the [Micronaut framework](https://micronaut.io/).

To get started, beyond knowing Kotlin, you need

* Java 21
* Maven 3.9.x -- this will fetch Kotlin, Micronaut and other dependencies

The [Micronaut CLI](https://github.com/micronaut-projects/micronaut-starter/releases/) is optional but helpful.

To run this on your system, type `mvn mn:run`. It'll run on port 8080 by default, you can
[change this](https://docs.micronaut.io/latest/guide/#runningSpecificPort).
