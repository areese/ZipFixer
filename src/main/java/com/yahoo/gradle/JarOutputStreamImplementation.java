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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.jar.JarOutputStream;

import org.gradle.api.internal.file.archive.compression.ArchiveOutputStreamFactory;

public class JarOutputStreamImplementation implements ArchiveOutputStreamFactory {

    public OutputStream createArchiveOutputStream(File destination) {
        try {
            return new ZipFixingOutputStream(new FileOutputStream(destination));
        } catch (Exception e) {
            throw new RuntimeException("Unable to create output stream for file " + destination);
        }
    }
}
