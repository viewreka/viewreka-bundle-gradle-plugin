package org.beryx.viewreka.bundle.gradle.plugin;

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowSpec;

public interface ViewrekaBundleSpec extends ShadowSpec {
    ViewrekaBundleTask relocationExclude(String... paths);
    ViewrekaBundleTask relocationExtraInclude(String... paths);
}
