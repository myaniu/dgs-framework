/*
 * Copyright 2020 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    application
    kotlin("jvm")
    id("org.springframework.boot") version "2.4.2"
}

application {
    mainClass.set("com.netflix.graphql.dgs.example.ExampleApp")
}

dependencies {
    implementation(project(":graphql-dgs-spring-boot-starter"))
    implementation(project(":graphql-dgs-subscriptions-websockets-autoconfigure"))
    implementation(project(":graphql-dgs-spring-boot-micrometer"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-actuator-autoconfigure")
    implementation("io.micrometer:micrometer-core")

    implementation("io.projectreactor:reactor-core:3.4.0")
}

// Disable Spring Boot Layering
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    layered {
        isEnabled = false
    }
}