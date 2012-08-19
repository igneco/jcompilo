package com.googlecode.compilo;

import com.googlecode.compilo.convention.AutoBuild;
import com.googlecode.shavenmaven.Dependencies;
import com.googlecode.totallylazy.Function1;
import com.googlecode.totallylazy.Option;
import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.Source;
import com.googlecode.yadic.SimpleContainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Properties;

import static com.googlecode.compilo.CompileProcessor.compile;
import static com.googlecode.compilo.Compiler.iterableSource;
import static com.googlecode.compilo.Environment.constructors.environment;
import static com.googlecode.totallylazy.Files.directory;
import static com.googlecode.totallylazy.Files.files;
import static com.googlecode.totallylazy.Files.hasSuffix;
import static com.googlecode.totallylazy.Files.name;
import static com.googlecode.totallylazy.Files.recursiveFiles;
import static com.googlecode.totallylazy.Files.relativePath;
import static com.googlecode.totallylazy.Predicates.where;
import static com.googlecode.totallylazy.Sequences.one;
import static com.googlecode.totallylazy.Strings.endsWith;
import static java.lang.System.nanoTime;

public class BootStrap {
    private final Environment env;
    private final File libDir;

    public BootStrap(Environment env) {
        this.env = env;
        libDir = directory(env.workingDirectory(), "lib");
    }

    public static void main(String[] args) throws Exception {
        System.exit(new BootStrap(environment()).build());
    }

    public int build() {
        long start = nanoTime();
        try {
            Option<File> buildFile = buildFile();
            env.out().printf("build: %s%n", buildFile.isEmpty() ? AutoBuild.class : buildFile.get());
            update();
            Class<?> buildClass = findBuildClass(buildFile);
            Build build = createBuildClass(buildClass);
            build.build();

            report("SUCCESSFUL", start);
            return 0;
        } catch (Exception e) {
            report(String.format("FAILED: %s %s%n", e.getClass().getName(), e.getMessage()), start);
            return -1;
        }

    }

    private void report(String message, long start) {
        env.out().println();
        env.out().println("BUILD " + message);
        env.out().printf("Total time: %s seconds%n", calculateSeconds(start));
    }

    private long calculateSeconds(long start) {
        return (nanoTime() - start) / 1000000000;
    }

    private void update() {
        Sequence<File> dependencies = files(directory(env.workingDirectory(), "build")).filter(hasSuffix("dependencies"));
        if (dependencies.isEmpty()) return;
        env.out().printf("update:%n");
        dependencies.mapConcurrently(new Function1<File, Boolean>() {
            @Override
            public Boolean call(File file) throws Exception {
                return Dependencies.load(file).update(directory(libDir, file.getName().replace(".dependencies", "")));
            }
        }).realise();
    }

    private Build createBuildClass(Class<?> aClass) throws Exception {
        return (Build) new SimpleContainer().
                addInstance(Environment.class, env).
                addInstance(File.class, env.workingDirectory()).
                addInstance(Properties.class, env.properties()).
                addInstance(PrintStream.class, env.out()).
                create(aClass);
    }

    public Class<?> findBuildClass(final Option<File> buildFile) throws Exception {
        return buildFile.map(new Function1<File, Class<?>>() {
            @Override
            public Class<?> call(File buildFile) throws Exception {
                String name = relativePath(env.workingDirectory(), buildFile);

                Sequence<File> libs = libs();
                final MemoryStore compiledBuild = MemoryStore.memoryStore();
                compile(libs,
                        fileSource(buildFile, name),
                        compiledBuild);

                ClassLoader loader = new ByteClassLoader(compiledBuild.data(), FileUrls.urls(libs));
                return loader.loadClass(className(name));

            }
        }).getOrElse(AutoBuild.class);

    }

    private Sequence<File> libs() {
        return recursiveFiles(libDir).filter(hasSuffix("jar")).realise().add(compiloJar());
    }

    private File compiloJar() {
        try {
            return new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Can't find compilo.jar");
        }
    }

    public Option<File> buildFile() {
        return files(env.workingDirectory()).
                find(where(name(), endsWith("uild.java")));
    }

    private static Source fileSource(File buildFile, String name) throws FileNotFoundException {
        return iterableSource(one(Pair.<String, InputStream>pair(name, new FileInputStream(buildFile))));
    }

    private static String className(String build) {
        return build.replace(".java", "").replace('.', '/');
    }
}