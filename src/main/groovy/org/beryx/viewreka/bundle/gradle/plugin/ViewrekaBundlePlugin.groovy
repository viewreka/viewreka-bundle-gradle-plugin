package org.beryx.viewreka.bundle.gradle.plugin

import com.github.jengelman.gradle.plugins.shadow.ShadowBasePlugin
import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

public class ViewrekaBundlePlugin implements Plugin<Project> {
    static final String VIEWREKA_BUNDLE_TASK_NAME = "viewrekaBundle"

    Project project
    ShadowJar shadowJar

    @Override
    void apply(Project project) {
        project.apply plugin: 'java'
        project.apply plugin: ShadowPlugin

        this.project = project
        shadowJar = project.tasks.findByName(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME)

        configureViewrekaBundleTask()
    }

    // Adapted from com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.configureShadowTask
    protected void configureViewrekaBundleTask() {
        ViewrekaBundleTask viewrekaBundle = project.tasks.create(VIEWREKA_BUNDLE_TASK_NAME, ViewrekaBundleTask)
        viewrekaBundle.group = ShadowJavaPlugin.SHADOW_GROUP
        viewrekaBundle.description = 'Create a Viewreka bundle of project and runtime dependencies'
        viewrekaBundle.manifest.inheritFrom project.tasks.jar.manifest
        viewrekaBundle.doFirst {
            def files = project.configurations.findByName(ShadowBasePlugin.CONFIGURATION_NAME).files
            if (files) {
                def libs = [project.tasks.jar.manifest.attributes.get('Class-Path')]
                libs.addAll files.collect { "${it.name}" }
                manifest.attributes 'Class-Path': libs.findAll { it }.join(' ')
            }
        }
        JavaPluginConvention convention = project.convention.getPlugin(JavaPluginConvention)
        viewrekaBundle.from(convention.sourceSets.main.output)
        viewrekaBundle.configurations = [project.configurations.runtime]
        viewrekaBundle.exclude('META-INF/INDEX.LIST', 'META-INF/*.SF', 'META-INF/*.DSA', 'META-INF/*.RSA')
    }
}
