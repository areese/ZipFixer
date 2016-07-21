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

import org.apache.commons.io.IOUtils;
import org.apache.tools.zip.Zip64Mode;
import org.apache.tools.zip.ZipOutputStream;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.internal.file.copy.DefaultZipCompressor;
import org.gradle.api.internal.file.copy.ZipCompressor;

import java.io.File;
import java.io.IOException;

/**
 * Adapted from https://github.com/gradle/gradle/blob/c8143c9bcf412a2d19053b8c926c7787c1dc8087/subprojects/core/src/main/java/org/gradle/api/internal/file/copy/DefaultZipCompressor.java
 */
public class DatelessZipCompressor implements ZipCompressor {
    private final int entryCompressionMethod;
    private final Zip64Mode zip64Mode;

    public DatelessZipCompressor(boolean allowZip64Mode, int entryCompressionMethod) {
        this.entryCompressionMethod = entryCompressionMethod;
        this.zip64Mode = allowZip64Mode ? Zip64Mode.AsNeeded : Zip64Mode.Never;
    }

    @Override
    public ZipOutputStream createArchiveOutputStream(File destination) {
        try {
            ZipOutputStream e = new DatelessZipOutputStream(destination);
            e.setUseZip64(this.zip64Mode);
            e.setMethod(this.entryCompressionMethod);
            return e;
        } catch (Exception e) {
            throw new UncheckedIOException("Unable to create ZIP output stream for file " + destination);
        }
    }
}
