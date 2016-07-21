// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the New-BSD license. Please see LICENSE file in the project root for terms.
package com.yahoo.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestZipFixer {

    static final FileTime zeroTime = FileTime.from(0, TimeUnit.MILLISECONDS);

    /**
     * Ensure the times are correctly set to 0.
     */
    @Test
    public void testFixTime() {
        ZipEntry entryMock = new ZipEntry("name");
        entryMock.setTime(512);
        entryMock.setLastAccessTime(FileTime.from(1024, TimeUnit.SECONDS));
        entryMock.setLastModifiedTime(FileTime.from(2048, TimeUnit.SECONDS));
        entryMock.setCreationTime(FileTime.from(4096, TimeUnit.SECONDS));
        //Mockito.mock(ZipEntry.class);


        ZipEntry result = ZipFixer.fixTimes(entryMock);

        Assert.assertEquals(result.getTime(), 0, "time should be 0");
        Assert.assertEquals(result.getLastAccessTime(), zeroTime, "Access time should be 0");
        Assert.assertEquals(result.getLastModifiedTime(), zeroTime, "Last Modified time should be 0");
        Assert.assertEquals(result.getCreationTime(), zeroTime, "Creation time should be 0");
    }

    private static final class CountingJarOutputStream extends JarOutputStream {
        private int bytesCopied = 0;

        public CountingJarOutputStream(OutputStream os) throws IOException {
            super(os);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            bytesCopied += len;
        }

        public int getBytesCopied() {
            return bytesCopied;
        }
    }

    private static final class DummyInputStream extends InputStream {
        private final int maxBytes;
        private int atByte = 0;

        public DummyInputStream(int bytes) throws IOException {
            this.maxBytes = bytes;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int left = maxBytes - atByte;

            if (left <= 0) {
                // no more bytes to read.
                return -1;
            }

            // get the smallest amount we can read.
            int bytesToRead = Math.min(left, len);

            // adjust our read
            atByte += bytesToRead;

            return bytesToRead;
        }

        @Override
        public int read() throws IOException {
            return 0;
        }
    }

    @Test
    public void testDummyInputStream() throws IOException {
        try (DummyInputStream dis = new DummyInputStream(500)) {
            byte[] b = new byte[1024];
            Assert.assertEquals(dis.read(b), 500);
        }
    }

    /**
     * Test that if we copy a large entry, say 33k, that we actually copy that amount/
     *
     * @throws IOException on IO failure
     */
    @Test
    public void testCopyBytes() throws IOException {
        try (DummyInputStream dis = new DummyInputStream(500)) {
            OutputStream os = Mockito.mock(OutputStream.class);
            try (CountingJarOutputStream cos = new CountingJarOutputStream(os)) {
                ZipFixer.copyEntryBytes(dis, cos);
                Assert.assertEquals(cos.getBytesCopied(), 500);
            }
        }
    }

    @Test
    public void testCloses() throws IOException {
        ZipFixer zf = new ZipFixer("src/test/resources/input.jar", "build/resources/test/output.jar");
        zf.close();
    }

    @DataProvider
    public Object[][] inputStreams() {
        return new Object[][]{ //
                {null, null}, //
                {Mockito.mock(JarInputStream.class), null}, //
                {null, Mockito.mock(ZipFixingOutputStream.class)}, //
                {Mockito.mock(JarInputStream.class), Mockito.mock(ZipFixingOutputStream.class)}, //
        };
    }

    @Test(dataProvider = "inputStreams")
    public void testCloses(JarInputStream i, ZipFixingOutputStream o) throws IOException {
        ZipFixer zf = new ZipFixer(i, o);
        zf.close();
    }

    @Test
    public void testCopyEntry() throws IOException{
        JarInputStream jis = Mockito.mock(JarInputStream.class);
        ZipFixingOutputStream zos = Mockito.mock(ZipFixingOutputStream.class);

        Mockito.when(jis.read(Mockito.any(byte[].class))).thenReturn(-1);

        try (ZipFixer zf = new ZipFixer(jis, zos)) {
            ZipEntry mockEntry = Mockito.mock(ZipEntry.class);
            zf.copyEntry(mockEntry);

            Mockito.verify(zos).putNextEntry(mockEntry);
        }
    }

    @Test
    public void testNullManifest() throws IOException{
        JarInputStream jis = Mockito.mock(JarInputStream.class);
        ZipFixingOutputStream zos = Mockito.mock(ZipFixingOutputStream.class);

        Mockito.when(jis.getManifest()).thenReturn(null);
        Mockito.when(jis.read(Mockito.any(byte[].class))).thenReturn(-1);

        try (ZipFixer zf = new ZipFixer(jis, zos)) {
            zf.writeManifest();
        }
    }

    @Test
    public void testWriteManifest() throws IOException{
        JarInputStream jis = Mockito.mock(JarInputStream.class);
        ZipFixingOutputStream zos = Mockito.mock(ZipFixingOutputStream.class);

        Manifest manifestMock = Mockito.mock(Manifest.class);

        Mockito.when(jis.getManifest()).thenReturn(manifestMock);
        Mockito.when(jis.read(Mockito.any(byte[].class))).thenReturn(-1);

        try (ZipFixer zf = new ZipFixer(jis, zos)) {
            zf.writeManifest();

            Mockito.verify(zos).putNextEntry(Mockito.any(ZipEntry.class));
            Mockito.verify(manifestMock).write(zos);

        }
    }


    @Test
    public void testAdjust() throws IOException{
        Manifest manifestMock = Mockito.mock(Manifest.class);
        ZipEntry entryMock = Mockito.mock(ZipEntry.class);
        JarInputStream internalJis = Mockito.mock(JarInputStream.class);
        Mockito.when(internalJis.getManifest()).thenReturn(manifestMock);
        Mockito.when(internalJis.read(Mockito.any(byte[].class))).thenReturn(-1);
        Mockito.when(internalJis.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenReturn(-1);
        Mockito.when(internalJis.getNextEntry()).thenReturn(entryMock);


        JarEntry jarMock = Mockito.mock(JarEntry.class);
        DummyJarInputStream jis = new DummyJarInputStream(internalJis, 1, jarMock);
        ZipFixingOutputStream zos = Mockito.mock(ZipFixingOutputStream.class);


        try (ZipFixer zf = new ZipFixer(jis, zos)) {
            zf.adjustDatesToEpoch();

            Mockito.verify(zos, Mockito.times(2)).putNextEntry(Mockito.any(ZipEntry.class));
            Mockito.verify(manifestMock).write(zos);
        }
    }

    static final class DummyJarInputStream extends JarInputStream {
        private int counter = 0;
        private final int max;
        private final JarEntry mock;
        private final JarInputStream jis;

        public DummyJarInputStream(JarInputStream in, int max, JarEntry mock) throws IOException {
            super(in);
            this.max = max;
            this.mock = mock;
            this.jis = in;
        }

        @Override
        public ZipEntry getNextEntry() throws IOException {
            if (counter++ < max) {
                return mock;
            }

            return null;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return jis.read(b);
        }

        @Override
        public Manifest getManifest() {
            return jis.getManifest();
        }
    }
}
