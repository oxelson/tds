---
title: Installing The Java Development Kit
last_updated: 2021-06-08
sidebar: quickstart_sidebar
toc: true
permalink: installing_java.html
---

{% include note.html content="
Users of OS-provided packages via package management systems for Java and/or Tomcat may want to reference the [THREDDS mailing list](https://www.unidata.ucar.edu/mailing_lists/archives/thredds/){:target='_blank'} for installation help."
%}

## System Requirements

* OpenJDK Java 8 (latest version)
* Apache Tomcat 8.x

While there are different distributors of Java and servlet containers, Unidata develops, uses and tests the THREDDS Data Server using _OpenJDK Java_ and the _Apache Tomcat_ servlet container.


## Installing OpenJDK Java JDK

The following example shows the JDK installation on a linux system.  
The installation is being performed as the `root` user.

{% include note.html content="
For installation of the JDK on Windows or Mac OS, see the [JDK Installation Guide](https://adoptopenjdk.net/installation.html){:target='_blank'}.
" %}

1.  [Download](https://adoptopenjdk.net/){:target="_blank"} current OpenJDK 8 (LTS) JDK version from the AdoptOpenJDK site. 

2.  Install the JDK.

    Copy the binary `tar.gz` file into the installation directory (`/usr/local` in this example):

    ~~~bash
    # pwd
    /usr/local
    
    # cp /tmp/jdk-8u192-linux-x64.tar.gz .

    # ls -l
    total 187268
    -rw-r--r-- 1 root root 191757099 Oct 24 13:19 jdk-8u192-linux-x64.tar.gz
    ~~~

    Unpack the archive file:

    ~~~bash
    # tar xvfz jdk-8u192-linux-x64.tar.gz 
    ~~~

    This will extract the JDK in the installation directory:

    ~~~bash
    # ls -l
    total 187272
    drwxr-xr-x 7 root root      4096 Oct  6 07:58 jdk1.8.0_192
    -rw-r--r-- 1 root root 191757099 Oct 24 13:19 jdk-8u192-linux-x64.tar.gz
    ~~~

    Remove the remaining binary `tar.gz` file when the installation is complete.
   
    ~~~bash
    # rm jdk-8u192-linux-x64.tar.gz
    # ls -l
    total 187279
    drwxr-xr-x 7 root root      4096 Oct  6 07:58 jdk1.8.0_192
    ~~~

    {% include important.html content="
    Depending on your OS you may need install either the 32-bit or 64-bit version of the JDK.
    But, we *really, really, really recommend* you use a 64-bit OS if you're planning to run the THREDDS Data Server.
    " %}


## Next Step
