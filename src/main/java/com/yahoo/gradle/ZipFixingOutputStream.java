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

import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Created by areese on 6/29/16.
 *
 * This replaces a {@link JarOutputStream} and resets the time to epoch or a configured time as each entry is written.
 */
public class ZipFixingOutputStream extends JarOutputStream {
    public ZipFixingOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    @Override
    public void putNextEntry(ZipEntry ze) throws IOException {
        ze = ZipFixer.fixTimes(ze);

        super.putNextEntry(ze);
    }
}
