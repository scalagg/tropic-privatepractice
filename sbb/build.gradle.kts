plugins {
    id("io.freefair.lombok") version "5.3.0"
}

dependencies {
    compileOnly(project(":game"))

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
}

