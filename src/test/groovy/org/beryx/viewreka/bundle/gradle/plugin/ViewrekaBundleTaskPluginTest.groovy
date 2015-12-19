package org.beryx.viewreka.bundle.gradle.plugin

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.util.jar.Attributes
import java.util.jar.JarFile

class ViewrekaBundleTaskPluginTest extends Specification {
    private static final TEST_RESOURCES_DIR = 'dummyBundle'
    private static final TEST_BUNDLE_CLASS = 'org.beryx.dummy.DummyBundle'

    def createBuildContent(bundleConfig) {
        """
            plugins {
                id 'java'
                id 'org.beryx.viewreka.bundle' version 'local-jar'
            }

            repositories {
                jcenter()
            }

            def defaultEncoding = 'UTF-8'
            [compileJava, compileTestJava]*.options*.encoding = defaultEncoding

            dependencies {
                compile 'org.json:json:20150729'
                compile 'org.apache.httpcomponents:httpclient:4.5.1'
                compile 'org.apache.commons:commons-lang3:3.2'
                compile 'org.apache.commons:commons-io:1.3.2'
                compile 'org.apache.derby:derby:10.11.1.1'
                compile 'commons-cli:commons-cli:1.3.1'
            }

            group = 'org.beryx'
            version = '0.0.1'

            viewrekaBundle {
                bundleClass = '$TEST_BUNDLE_CLASS'
                viewrekaVersionMajor = 1
                viewrekaVersionMinor = 2
                mergeServiceFiles()
                $bundleConfig
            }
        """.stripIndent()
    }

    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    def pluginClasspath = [new File('build/classes/main')] + new FileNameFinder().getFileNames('build/libs', 'viewreka-bundle-gradle-plugin-*-all.jar').collect{new File(it)}

    def buildBundle(String bundleConfig) {
        println "testProjectDir: $testProjectDir.root"
        def srcDir = new File("src/test/resources/$TEST_RESOURCES_DIR")
        FileUtils.copyDirectory(srcDir, testProjectDir.root)

        new File(testProjectDir.root, 'build.gradle') << createBuildContent(bundleConfig)

        BuildResult build = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath(pluginClasspath)
                .withArguments('--stacktrace', '--debug', ViewrekaBundlePlugin.VIEWREKA_BUNDLE_TASK_NAME)
                .build()

        def jars = new FileNameFinder().getFileNames("$testProjectDir.root.absolutePath/build/libs", "*.$ViewrekaBundleTask.DEFAULT_EXTENSION").collect{new File(it)}
        assert jars.size() == 1

        println "Build output:\n$build.output"

        def jar = new JarFile(jars[0])

        println "$jar.name:"
        jar.entries().each {entry ->
            println "\t$entry.name"
        }
        println "\n\nmanifest: $jar.manifest.mainAttributes"

        assert jar.manifest.mainAttributes[new Attributes.Name(ViewrekaBundleTask.ATTR_BUNDLE_CLASS)] == TEST_BUNDLE_CLASS

        def cmd = "java -cp $jar.name org.beryx.dummy.DummyTest"
        def output = cmd.execute().text

        println "DummyTest output: \n$output"

        def outputProps = new Properties()
        outputProps.load(new ByteArrayInputStream(output.bytes))

        assert outputProps.dummy == TEST_BUNDLE_CLASS

        return [jar, outputProps]
    }

    def "should create the viewreka-bundle task"() {
        given:
        Project project = ProjectBuilder.builder().build()

        when:
        project.pluginManager.apply 'org.beryx.viewreka.bundle'

        then:
        project.tasks[ViewrekaBundlePlugin.VIEWREKA_BUNDLE_TASK_NAME] instanceof Task
    }

    def "should perform standard relocation"() {
        when:
        def (jar, outputProps) = buildBundle('')

        then:
        jar.getEntry('org/beryx/test/shaded/org/apache/commons/cli/Parser.class') != null
        jar.getEntry('org/apache/commons/cli/Parser.class') == null

        and:
        outputProps.EventUtils == 'org.beryx.test.shaded.org.apache.commons.lang3.event.EventUtils'
        outputProps.FormatFactory == 'org.beryx.test.shaded.org.apache.commons.lang3.text.FormatFactory'
        outputProps.EntityArrays == 'org.beryx.test.shaded.org.apache.commons.lang3.text.translate.EntityArrays'
        outputProps.Statistics == 'org.beryx.test.shaded.org.apache.derby.catalog.Statistics'
    }

    def "should not relocate"() {
        when:
        def (jar, outputProps) = buildBundle('relocationPrefix = ""')

        then:
        jar.getEntry('my/relocation/org/apache/commons/cli/Parser.class') == null
        jar.getEntry('org/apache/commons/cli/Parser.class') != null

        and:
        outputProps.EventUtils == 'org.apache.commons.lang3.event.EventUtils'
        outputProps.FormatFactory == 'org.apache.commons.lang3.text.FormatFactory'
        outputProps.EntityArrays == 'org.apache.commons.lang3.text.translate.EntityArrays'
        outputProps.Statistics == 'org.apache.derby.catalog.Statistics'
    }

