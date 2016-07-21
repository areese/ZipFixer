// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the New-BSD license. Please see LICENSE file in the project root for terms.
package com.yahoo.gradle;

import org.apache.tools.zip.Zip64Mode;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.zip.ZipEntry;

public class TestGradlePlugin {
    @Test
    public void testJarOutputStreamImplementation() {
        OutputStream o = new JarOutputStreamImplementation().createArchiveOutputStream(new File("build/tmp/testJarOutputStreamImplementation.jar"));
        Assert.assertNotNull(o);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testJarOutputStreamImplementationFail() {
        OutputStream o = new JarOutputStreamImplementation().createArchiveOutputStream(new File("/does/not/exist"));
        Assert.assertNotNull(o);
    }

    @Test
    public void testZipTime() throws Exception {
        DatelessZipOutputStream dzo = new DatelessZipOutputStream(new File("build/tmp/testZipTime.jar"));
        ZipEntry source = new ZipEntry("src/test/resources/input.jar");
        source.setMethod(java.util.zip.ZipEntry.DEFLATED);
        source.setLastModifiedTime(FileTime.from(Instant.now()));

        ZipEntry modified = dzo.fixTimes(source);

        Assert.assertEquals(modified.getComment(), source.getComment());
        Assert.assertEquals(modified.getInternalAttributes(), source.getInternalAttributes());
        Assert.assertEquals(modified.getPlatform(), source.getPlatform());
        Assert.assertEquals(modified.getExternalAttributes(), source.getExternalAttributes());
        Assert.assertEquals(modified.getMethod(), source.getMethod());
        Assert.assertEquals(modified.getSize(), source.getSize());
        Assert.assertEquals(modified.getCrc(), source.getCrc());
        Assert.assertEquals(modified.getCompressedSize(), source.getCompressedSize());
        Assert.assertEquals(modified.getCentralDirectoryExtra(), source.getCentralDirectoryExtra());
        Assert.assertEquals(modified.getLocalFileDataExtra(), source.getLocalFileDataExtra());
        Assert.assertEquals(modified.getGeneralPurposeBit(), source.getGeneralPurposeBit());

        Assert.assertNotEquals(modified.getTime(), source.getTime());
        Assert.assertEquals(modified.getTime(), 0);
    }

    @Test
    public void testDatelessZipCompressor() {
        DatelessZipCompressor dz = new DatelessZipCompressor(true, java.util.zip.ZipEntry.DEFLATED);
        DatelessZipOutputStream zos = (DatelessZipOutputStream) dz.createArchiveOutputStream(new File("build/tmp/testDatelessZipCompressor.jar"));

        Assert.assertEquals(zos.getUseZip64(), Zip64Mode.AsNeeded);
        Assert.assertEquals(zos.getMethod(), java.util.zip.ZipEntry.DEFLATED);
    }

// Still not sure how to test this part.
//    @Test
//    public void testDatelessJar() {
//        Project project = ProjectBuilder.builder().build();
//
//        Map<String, Class<DatelessJar>> m = new HashMap<>();
//
//        m.put("plugin", DatelessJar.class);
//
//        project.task(m ,"DatelessJar");
//        Task task = project.task("DatelessJar");
//
//        final List<Action<? super Task>> actions = task.getActions();
//        System.err.println(Arrays.toString(actions.toArray()));
//    }
}
