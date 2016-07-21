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

import org.apache.tools.zip.Zip64Mode;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;

/**
 * Adapted from http://grepcode.com/file/repo1.maven.org/maven2/org.apache.ant/ant/1.9.6/org/apache/tools/zip/ZipOutputStream.java
 * <p>
 * This overrides putNextEntry to force the time to be 0.
 */
public class DatelessZipOutputStream extends ZipOutputStream {
    private Zip64Mode mode;
    private int method;

    public DatelessZipOutputStream(File destination) throws IOException {
        super(destination);
    }

    @Override
    public void putNextEntry(ZipEntry archiveEntry) throws IOException {
        ZipEntry modified = fixTimes(archiveEntry);
        super.putNextEntry(modified);
    }

    /**
     * Mirrors fixTimes in {@link ZipFixer}, however takes an org.apache.tools.zip.ZipEntry instead. yay ant
     *
     * @param archiveEntry
     * @return
     */
    ZipEntry fixTimes(ZipEntry archiveEntry) throws ZipException {
        ZipEntry modified = new ZipEntry(archiveEntry);
        // The JDK has last modified, and access time, but ant only has setTime which maps to lastModified.
        modified.setTime(0);

        return modified;
    }

    @Override
    public void setUseZip64(Zip64Mode mode) {
        super.setUseZip64(mode);
        this.mode = mode;
    }

    public Zip64Mode getUseZip64() {
        return mode;
    }

    @Override
    public void setMethod(int method) {
        super.setMethod(method);
        this.method = method;
    }

    int getMethod() {
        return method;
    }
}
