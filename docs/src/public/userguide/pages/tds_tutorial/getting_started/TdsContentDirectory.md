---
title: TDS Content Directory
last_updated: 2020-05-08
sidebar: tdsTutorial_sidebar
toc: false
permalink: tds_content_directory.html
---
This section examines the directory structure and files found in the TDS Content directory.

{%include note.html content="
This section assumes you have [installed the JDK and Tomcat Servlet Container](install_java_tomcat.html) and have successfully [deployed the TDS](deploying_the_tds.html).
" %}

## TDS `Content` Directory Location

As mentioned in the [Deploying the TDS](deploying_the_tds.html#creation-of-tds-content_root) section of this tutorial, the TDS `content` (a.k.a `$CONTENT_ROOT`) is a directory created by the TDS the first time it is deployed (or any time the directory is empty).

All THREDDS Data Server configuration information is stored under the TDS `content` directory.
The location of the directory is controlled by the `tds.content.root.path` Java system property which is set in the [$TOMCAT_HOME/bin/setenv.sh](running_tomcat.html#setting-java_home-java_opts-catalina_home-catalina_base-and-content_root) file.

**There is no default location - `tds.content.root.path` must be set or the TDS will not start.**

{%include note.html content="
Please see the [Running Tomcat](running_tomcat.html#setting-java_home-java_opts-catalina_home-catalina_base-and-content_root) page of this tutorial for information on how to set the TDS `content` location.
" %}

## Importance Of The TDS `Content` Directory

Please note the following about this important directory:

* Once created the TDS `content` directory is persisted even when a TDS installation is upgraded or re-deployed.  Therefore, we recommend locating it somewhere separate from `$TOMCAT_HOME` on your file system that will be persisted.
* All your configuration, modifications, and additions should be made in this directory.
* Do NOT place files containing passwords or anything else with security issues in this directory.
* Typically, you will only be adding and modifying catalogs and configuration files in `content`.

## TDS `content` Directory Structure

Examine the TDS `content` directory structure by moving into whatever you set `tds.content.root.path` to be and do a long listing (`/usr/local/tomcat/content` in this case):
  
~~~bash
# cd /usr/local/tomcat/content
# ls -l

total 80
drwxr-x---   8 oxelson  staff   256 May  6 12:05 cache
-rw-r-----   1 oxelson  staff  2259 May  6 15:33 catalog.xml
-rw-r-----   1 oxelson  staff  2608 May  6 12:05 enhancedCatalog.xml
drwxr-x---  11 oxelson  staff   352 May  8 12:06 logs
drwxr-x---   3 oxelson  staff    96 May  6 12:05 notebooks
drwxr-x---   3 oxelson  staff    96 May  6 12:05 public
drwxr-x---   4 oxelson  staff   128 May  8 12:08 state
drwxr-x---   3 oxelson  staff    96 May  6 12:05 templates
-rw-r-----   1 oxelson  staff  8942 May  6 15:56 threddsConfig.xml
-rw-r-----   1 oxelson  staff  2797 May  6 12:05 wmsConfig.xml
~~~

Familiarize yourself with these important files directories:



For now, we will focus on the following subset of the content directory:

 * `<tds.content.root.path>/thredds/`
   * `catalog.xml` - the main TDS configuration catalog (root catalog for TDS configuration)
   * `enhancedCatalog.xml` - an example catalog [Note: It is referenced from catalog.xml.]
   * `threddsConfig.xml` - configuration file for allowing non-default services, configuring caching, etc (more details available here).
   * `logs/`
     * `catalogInit.log` - log file for messages generated while reading TDS configuration catalogs during TDS initialization and reinitialization.
     * `threddsServlet.log` - log messages about individual TDS requests, including any error messages. Useful for debugging problems.
   * `cache/` - various cache directories
     * `agg/`
     * `cdm/`
     * `collection/`
     * `ehcache/`
     * `ncss/`
     * `wcs/`
   * `templates/`
     * `tdsTemplateFragments.html` - user-supplied Thymeleaf HTML templates (see [Customizing TDS](customizing_tds_look_and_feel.html#thymeleaf-templates) for details).
