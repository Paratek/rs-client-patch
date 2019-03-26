package io.paratek.patch;

import com.google.common.flogger.FluentLogger;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class JarHandler {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private final HashMap<String, ClassNode> classNodeMap = new HashMap<>();
    private final String inputPath;

    public JarHandler(final String path) {
        this.inputPath = path;
        try {
            this.loadLocal(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Write out the ClassNodes to a jar file
     * @param location
     */
    public void dumpTo(final String location) {
        try {
            JarOutputStream out = new JarOutputStream(new FileOutputStream(location));
            final JarFile original = new JarFile(this.inputPath);
            Enumeration entries = original.entries();
            byte[] buffer = new byte[512];
            while (entries.hasMoreElements()) {
                JarEntry entry = (JarEntry) entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    // Skip the key files cause we don't want those
                    if (entry.getName().contains("META-INF")) {
                        continue;
                    }
                    // There are no other non class files but for my sanity add them anyway
                    ZipEntry newEntry = new ZipEntry(entry.getName());
                    out.putNextEntry(newEntry);
                    InputStream in = original.getInputStream(entry);
                    while (0 < in.available()) {
                        int read = in.read(buffer);
                        if (read > 0) {
                            out.write(buffer, 0, read);
                        }
                    }
                    in.close();
                }
            }
            for (ClassNode cn : this.classNodeMap.values()) {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                cn.accept(cw);
                final ZipEntry newEntry = new ZipEntry(cn.name + ".class");
                out.putNextEntry(newEntry);
                out.write(cw.toByteArray());
                out.closeEntry();
            }
            // Add in our own MANIFEST
            final ZipEntry entry = new ZipEntry("META-INF/MANIFEST.MF");
            out.putNextEntry(entry);
            final InputStream in = this.getClass().getResourceAsStream("/MANIFEST.MF");
            for(int read = in.read(buffer); read > -1; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
            out.closeEntry();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



        /**
     * Opens the JarInputStream from a FileInputStream
     * @param jarFile
     * @throws IOException
     */
    private void loadLocal(final File jarFile) throws IOException {
        final JarInputStream inputStream = new JarInputStream(new FileInputStream(jarFile));
        readJarEntries(inputStream);
        logger.atInfo().log("JarHandler loaded from " + jarFile.getAbsolutePath() + " found " + this.getClassMap().size() + " classes");
    }

    /**
     * Reads the entries of the JarInputStream JarHandler#classNodeMap
     * @param inputStream
     * @throws IOException
     */
    private void readJarEntries(JarInputStream inputStream) throws IOException {
        JarEntry entry;
        while ((entry = inputStream.getNextJarEntry()) != null) {
            readEntryStream(inputStream, entry);
            inputStream.closeEntry();
        }
        inputStream.close();
    }


    /**
     * Reads JarEntry from JarInputStream
     * @param inputStream
     * @param entry
     * @throws IOException
     */
    private void readEntryStream(JarInputStream inputStream, JarEntry entry) throws IOException {
        if (entry.getName().endsWith(".class")) {
            final ClassReader reader = new ClassReader(inputStream);
            final ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.EXPAND_FRAMES);
            this.getClassMap().put(entry.getName()
                    .replace(".class", "")
                    .replace("/", "."), classNode);
        }
    }

    /**
     * Get the map of ClassNodes
     * @return
     */
    public HashMap<String, ClassNode> getClassMap() {
        return this.classNodeMap;
    }

}
