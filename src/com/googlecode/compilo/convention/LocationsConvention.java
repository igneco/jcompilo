package com.googlecode.compilo.convention;

import com.googlecode.compilo.Environment;
import com.googlecode.compilo.Locations;

import java.io.File;

import static com.googlecode.totallylazy.Files.directory;
import static com.googlecode.totallylazy.Files.file;
import static java.lang.String.format;

public abstract class LocationsConvention extends IdentifiersConvention implements Locations {
    protected LocationsConvention(Environment environment) { super(environment); }

    public File rootDir() { return env.workingDirectory(); }
    public File artifactsDir() { return directory(rootDir(), "build/artifacts"); }
    public File srcDir() { return directory(rootDir(), "src"); }
    public File testDir() { return directory(rootDir(), "test"); }
    public File libDir() { return directory(rootDir(), "lib"); }
    public File mainJar() { return file(artifactsDir(), format("%s.jar", versionedArtifact())); }
    public File sourcesJar() { return file(artifactsDir(), format("%s-sources.jar", versionedArtifact())); }
    public File testJar() { return file(artifactsDir(), format("%s-tests.jar", versionedArtifact())); }
    public File testSourcesJar() { return file(artifactsDir(), format("%s-test-sources.jar", versionedArtifact())); }
}
