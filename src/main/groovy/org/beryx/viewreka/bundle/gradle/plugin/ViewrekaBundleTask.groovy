package org.beryx.viewreka.bundle.gradle.plugin

import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.codehaus.plexus.util.SelectorUtils
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import java.util.jar.JarFile

class ViewrekaBundleTask extends ShadowJar implements ViewrekaBundleSpec {
    public static final String DEFAULT_EXTENSION = 'vbundle'
    public static final VIEWREKA_PROVIDED_PATHS = [
            'org/beryx/viewreka/**',
    ]
    public static final String ATTR_BUNDLE_CLASS = 'viewrekaBundleClass'
    public static final String ATTR_VIEWREKA_VERSION_MAJOR = 'viewrekaVersionMajor'
    public static final String ATTR_VIEWREKA_VERSION_MINOR = 'viewrekaVersionMinor'
    public static final String ATTR_VIEWREKA_VERSION_PATCH = 'viewrekaVersionPatch'

    @Input String bundleClass

    // The minimum Viewreka version required by the bundle
    @Input int viewrekaVersionMajor = -1
    @Input int viewrekaVersionMinor = -1
    @Input @Optional int viewrekaVersionPatch

    @Input @Optional String relocationPrefix

    private final Set<String> relocationExcludes = []
    private final Set<String> relocationExtraIncludes = []

    public ViewrekaBundleTask() {
        extension = DEFAULT_EXTENSION
        dependencies {
            exclude(dependency('org.slf4j:slf4j-api:.*'))
            exclude(dependency('ch.qos.logback:logback-classic:.*'))
            exclude(dependency('ch.qos.logback:logback-core:.*'))
            exclude(dependency('org.codehaus.groovy:groovy-all:.*'))
        }
        addValidator { task, messages ->
            if(task.viewrekaVersionMajor < 0) messages << "No value has been specified for property 'viewrekaVersionMajor'."
            if(task.viewrekaVersionMinor < 0) messages << "No value has been specified for property 'viewrekaVersionMinor'."
        }
    }

    @Override
    public ViewrekaBundleTask relocationExclude(String... paths) {
        relocationExcludes.addAll paths
        return this
    }

    @Override
    public ViewrekaBundleTask relocationExtraInclude(String... paths) {
        relocationExtraIncludes.addAll paths
        return this
    }

    @Override
    protected void copy() {
        long startTime = System.currentTimeMillis()

        super.copy()

        logger.debug "copy() done in ${System.currentTimeMillis() - startTime} ms."
    }

    @Override
    protected CopyAction createCopyAction() {
        long startTime = System.currentTimeMillis()

        configureRelocation()
        configureBundleClass()

        def result = super.createCopyAction()

        logger.debug "creatCopyAction() done in ${System.currentTimeMillis() - startTime} ms."
        return result
    }

    private void configureBundleClass() {
        manifest.attributes[ATTR_BUNDLE_CLASS] = bundleClass
        manifest.attributes[ATTR_VIEWREKA_VERSION_MAJOR] = viewrekaVersionMajor
        manifest.attributes[ATTR_VIEWREKA_VERSION_MINOR] = viewrekaVersionMinor
        manifest.attributes[ATTR_VIEWREKA_VERSION_PATCH] = viewrekaVersionPatch
    }

    private void configureRelocation() {
        long startTime = System.currentTimeMillis();

        if(relocationPrefix == null) {
            relocationPrefix = project.name
            if(project.group) {
                relocationPrefix = "${project.group}.$relocationPrefix"
            }
            relocationPrefix += '.shaded'
        }
        relocationPrefix = toValidRelocator(relocationPrefix)

        excludes.addAll(VIEWREKA_PROVIDED_PATHS)

        if(relocationPrefix) {
            relocationExcludes.addAll(excludes)
            relocators.findAll{it instanceof SimpleRelocator}.each {SimpleRelocator rel -> relocationExclude(rel.excludes as String[])}

            logger.debug "Excludes: $excludes"
            logger.debug "relocationExcludes: $relocationExcludes"

            logger.debug "Entries:"

            TreeSet<String> dirEntries = [] + relocationExtraIncludes
            includedDependencies.files.each {f ->
                logger.debug "\tdependency $f.name"
                def jar = new JarFile(f)
                jar.entries().each {entry ->
                    logger.debug "\t\tentry $entry.name"
                    if(!entry.directory && entry.name.endsWith('.class')) {
                        def cls = entry.name - '.class'
                        def idx = cls.lastIndexOf('/')
                        if(idx > 0) {
                            TreeSet<String> oldDirEntries = [] + dirEntries
                            def dir = cls.substring(0, idx)
                            dirEntries.removeIf {it.startsWith "$dir/"}
                            if(!dirEntries.find {dir.startsWith "$it/"}) {
                                dirEntries << dir
                            }
                            if(oldDirEntries != dirEntries) {
                                logger.debug "\t\t\tupdated dirEntries: $dirEntries"
                            }
                        }
                    }
                }
            }
            for(String dir : dirEntries) {
                if(!isRelocationExcluded(dir)) {
                    def pattern = dir.replaceAll('/', '.')
                    logger.debug "\t\t\tadded: $pattern"
                    relocators << new SimpleRelocator(pattern, "${relocationPrefix}.$pattern" as String, null, relocationExcludes as List)
                } else {
                    logger.debug "\t\t\texcluded: $dir"
                }
            }
        }

        logger.debug "configureRelocation() done in ${System.currentTimeMillis() - startTime} ms."
    }

    boolean isRelocationExcluded(String path) {
        relocationExcludes.find{SelectorUtils.matchPath(it, path, true)}
    }

    private static String toValidRelocator(String s) {
        if(!s) return s
        def sb = new StringBuilder()
        if(isNotPackageStart(s[0])) sb << '_'
        s.each {sb << (isPackagePart(it) ? it : '_')}

        // remove leading and trailing '.' characters
        def rel = sb.toString() - ~/^\.+/ - ~/\.+$/

        // replace multiple '.' characters with single '.'
        return rel.replaceAll(/(\.){2,}/, '.')
    }

    private static boolean isNotPackageStart(ch) {
        if(ch == '.') return false
        return Character.isJavaIdentifierPart(ch as char) && !Character.isJavaIdentifierStart(ch as char)
    }

    private static boolean isPackagePart(ch) {
        return (ch == '.') || Character.isJavaIdentifierPart(ch as char)
    }
}
