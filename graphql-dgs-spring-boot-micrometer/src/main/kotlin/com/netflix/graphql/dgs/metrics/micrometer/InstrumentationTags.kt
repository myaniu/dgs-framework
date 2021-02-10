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
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.validation.ValidationError

object InstrumentationTags {
    private val instrumentationIgnores = setOf("__typename", "__Schema", "__Type")

    fun findDataFetcherTag(parameters: InstrumentationFieldFetchParameters): String {
        val type = parameters.executionStepInfo.parent.type
        val parentType = if (type is GraphQLNonNull) {
            type.wrappedType as GraphQLObjectType
        } else {
            type as GraphQLObjectType
        }
        return "${parentType.name}.${parameters.executionStepInfo.path.segmentName}"
    }

    fun shouldIgnored(tag: String): Boolean {
        return instrumentationIgnores.find { tag.contains(it) } != null
    }

    fun sanitizeErrorPathsForTags(executionResult: ExecutionResult) : Map<String, Pair<String, String>> {
        var dedupeErrorPaths: Map<String, Pair<String,String>> = emptyMap()
        executionResult.errors.forEach {
            var errorPath: List<Any> = emptyList()
            val errorDetail: String
            val errorType: String
            if (it is ValidationError) {
                errorPath = it.queryPath ?: emptyList()
                errorType = it.errorType?.toString()?:""
                errorDetail = it.extensions?.get("errorDetail")?.toString()?:"none"
            } else {
                if (!it.path.isNullOrEmpty()) errorPath = it.path
                errorType = it.extensions?.get("errorType")?.toString() ?: ""
                errorDetail = it.extensions?.get("errorDetail")?.toString() ?: "none"
            }
            val sanitizedErrors = errorPath.map { iter ->
                if (iter.toString().toIntOrNull() != null) "number"
                else iter.toString()
            }
            // in case of batch loaders, eliminate duplicate instances of the same error at different indices
            if (!dedupeErrorPaths.contains(sanitizedErrors.toString())) {
                dedupeErrorPaths = dedupeErrorPaths.plus(Pair(sanitizedErrors.toString(), Pair(errorType, errorDetail)))
            }
        }
        return dedupeErrorPaths
    }
}
