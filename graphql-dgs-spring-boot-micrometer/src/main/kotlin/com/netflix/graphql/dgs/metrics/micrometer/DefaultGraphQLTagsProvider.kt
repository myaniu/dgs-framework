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

package com.netflix.graphql.dgs.metrics.micrometer

import graphql.ExecutionResult
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags

class DefaultGraphQLTagsProvider : GraphQLTagsProvider {

    companion object {
        val OUTCOME_SUCCESS: Tag = Tag.of("outcome", "SUCCESS")
        val OUTCOME_ERROR: Tag = Tag.of("outcome", "ERROR")
    }


    override fun getTags(parameters: InstrumentationExecutionParameters,
                         result: ExecutionResult,
                         exception: Throwable?): Iterable<Tag> {

        return if (result.errors.isNotEmpty()) {
            InstrumentationTags.sanitizeErrorPathsForTags(result).map {
                Tags.of("gql.errorCode", it.value.first,
                        "gql.path", it.key,
                        "gql.errorDetail", it.value.second)
            }.fold(Tags.of(OUTCOME_ERROR)) { acc, a -> acc.and(a) }
        } else {
            Tags.of(OUTCOME_SUCCESS)
        }
    }
}
