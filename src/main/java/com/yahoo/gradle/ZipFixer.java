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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * This class takes an input Jarfile, and changes all of the access/lastmodified times to 0. This allows one to take 2
 * jar files, and compare the sha1sum's of them, resulting in idempotent output. There are 2 things I've seen so far
 * that will cause multiple builds of the same source to generate different jar files:
 * <p>
 * <p>
 * <ol>
 * <li>Maven builds contain a pom.properties with a date in it:
 * <p>
 * <pre>
 * <code>
 * bash-4.2 $ unzip -p ../JmhQuestion/target/original-benchmarks.jar META-INF/maven/com.yahoo.areese/JmhQuestion/pom.properties
 * #Generated by Maven
 * #Thu May 12 20:25:53 PDT 2016
 * version=1.0 groupId=com.yahoo.areese
 * artifactId=JmhQuestion
 * </code>
 * </pre>
 * <p>
 * </li>
 * <p>
 * <li>The zip file contains last modified/access time entries in it:
 * <p>
 * <pre>
 * <code>
 * bash-4.2$ unzip -l build/libs/ZipFixer.jar
 * Archive:  build/libs/ZipFixer.jar
 * Length     Date   Time    Name
 * --------    ----   ----    ----
 * 0  06-27-16 14:03   META-INF/
 * 25  06-27-16 13:59   META-INF/MANIFEST.MF
 * 0  06-27-16 14:03   A/
 * 3945  06-27-16 14:03   A/ZipFixer.class
 * --------                   -------
 * 3970                   4 files
 * </code>
 * </pre>
 * <p>
 * </li>
 * </ol>
 * <p>
 * This class will fix the last modified and access time dates within a jar to be 12/31/1969 00:00:00.
 * <p>
 * <pre>
 * <code>
 * bash-4.2 $ java -cp build/libs/ZipFixer.jar  A.ZipFixer  build/libs/ZipFixer.jar fixed-ZipFixer.jar
 * Writing META-INF/MANIFEST.MF
 * Writing A/ZipFixer.class
 * bash-4.2 $ unzip -l fixed-ZipFixer.jar
 * Archive:  fixed-ZipFixer.jar
 * Length     Date   Time    Name
 * --------    ----   ----    ----
 * 25  12-31-69 16:00   META-INF/MANIFEST.MF
 * 3945  12-31-69 16:00   A/ZipFixer.class
 * --------                   -------
 * 3970                   2 files
 * bash-4.2 $
 * </code>
 * </pre>
 */
public class ZipFixer implements Closeable {

    static final FileTime time0 = FileTime.from(0, TimeUnit.MILLISECONDS);

    private JarInputStream jis;
    private ZipFixingOutputStream zos;
    private final boolean debug = false;

    public ZipFixer(String inputFile, String outputFile) throws FileNotFoundException, IOException {
        this(new File(inputFile), new File(outputFile));
    }

    public ZipFixer(File inputFile, File outputFile) throws FileNotFoundException, IOException {
        Objects.requireNonNull(inputFile);
        Objects.requireNonNull(outputFile);
        jis = new JarInputStream(new FileInputStream(inputFile));
        zos = new ZipFixingOutputStream(new FileOutputStream(outputFile));
    }

    ZipFixer(JarInputStream i, ZipFixingOutputStream o) {
        this.jis = i;
        this.zos = o;
    }

    public void adjustDatesToEpoch() throws FileNotFoundException, IOException {
        // we want the manifest first, so we can give it to the outputstream
        // but you have to call getManifest after you call nextJarEntry
        // since we're changing the access times of the entry we can't pass it to the constructor,
        // so we'll have to manually add it.
        writeManifest();

        // The first entry should be the manifest
        JarEntry nextJarEntry;
        while (null != (nextJarEntry = jis.getNextJarEntry())) {
            // outputstream will fix the times for us.
            copyEntry(nextJarEntry);
        }

        close();
    }

    void writeManifest() throws IOException {
        Manifest manifest = jis.getManifest();

        // it's entirely possible we have no manifest.
        if (null != manifest) {
            // Important, MANIFEST must be the first (or second) entry, or the next person reading the jarfile
            // will trip.
            // see JarInputStream, it takes care of META-INF as first, or MANIFEST.MF as first.
            ZipEntry ze = new ZipEntry(JarFile.MANIFEST_NAME);

            // we can't use copyEntry here because we need to tell the manifest to write itself out.
            zos.putNextEntry(ze);
            manifest.write(zos);
        }
    }

    /**
     * Copies input bytes to output 4k at a time.
     *
     * @param jis inputstream
     * @param jos outputstream
     * @throws IOException
     */
    static void copyEntryBytes(InputStream jis, OutputStream jos) throws IOException {
        byte[] buffer = new byte[4096];
        int len = 0;
        while (-1 != (len = jis.read(buffer))) {
            jos.write(buffer, 0, len);
        }
    }

    /**
     * Adjusts the access and last modified times of newEntry, and then writes that meta data, and the bytes for the
     * entry to the outputstream.
     *
     * @param newEntry {@link ZipEntry} that contains the metadata.
     * @throws IOException
     */
    void copyEntry(ZipEntry newEntry) throws IOException {
        if (debug) {
            // lazy man's debugging.
            System.out.println("Writing " + newEntry.getName());
        }

        // outputstream will fix the times for us.
        zos.putNextEntry(newEntry);

        // however, we still need to shuttle the bytes across
        copyEntryBytes(jis, zos);
    }

    /**
     * Set the access and last modified times of a {@link ZipEntry} to 0. (12/31/1969 00:00:00).
     *
     * @param entry {@link ZipEntry} input
     * @return ZipEntry with time set to zero.
     */
    public static ZipEntry fixTimes(ZipEntry entry) {
        ZipEntry newEntry = new ZipEntry(entry);
        newEntry.setTime(0);
        newEntry.setLastModifiedTime(time0);
        if (null != entry.getLastAccessTime()) {
            newEntry.setLastAccessTime(time0);
        }
        if (null != entry.getCreationTime()) {
            newEntry.setCreationTime(time0);
        }

        return newEntry;
    }


    public static void main(String[] args) throws FileNotFoundException, IOException {
        if (args.length < 2) {
            printHelp();
            System.exit(-1);
        }

        try (ZipFixer zf = new ZipFixer(args[0], args[1])) {
            zf.adjustDatesToEpoch();
        }
    }

    private static void printHelp() {
        System.err.println("JarFixer usage: ");
        System.err.println("JarFixer <inputJar> <outputJar>");
    }

    @Override
    public void close() throws IOException {
        if (null != jis) {
            jis.close();
            jis = null;
        }

        if (null != zos) {
            zos.close();
            zos = null;
        }
    }
}