    def "should perform relocation with custom prefix"() {
        when:
        def (jar, outputProps) = buildBundle('relocationPrefix = "my.relocation"')

        then:
        jar.getEntry('my/relocation/org/apache/commons/cli/Parser.class') != null
        jar.getEntry('org/apache/commons/cli/Parser.class') == null

        and:
        outputProps.EventUtils == 'my.relocation.org.apache.commons.lang3.event.EventUtils'
        outputProps.FormatFactory == 'my.relocation.org.apache.commons.lang3.text.FormatFactory'
        outputProps.EntityArrays == 'my.relocation.org.apache.commons.lang3.text.translate.EntityArrays'
        outputProps.Statistics == 'my.relocation.org.apache.derby.catalog.Statistics'
    }

    def "should perform custom relocation"() {
        when:
        def (jar, outputProps) = buildBundle('''
            dependencies {
              exclude(dependency('commons-cli:commons-cli'))
            }
            relocationPrefix = 'my.relocation'
            relocationExclude 'org.apache.derby.catalog.**'
            relocationExtraInclude 'org.apache.derby.loc'
            relocate 'org.json', 'myjson'
            relocate('org.apache.http', 'myhttp') {
                exclude 'org.apache.http.client.config.RequestConfig'
                exclude 'org.apache.http.client.methods.**'
            }
            relocate('org.apache.commons.lang3.text', 'mylang3.text') {
                exclude 'org.apache.commons.lang3.text.translate.**'
            }
        ''')

        then:
        jar.getEntry('org/apache/commons/cli/Parser.class') == null
        jar.getEntry('my/relocation/org/apache/commons/cli/Parser.class') == null

        jar.getEntry('myjson/JSONWriter.class') != null
        jar.getEntry('org/json/JSONWriter.class') == null
        jar.getEntry('my/relocation/org/json/JSONWriter.class') == null

        jar.getEntry('myhttp/client/HttpClient.class') != null
        jar.getEntry('org/apache/http/client/HttpClient.class') == null
        jar.getEntry('my/relocation/org/apache/http/client/HttpClient.class') == null

        jar.getEntry('myhttp/client/config/RequestConfig.class') == null
        jar.getEntry('org/apache/http/client/config/RequestConfig.class') != null
        jar.getEntry('my/relocation/org/apache/http/client/config/RequestConfig.class') == null

        jar.getEntry('myhttp/client/methods/HttpPost.class') == null
        jar.getEntry('org/apache/http/client/methods/HttpPost.class') != null
        jar.getEntry('my/relocation/org/apache/http/client/methods/HttpPost.class') == null

        jar.getEntry('org/apache/derby/info/DBMS.properties') != null
        jar.getEntry('my/relocation/org/apache/derby/info/DBMS.properties') == null

        jar.getEntry('org/apache/derby/loc/m0_en.properties') == null
        jar.getEntry('my/relocation/org/apache/derby/loc/m0_en.properties') != null

        jar.getEntry('mylang3/event/EventUtils.class') == null
        jar.getEntry('org/apache/commons/lang3/event/EventUtils.class') == null
        jar.getEntry('my/relocation/org/apache/commons/lang3/event/EventUtils.class') != null

        jar.getEntry('mylang3/text/FormatFactory.class') != null
        jar.getEntry('org/apache/commons/lang3/text/FormatFactory.class') == null
        jar.getEntry('my/relocation/org/apache/commons/lang3/text/FormatFactory.class') == null

        jar.getEntry('mylang3/text/translate/EntityArrays.class') == null
        jar.getEntry('org/apache/commons/lang3/text/translate/EntityArrays.class') != null
        jar.getEntry('my/relocation/org/apache/commons/lang3/text/translate/EntityArrays.class') == null

        jar.getEntry('org/apache/derby/catalog/Statistics.class') != null
        jar.getEntry('my/relocation/org/apache/derby/catalog/Statistics.class') == null

        and:
        outputProps.JSONWriter == 'myjson.JSONWriter'
        outputProps.RequestConfig == 'org.apache.http.client.config.RequestConfig'
        outputProps.AuthState == 'myhttp.auth.AuthState'
        outputProps.HttpClient == 'myhttp.client.HttpClient'
        outputProps.HttpGet == 'org.apache.http.client.methods.HttpGet'
        outputProps.EventUtils == 'my.relocation.org.apache.commons.lang3.event.EventUtils'
        outputProps.FormatFactory == 'mylang3.text.FormatFactory'
        outputProps.EntityArrays == 'org.apache.commons.lang3.text.translate.EntityArrays'
        outputProps.Statistics == 'org.apache.derby.catalog.Statistics'
    }
}
