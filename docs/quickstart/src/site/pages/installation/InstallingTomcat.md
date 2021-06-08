---
title: Installing The Tomcat Servlet Container
last_updated: 2021-06-08
sidebar: quickstart_sidebar
toc: true
permalink: installing_tomcat.html
---

The following example shows Tomcat installation on a linux system. 
(This type of installation will work on Mac OS systems as well.) 
The installation is performed as the `root` user.

{% include note.html content="
For installation of Tomcat on Windows, see the [Tomcat Setup Guide](http://tomcat.apache.org/tomcat-8.5-doc/setup.html#Windows){:target='_blank'}.
" %}

1.  [Download](http://tomcat.apache.org/download-80.cgi){:target="_blank"} current version of the Tomcat 8.5 servlet container.

2.  Install Tomcat as per the Apache Tomcat [installation instructions](http://tomcat.apache.org/tomcat-8.5-doc/setup.html){:target="_blank"}.

    Copy the binary tar.gz file into the installation directory (`/usr/local` in this example):

    ~~~bash
    # pwd
    /usr/local
    
    # cp /tmp/apache-tomcat-8.5.34.tar.gz .

    # ls -l
    total 196676
    -rw-r--r-- 1 root root   9625824 Oct 24 13:27 apache-tomcat-8.5.34.tar.gz
    drwxr-xr-x 7 root root      4096 Oct  6 07:58 jdk1.8.0_192
    ~~~

    Unpack the archive file:

    ~~~bash
    # tar xvfz apache-tomcat-8.5.34.tar.gz
    ~~~

    This will create a Tomcat directory:

    ~~~bash
    # ls -l
    total 196680
    drwxr-xr-x 9 root root      4096 Oct 24 13:29 apache-tomcat-8.5.34
    -rw-r--r-- 1 root root   9625824 Oct 24 13:27 apache-tomcat-8.5.34.tar.gz
    drwxr-xr-x 7 root root      4096 Oct  6 07:58 jdk1.8.0_192
    ~~~

    Remove the remaining binary `tar.gz` file when the installation is complete.
   
    ~~~bash
    # rm apache-tomcat-8.5.34.tar.gz
    # ls -l
    total 187282
    drwxr-xr-x 9 root root      4096 Oct 24 13:29 apache-tomcat-8.5.34
    drwxr-xr-x 7 root root      4096 Oct  6 07:58 jdk1.8.0_192
    ~~~

## Create Symbolic Links

Adding symbolic links for both the Tomcat and the JDK installations will allow for upgrades of both packages without having to change to configuration files and server startup/shutdown scripts.

The following example shows creating symbolic links for the Tomcat and JDK installation on a linux system. 
(This type of installation will work on Mac OS systems as well.) 
The installation is performed as the `root` user.

{%include note.html content="
Windows users can consult the [Microsoft Documentation](https://docs.microsoft.com/en-us/windows/win32/fileio/symbolic-links){:target='_blank'} for creating symbolic links on Windows systems.
" %}

1. Create symbolic links for the Tomcat and the JDK installations:

    ~~~ bash
    # pwd
    /usr/local
    
    # ln -s apache-tomcat-8.5.34 tomcat 
    # ln -s jdk1.8.0_192 jdk
    # ls -l 
    total 196684
    drwxr-xr-x 9 root root      4096 Oct 24 13:29 tomcat -> apache-tomcat-8.5.34
    drwxr-xr-x 9 root root      4096 Oct 24 13:29 apache-tomcat-8.5.34
    lrwxrwxrwx 1 root root        12 Oct 24 13:59 jdk -> jdk1.8.0_192
    drwxr-xr-x 7 root root      4096 Oct  6 07:58 jdk1.8.0_192
    ~~~

## Next Step

Next, we'll do a quick tour of the relevant elements of the [Tomcat Directory Structure](tomcat_dir_structure_qt.html) and how these elements relate to the TDS.