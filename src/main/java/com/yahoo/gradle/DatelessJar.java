/*
 * Copyright 2016, Yahoo Inc.
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
package com.yahoo.gradle;

import org.apache.tools.zip.ZipOutputStream;
import org.gradle.api.Incubating;
import org.gradle.api.internal.file.copy.DefaultZipCompressor;
import org.gradle.api.internal.file.copy.ZipCompressor;
import org.gradle.api.tasks.ParallelizableTask;
import org.gradle.api.tasks.bundling.ZipEntryCompression;
import org.gradle.jvm.tasks.Jar;

/**
 * Adapted from https://github.com/gradle/gradle/blob/ff0d36e210e25df9e391b536d861913d433e3ff1/subprojects/platform-jvm/src/main/java/org/gradle/jvm/tasks/Jar.java
 *  and https://github.com/gradle/gradle/blob/f15270245f55d63989ee4a26412ae663e177c609/subprojects/core/src/main/java/org/gradle/api/tasks/bundling/Zip.java
 */
@ParallelizableTask
@Incubating
public class DatelessJar extends Jar {
    protected ZipCompressor getCompressor() {
        ZipEntryCompression entryCompression = getEntryCompression();
        switch (entryCompression) {
            case DEFLATED:
                return new DatelessZipCompressor(isZip64(), ZipOutputStream.DEFLATED);
            case STORED:
                return new DefaultZipCompressor(isZip64(), ZipOutputStream.STORED);
            default:
                throw new IllegalArgumentException(String.format("Unknown Compression type %s", entryCompression));
        }
    }
}
