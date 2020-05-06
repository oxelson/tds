---
title: Compound Service Elements
last_updated: 2020-05-06
sidebar: tdsTutorial_sidebar
toc: false
permalink: compound_service_elements.html
---

Datasets can be made available through more than one access method by creating _compound service_. 

## Defining A Compound Service

To create a _compound service_, give a `service` element the `serviceType` of `Compound` and nest other services within this outer `service` element. 

For example, the following defines a compound service named `all` which contains two nested services for OPeNDAP and WCS:

~~~xml
<service name="all" serviceType="Compound" base="" >
  <service name="odap" serviceType="OPeNDAP" base="/thredds/dodsC/" />
  <service name="wcs" serviceType="WCS" base="/thredds/wcs/" />
</service>
~~~

{%include important.html content="
The `base` attribute of the compound `service` element does not need to have a value, but it **does** need to be defined in order to pass catalog validation.  Just leave the value blank (`base=\"\"`).
" %}

## Referencing A Compound Service 

Any dataset that references a compound service will have access to all of its defined services. 

For instance, the following `SAGE III Ozone 2006-10-3` dataset references the example compound service `all` defined above.

~~~xml
<dataset name="SAGE III Ozone 2006-10-31" urlPath="sage/20061031.nc" ID="20061031.nc">
  <serviceName>all</serviceName>
</dataset>
~~~

This dataset will have two access URLs:

* one for OPeNDAP access:  `https://hostname:port/thredds/dodsC/sage/20061031.nc`
* and one for WCS access: `https://hostname:port/thredds/wcs/sage/20061031.nc`


The corresponding page for the `SAGE III Ozone 2006-10-3` dataset in the TDS would show these two access methods:

{%include image.html file="tds/tutorial/client_catalogs/CompoundService.png" alt="Compound service in the TDS" caption="" %}


## Cherry Pick Your Services 

A contained services within a compound service can still be referenced independently.  

For instance the `Global Averages` dataset in the following client catalog directly references the `odap` service:

~~~xml
<?xml version="1.0" ?>  
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" >  
  <service name="all" serviceType="Compound" base="" >
    <service name="odap" serviceType="OPeNDAP" base="/thredds/dodsC/" />
    <service name="wcs" serviceType="WCS" base="/thredds/wcs/" />
  </service>

  <!-- this dataset is only available via OPeNDAP -->
  <dataset name=\"Global Averages\" urlPath=\"sage/global.nc\" ID=\"global.nc\">
    <serviceName>odap</serviceName>
  </dataset>

  <!-- this dataset is available to all services defined in the compound service 'all' -->
  <dataset name="SAGE III Ozone 2006-10-31" urlPath="sage/20061031.nc" ID="20061031.nc">
    <serviceName>all</serviceName>
  </dataset>
</catalog>
~~~

resulting in a single access URL for OPeNDAP: `https://hostname:port/thredds/dodsC/sage/global.nc`

{%include image.html file="tds/tutorial/client_catalogs/CherryPick.png" alt="Cherry picking services in the TDS" caption="" %}

{%include note.html content="
Consult the [Data Services Reference](services_ref.html) for a complete listing of recognized service types.
" %}


## Next Step

Next we need to address [xml validation](client_catalog_xml_validation.html) of our client catalogs.  