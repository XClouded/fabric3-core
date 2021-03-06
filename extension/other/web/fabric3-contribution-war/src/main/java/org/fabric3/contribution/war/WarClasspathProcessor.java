/*
 * Fabric3
 * Copyright (c) 2009-2015 Metaform Systems
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
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 */
package org.fabric3.contribution.war;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.api.host.runtime.HostInfo;
import org.fabric3.api.host.util.IOHelper;
import org.fabric3.spi.contribution.Contribution;
import org.fabric3.spi.contribution.ClasspathProcessor;
import org.fabric3.spi.contribution.ClasspathProcessorRegistry;
import org.fabric3.spi.model.os.Library;
import org.oasisopen.sca.annotation.Destroy;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Init;
import org.oasisopen.sca.annotation.Reference;

/**
 * Creates a classpath based on the contents of a WAR. Specifically, adds jars contained in WEB-INF/lib and classes in WEB-INF/classes to the classpath.
 */
@EagerInit
public class WarClasspathProcessor implements ClasspathProcessor {
    private static final Random RANDOM = new Random();

    private final ClasspathProcessorRegistry registry;
    private HostInfo info;

    public WarClasspathProcessor(@Reference ClasspathProcessorRegistry registry, @Reference HostInfo info) {
        this.registry = registry;
        this.info = info;
    }

    @Init
    public void init() {
        registry.register(this);
    }

    @Destroy
    public void destroy() {
        registry.unregister(this);
    }

    public boolean canProcess(Contribution contribution) {
        URL url = contribution.getLocation();
        String name = url.getFile().toLowerCase();
        return name.endsWith(".war");
    }

    public List<URL> process(Contribution contribution) throws Fabric3Exception {
        URL url = contribution.getLocation();
        List<Library> libraries = contribution.getManifest().getLibraries();

        List<URL> classpath = new ArrayList<>();
        // add the the jar itself to the classpath
        classpath.add(url);

        if (libraries.isEmpty() && !hasLibDirectory(new File(url.getFile()), "lib") && !hasLibDirectory(new File(url.getFile()), "classes")) {
            return classpath;
        }

        try {
            // add libraries from the jar
            addLibraries(classpath, url);
        } catch (IOException e) {
            throw new Fabric3Exception(e);
        }
        return classpath;
    }

    private void addLibraries(List<URL> classpath, URL jar) throws IOException {
        File dir = info.getTempDir();
        try (InputStream is = jar.openStream()) {
            JarInputStream jarStream = new JarInputStream(is);
            JarEntry entry;
            File classesDir = null;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String path = entry.getName();
                if (path.startsWith("WEB-INF/lib/")) {
                    // expand jars in WEB-INF/lib and add to the classpath
                    File jarFile = File.createTempFile("fabric3", ".jar", dir);
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(jarFile))) {
                        copy(jarStream, os);
                        os.flush();
                    }
                    jarFile.deleteOnExit();
                    classpath.add(jarFile.toURI().toURL());
                } else if (path.startsWith("WEB-INF/classes/")) {
                    // expand classes in WEB-INF/classes and add to classpath
                    if (classesDir == null) {
                        classesDir = new File(dir, "webclasses" + RANDOM.nextInt());
                        classesDir.mkdir();
                        classesDir.deleteOnExit();
                    }
                    int lastDelimeter = path.lastIndexOf("/");
                    String name = path.substring(lastDelimeter);
                    File pathAndPackageName;
                    if (lastDelimeter < 16) { // 16 is length of "WEB-INF/classes
                        // in case there is no trailing '/', i.e. properties files or other resources under WEB_INF/classes
                        pathAndPackageName = classesDir;
                    } else {
                        pathAndPackageName = new File(classesDir, path.substring(16, lastDelimeter));
                    }
                    pathAndPackageName.mkdirs();
                    pathAndPackageName.deleteOnExit();
                    File classFile = new File(pathAndPackageName, name);
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(classFile))) {
                        copy(jarStream, os);
                        os.flush();
                    }
                    classFile.deleteOnExit();
                    classpath.add(classesDir.toURI().toURL());
                }
            }
        }
    }

    private boolean hasLibDirectory(File file, String dir) {
        InputStream stream = null;
        try {
            URL jarUrl = new URL("jar:" + file.toURI().toURL().toExternalForm() + "!/WEB-INF/" + dir);
            stream = jarUrl.openStream();
            return true;
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                IOHelper.closeQuietly(stream);
            } catch (NullPointerException e) {
                // ignore will be thrown if the directory exists as the underlying stream is null
            }
        }
    }

    private int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

}