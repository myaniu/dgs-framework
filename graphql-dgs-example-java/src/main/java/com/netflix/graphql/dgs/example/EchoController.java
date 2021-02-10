/*
 * Copyright 2021 Netflix, Inc.
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

package com.netflix.graphql.dgs.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * You can still use Spring MVC's components. For example, this class
 * defines a simple REST {@code /echo} endpoint.
 */
@RestController
public class EchoController {
    private static final String template = "Hello, %s!";

    @GetMapping("/echo")
    public EchoReply greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return new EchoReply(String.format(template, name));
    }

    public static class EchoReply {
        private final String reply;

        private EchoReply(String content) {
            this.reply = content;
        }

        public String getReply() {
            return reply;
        }
    }
}