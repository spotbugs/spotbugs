/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.transform;

import com.google.common.collect.ImmutableList;
import org.gradle.internal.execution.fingerprint.InputFingerprinter;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;

@ThreadSafe
public interface TransformerInvocationFactory {
    /**
     * Returns an invocation which allows invoking the actual transformer.
     */
    CacheableInvocation<ImmutableList<File>> createInvocation(
        Transformer transformer,
        File inputArtifact,
        ArtifactTransformDependencies dependencies,
        TransformationSubject subject,
        InputFingerprinter inputFingerprinter);
}
