---
title: THREDDS Catalog Overview
last_updated: 2020-06-09
sidebar: tdsTutorial_sidebar
toc: false
permalink: thredds_catalog_overview.html
---

This section introduces the **THREDDS catalog**, the most fundamental component of the TDS.


## What Are THREDDS Catalogs?

THREDDS catalogs are simple [XML](https://www.w3.org/XML/){:target='_blank'} files used to:

1. *configure* the THREDDS Data Server, detailing the datasets the TDS will serve and what services will be available for each dataset; **and/or**
2. collect, organize, and describe the accessible datasets to potential *clients*.
    
## Types Of Catalogs 

THREDDS Catalogs are *versatile*, meaning they may have multiple functions.   As such, they can be referred to as either **client** catalogs or **configuration** catalogs, depending on what XML elements they contain:  

### Client Catalogs

Client catalogs contain xml elements that tell the **client** (browser, program, etc) what to expect, and consume from the TDS.

{%include note.html content="
A complete listing of all client elements and  properties can be found in the [client catalog specification](client_catalog_specification.html).
" %}

### Configuration Catalogs 

Configuration catalogs contain **configuration elements** to tell the TDS what datasets to make available and how to how to serve those datasets.

{%include note.html content="
A complete listing of all catalog configuration elements and properties can be found in the [server-side catalog specification](server_side_catalog_specification.html).
" %}

It is not uncommon to have **both** client and configuration elements in the same catalog.  It is also possible for the same element/attribute function as both a client and configuration element.

In both of these cases, the catalog could be referred to as either a client or configuration catalog, depending on the elements and context in which you are using them. 


An easy way to remember the difference is:

* client = client-side configuration to find/access the data.
* configuration = server-side configuration just for the TDS.

   {% include image.html file="tds/tutorial/tds_configuration/catalogs.jpg" alt="Client versus configuration catalogs" caption="Clear as mud." %}



{%include ahead.html content="  
You will learn more about the differences and uses of [client](basic_client_catalog.html) and [configuration](basic_config_catalog.html) catalogs in subsequent sections of this tutorial.
" %} 

## Other Catalog Nomenclature

Another term you may encounter in this tutorial and other TDS documentation is **root catalog**.

### Root Catalog

A root catalog is nothing more than a client or configuration catalog that references other catalogs, thereby including those other catalogs into its configuration during TDS startup.  The root catalog and the catalogs it refers to create a *catalog tree* of sorts:

~~~bash
root catalog
   |
   |-- catalog
   |-- catalog
   |-- catalog
~~~

{%include ahead.html content="  
You will learn more about the root catalog that comes with the TDS, called [catalog.xml](default_catalog.html), in a another section of this tutorial.
" %}

## Next Step

Next, we'll examine THREDDS [client catalogs](basic_client_catalog.html) in more depth.