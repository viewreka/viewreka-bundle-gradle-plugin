[![License](https://img.shields.io/github/license/viewreka/viewreka-bundle-gradle-plugin.svg)](https://github.com/viewreka/viewreka-bundle-gradle-plugin/blob/master/LICENSE)
[![Build Status](https://img.shields.io/travis/viewreka/viewreka-bundle-gradle-plugin/master.svg?label=Build)](https://travis-ci.org/viewreka/viewreka-bundle-gradle-plugin)

## viewreka-bundle-gradle-plugin ##

This plugin extends the [Shadow plugin](https://github.com/johnrengelman/shadow) in order to allow creating [Viewreka](https://github.com/viewreka/viewreka) bundles (aka vbundles). 

Viewreka bundles are fat jars with the file extension `.vbundle`. The following attributes must be present in the manifest of a vbundle:

- viewrekaBundleClass - the fully qualified name of the class implementing the [ViewrekaBundle](https://github.com/viewreka/viewreka/blob/master/projects/bundle-api/src/main/java/org/beryx/viewreka/bundle/api/ViewrekaBundle.java) interface.
- viewrekaVersionMajor - the major version of the oldest Viewreka release supported by this vbundle.
- viewrekaVersionMinor - the munor version of the oldest Viewreka release supported by this vbundle.
- viewrekaVersionPatch - the patch version of the oldest Viewreka release supported by this vbundle. 



viewreka-bundle-gradle-plugin provides a new task called `viewrekaBundle`.
