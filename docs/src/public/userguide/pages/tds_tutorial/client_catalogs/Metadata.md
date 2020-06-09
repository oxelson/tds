---
title: Client Catalog Metadata
last_updated: 2020-06-09
sidebar: tdsTutorial_sidebar
toc: false
permalink: client_catalog_metadata.html
---

This section covers how to add supplementary metadata to each dataset in a client catalog to provide more useful, descriptive information.


{%include note.html content="
This section assumes you have a basic understanding of a THREDDS [client catalog](basic_client_catalog.html) and how to make [multiple datasets](nested_datasets.html) available to a client from a single client catalog.
" %}

## Describing Datasets

So far, we've used the `name`, `serviceName`, `urlPath`, and `ID` attributes of the `dataset` element to tell THREDDS how to treat our datasets.
However, there are a lot of optional properties, or *metadata*, that can be added to help *other* applications and digital libraries know how to "do the right thing" with our data.


Here is a small sample of them:

* The `collectionType` attribute  of the `dataset` element is used on *collection* datasets to describe the relationship of their nested *direct* datasets.
  {%include note.html content="
  Remember that *direct* datasets are [nested](nested_datasets.html) datasets and *directly* point to data via their `urlPath` attribute.
  " %}
* The `dataType` element is a child-element  of the `dataset` element and is a simple classification that helps clients to know how to display the data (e.g. `Image`, `Grid`, `Point` data, etc).
* The `dataFormatType` element (also a child-element of the `dataset` element) describes what format the data is stored in (e.g. `NetCDF`, `GRIB-2`, `NcML`, etc).
  This information is used by data access protocols like OPeNDAP and HTTP.
* The combination of the `ID` attributes of the `dataset` element should form a globally-unique identifier for a dataset.
  In the TDS, it is especially important to add the `ID` attribute to your datasets.

For example, in this snippet of a client catalog:

~~~xml
1    <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/"/>
2    
3    <dataset name="SAGE III Ozone Loss Experiment" ID="Sage III" collectionType="TimeSeries">
4      <dataset name="January Averages" serviceName="odap" urlPath="sage/avg/jan.nc" ID="jan.nc">
5        <dataType>Trajectory</dataType>
6        <dataFormatType>NetCDF</dataFormatType>
7      </dataset>
8    </dataset>
~~~

| **line #**  | **Explanation**                   |
| 3 | Declares `Sage III` is a collection of `TimeSeries` datasets.    |       
| 4 | The`Sage III` data is accessible at `https://hostname:port/thredds/dodsC/sage/avg/jan.nc`|           
| 5 | Notifies a client that the type of data in the `January Averages` dataset is Trajectory data. |
| 6 | Tells the client the `January Averages` dataset is in NetCDF format.|



{%include note.html content="
A complete listing of available properties for the `dataset` and other elements can be found in the [client catalog specification](client_catalog_specification.html).
" %}

## Exporting THREDDS Datasets To Digital Libraries

Digital libraries require a standard set of metadata to be provided in order catalog a dataset.  Such metadata entries are available to supplement your data in THREDDS client catalogs.  

For example, the `harvest` attribute of the `dataset` element indicates that the dataset is at the right level of granularity to be exported to digital libraries or other discovery services.
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

For example, the following *direct* datasets share the same `serviceName`, `authority`, and `dataFormatType`:

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
 1    <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/"/>
 2    
 3    <dataset name="SAGE III Ozone Loss Experiment" ID="Sage III">
 4      <metadata inherited="true"> 
 5        <serviceName>odap</serviceName> 
 6        <authority>unidata.ucar.edu</authority>  
 7        <dataFormatType>NetCDF</dataFormatType>
 8      </metadata>
 9
10      <dataset name="January Averages" urlPath="sage/avg/jan.nc" ID="jan.nc"/> 
11      <dataset name="February Averages" urlPath="sage/avg/feb.nc" ID="feb.nc"/> 
12      <dataset name="Global Averages" urlPath="sage/global.nc" ID="global.nc" authority="fluffycats.com"/> 
13    </dataset>
~~~

Line-by-line explanation of the above client catalog snippet:

| **line #**  | **Explanation**                   |
| 4 | The `metadata` element with `inherited="true"` implies that all the information inside the `metadata` element applies to the current dataset and all nested datasets.   |       
| 5-7 |`serviceName`, `authority`, and `dataFormatType` are declared as elements, and each include a value to be used by all nexted datasets. |           
| 10-11 | The `January Averages` and `February Averages` *direct* datasets use all the metadata values declared in the parent dataset. |
| 12 | The `Global Averages` dataset overrides the `authority` property specified in the `metadata` element, but uses the other `serviceName` and `dataFormatType` metadata values. |

## When To Use A Metadata Element?

Both the `dataset` and `metadata` elements are containers for metadata properties called the `threddsMetadata` group.

When the metadata is specific to the dataset, put it directly in the `dataset` element.
When you want to share it with **all** nested datasets, put it in a `metadata` `inherited="true"` element.

{%include note.html content="
For more information on what other properties are included in the `threddsMetadata` group, see the [client catalog specification](client_catalog_specification.html).
" %}

## Next Step

Next, we'll see how to manage large client catalogs by separating them into [smaller, organized pieces](client_catalog_references.html).  
