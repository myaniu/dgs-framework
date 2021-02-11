/*
 * Copyright 2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.netflix.graphql.dgs.metrics.micrometer

import com.netflix.graphql.dgs.metrics.micrometer.InstrumentationTags.sanitizeErrorPathsForTags
import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimpleInstrumentation
import graphql.execution.instrumentation.SimpleInstrumentationContext
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters
import graphql.schema.DataFetcher
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer

import org.springframework.boot.actuate.metrics.AutoTimer
import java.util.*
import java.util.concurrent.CompletableFuture

class GraphQLMetricsInstrumentation(
        private val registry: MeterRegistry,
        private val tagsProvider: GraphQLTagsProvider,
        private val autoTimer: AutoTimer
) : SimpleInstrumentation() {

    override fun createState(): InstrumentationState {
        return MetricsInstrumentationState(this.registry)
    }

    override fun beginExecution(parameters: InstrumentationExecutionParameters): InstrumentationContext<ExecutionResult> {
        val state: MetricsInstrumentationState = parameters.getInstrumentationState()
        state.startTimer()

        return object : SimpleInstrumentationContext<ExecutionResult>() {
            override fun onCompleted(result: ExecutionResult, exc: Throwable?) {
                state.stopTimer(autoTimer.builder("gql.query")
                        .tags(tagsProvider.getTags(parameters, result, exc))
                        .tags(tagsProvider.getEnvironmentTags()))
            }
        }
    }

    override fun instrumentExecutionResult(executionResult: ExecutionResult, parameters: InstrumentationExecutionParameters): CompletableFuture<ExecutionResult> {
        sanitizeErrorPathsForTags(executionResult).forEach { _ ->
            registry.counter("gql.error",
                    Tags.of(tagsProvider.getEnvironmentTags()).and(tagsProvider.getTags(parameters, executionResult, null)))
                    .increment()
        }
        return CompletableFuture.completedFuture(executionResult)
    }

    override fun instrumentDataFetcher(dataFetcher: DataFetcher<*>, parameters: InstrumentationFieldFetchParameters): DataFetcher<*> {
        val gqlField = InstrumentationTags.findDataFetcherTag(parameters)
        if (parameters.isTrivialDataFetcher || InstrumentationTags.shouldIgnored(gqlField)) {
            return dataFetcher
        }

        return DataFetcher { environment ->
            val tags = Tags.of("gql.field", gqlField).and(tagsProvider.getTags(parameters)).and(tagsProvider.getEnvironmentTags())
            registry.counter("gql.resolver.count", tags).increment()

            val sampler = Timer.start(registry)
            val result = dataFetcher.get(environment)
            if (result is CompletableFuture<*>) {
                result.thenAccept {
                    recordDataFetcherTime(sampler, tags, registry)
                }
            } else {
                recordDataFetcherTime(sampler, tags, registry)
            }
            result
        }
    }

    private fun recordDataFetcherTime(timerSampler: Timer.Sample, tags: Tags, registry: MeterRegistry) {
        timerSampler.stop(registry,
                Timer.builder("gql.resolver.time").tags(tags).publishPercentileHistogram())
    }

    class MetricsInstrumentationState(private val registry: MeterRegistry) : InstrumentationState {
        private var timerSample: Optional<Timer.Sample> = Optional.empty()

        fun startTimer() {
            this.timerSample = Optional.of(Timer.start(this.registry))
        }

        fun stopTimer(timer: Timer.Builder) {
            this.timerSample.map { it.stop(timer.register(this.registry)) }
        }
    }

}
