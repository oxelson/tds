---
title: Client Catalog Primer
last_updated: 2020-05-08
sidebar: tdsTutorial_sidebar
toc: false
permalink: basic_client_catalog.html
---
THREDDS client catalogs are simple XML files that collect, organize, and describe accessible datasets.
They provide an access method (URL) and a _human-understandable_ name for each dataset.

## Basic Client Catalog Structure

Here's an example of a very simple catalog:

~~~xml
<?xml version="1.0" ?>  <!-- 1 -->
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" > <!-- 2 -->
  <service name="odap" serviceType="OPeNDAP" base="/thredds/dodsC/" /> <!-- 3 -->
  <dataset name="SAGE III Ozone 2006-10-31" serviceName="odap" urlPath="sage/20061031.nc" ID="20061031.nc"/> <!-- 4 -->
</catalog> <!-- 5 -->
~~~

with this line-by-line explanation:

1. Indicates that this is an XML document.
2. The `catalog` element is the root element of the XML file.
   It must declare the THREDDS catalog namespace with the `xmlns` attribute exactly as shown.
3. Declares a `service` element named `odap` that will serve data (`serviceType`) via the `OPeNDAP` protocol.
4. The `dataset` element named `SAGE III Ozone 2006-10-31` is declared.

   It references the `odap service` on the prior line using the `serviceName` attribute, meaning that it will be served via the OPeNDAP protocol.
   
   The `urlPath` and `ID` attributes are used to [construct a URL](#constructing-an-access-url) with which to access the dataset.
5. Closes the `catalog` element.

{%include ahead.html content="  
In addition to OPeNDAP, many [other data access services](services_ref.html) come bundled with the THREDDS Data Server.
" %} 

## Constructing An Access URL

Using the example client catalog shown above, here are the steps for client software to construct a dataset access URL:

1. Find the `service` referenced by the `dataset` (in this case, `odap`):

   ~~~xml
   <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />
   <dataset name="SAGE III Ozone 2006-10-31" serviceName="odap" urlPath="sage/20061031.nc" ID="20061031.nc"/>
   ~~~

2. Append the `service` element's `base` path to the server root to construct the service base URL:
   * serverRoot = `https://hostname:port`
   * serviceBasePath = `/thredds/dodsC/`
   * serviceBaseUrl = serverRoot + serviceBasePath = `http://hostname:port/thredds/dodsC/`
3. Find the `urlPath` and `ID` referenced by the `dataset` element:
   ~~~xml
   <dataset name="SAGE III Ozone 2006-10-31" serviceName="odap" urlPath="sage/20061031.nc" ID="20061031.nc"/>
   ~~~
4. Append the `dataset` `urlPath` and `ID` to the `service` element's `base` URL to get the entire dataset access URL:
   * serviceBaseUrl = http://hostname:port/thredds/dodsC/
   * datasetUrlPath = sage/20061031.nc
   * datasetAccessUrl = serviceBaseUrl + datasetUrlPath = http://hostname:port/thredds/dodsC/sage/20061031.nc

In summary, the dataset access URL is constructed from the example client catalog with these 3 pieces of information:

~~~
https://hostname:port/thredds/dodsC/sage/20061031.nc
<------------------><------------><--------------->
     server            service         dataset
~~~

{%include ahead.html content="  
We will look at the location and contents of the [default client catalog](default_config_catalog.html) that comes with TDS when we discuss TDS configuration.
" %} 

## Next Step

Next, we'll examine how to have [multiple datasets](nested_datasets.html) in the client catalog.