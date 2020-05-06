---
title: Client Catalog Metadata
last_updated: 2020-05-06
sidebar: tdsTutorial_sidebar
toc: false
permalink: client_catalog_metadata.html
---

Supplementary metadata can be added to each dataset in a client catalog to provide more useful, descriptive information.

## Describing Datasets

So far, we've used the `name`, `serviceName`, `urlPath`, and `ID` attributes of the `dataset` element to tell THREDDS how to treat our datasets.
However, there are a lot of optional properties, or _metadata_, that can be added to help _other_ applications and digital libraries know how to "do the right thing" with our data.


Here is a sample of them:

* The `collectionType` attribute  of the `dataset` element is used on _collection_ datasets to describe the relationship of their nested _direct_ datasets.
* The `dataType` element is a child-element  of the `dataset` element and is a simple classification that helps clients to know how to display the data (e.g. `Image`, `Grid`, `Point` data, etc).
* The `dataFormatType` element (also a child-element of the `dataset` element) describes what format the data is stored in (e.g. `NetCDF`, `GRIB-2`, `NcML`, etc).
  This information is used by data access protocols like OPeNDAP and HTTP.
* The combination of the naming `authority` and the `ID` attributes of the `dataset` element should form a globally-unique identifier for a dataset.
  In the TDS, it is especially important to add the `ID` attribute to your datasets.

~~~xml
<service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/"/>

<dataset name="SAGE III Ozone Loss Experiment" ID="Sage III" collectionType="TimeSeries">
  <dataset name="January Averages" serviceName="odap" urlPath="sage/avg/jan.nc"
      ID="jan.nc" authority="unidata.ucar.edu">
    <dataType>Trajectory</dataType>
    <dataFormatType>NetCDF</dataFormatType>
  </dataset>
</dataset>
~~~

{%include note.html content="
A complete listing of available properties for the `dataset` and other elements can be found in the [client catalog specification](client_catalog_specification.html).
" %}

## Exporting THREDDS Datasets To Digital Libraries

The `harvest` attribute of the `dataset` element indicates that the dataset is at the right level of granularity to be exported to digital libraries or other discovery services.
Elements such as `summary`, `rights`, and `publisher` are needed in order to create valid entries for these services.

Here is an example of a dataset supplemented with `publisher` information:

~~~xml
<dataset name="SAGE III Ozone Loss Experiment" ID="Sage III" harvest="true">
  <contributor role="data manager">John Smith</contributor>
  <keyword>Atmospheric Chemistry</keyword>
  <publisher>
    <long_name vocabulary="DIF">Community Data Portal, National Center for Atmospheric Research, University Corporation for Atmospheric Research</long_name>
    <contact url="http://dataportal.ucar.edu" email="cdp@ucar.edu"/>
  </publisher>
</dataset>
~~~

## Sharing Metadata

When a catalog includes multiple datasets, it can often be the case that they have share properties.

For example, the following _direct_ datasets share the same `serviceName`, `authority`, and `dataFormatType`:

~~~xml
<service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/"/>

<dataset name="SAGE III Ozone Loss Experiment" ID="Sage III">
  <dataset name="January Averages" urlPath="sage/avg/jan.nc" ID="jan.nc" serviceName="odap" authority="unidata.ucar.edu" dataFormatType="NetCDF"/>
  <dataset name="February Averages" urlPath="sage/avg/feb.nc" ID="feb.nc" serviceName="odap" authority="unidata.ucar.edu" dataFormatType="NetCDF"/>
  <dataset name="March Averages" urlPath="sage/avg/mar.nc" ID="mar.nc" serviceName="odap" authority="unidata.ucar.edu" dataFormatType="NetCDF"/>
</dataset>
~~~

Rather than declare the same information on each dataset, use the `metadata` element to factor out common information:

~~~xml
<service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/"/>

<dataset name="SAGE III Ozone Loss Experiment" ID="Sage III">
  <metadata inherited="true"> <!-- 1 -->
    <serviceName>odap</serviceName> <!-- 2 -->
    <authority>unidata.ucar.edu</authority> <!-- 2 --> 
    <dataFormatType>NetCDF</dataFormatType> <!-- 2 -->
  </metadata>

  <dataset name="January Averages" urlPath="sage/avg/jan.nc" ID="jan.nc"/> <!-- 3 -->
  <dataset name="February Averages" urlPath="sage/avg/feb.nc" ID="feb.nc"/> <!-- 3 -->   
  <dataset name="Global Averages" urlPath="sage/global.nc" ID="global.nc" authority="fluffycats.com"/> <!-- 4 -->
</dataset>
~~~

1. The `metadata` element with `inherited="true"` implies that all the information inside the `metadata` element applies to the current dataset and all nested datasets.
2. `serviceName`, `authority`, and `dataFormatType` are declared as elements.
3. The `January Averages` and `Februsary Averages` _direct_ datasets use all the metadata values declared in the parent dataset.
4. The `Gloabl Averages` dataset overrides the `authority` property specified in the `metadata` element, but uses the other `serviceName` and `dataFormatType` metadata values.

## When To Use A Metadata Element?

Both the `dataset` and `metadata` elements are containers for metadata properties called the `threddsMetadata` group.

When the metadata is specific to the dataset, put it directly in the `dataset` element.
When you want to share it with all nested datasets, put it in a `metadata` `inherited="true"` element.

{%include note.html content="
For more information on what other properties are included in the `threddsMetadata` group, see the [client catalog specification](client_catalog_specification.html).
" %}

## Next Step

Next, we'll see how to manage large client catalogs by separating them into [smaller, organized pieces](client_catalog_references.html).  
