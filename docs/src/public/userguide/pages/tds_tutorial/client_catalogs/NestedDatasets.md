---
title: Nested Datasets
last_updated: 2020-05-06
sidebar: tdsTutorial_sidebar
toc: false
permalink: nested_datasets.html
---

Client catalogs allow you to organize multiple datasets into hierarchical, logical structure.

## Hierarchical Structure

When you have multiple datasets to declare in each client catalog, you can use _nested_ datasets:

~~~xml
<?xml version="1.0" ?>
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" >
  <service name="odap" serviceType="OpenDAP"  base="/thredds/dodsC/" />

  <dataset name="SAGE III Ozone Loss Experiment" ID="Sage III"> <!-- 1 -->
    <dataset name="January Averages" serviceName="odap" urlPath="sage/avg/jan.nc" ID="jan.nc"/>  <!-- 2 -->
    <dataset name="February Averages" serviceName="odap" urlPath="sage/avg/feb.nc" ID="feb.nc"/> <!-- 2 -->
    <dataset name="March Averages" serviceName="odap" urlPath="sage/avg/mar.nc" ID="mar.nc"/>    <!-- 2 -->
  </dataset> <!-- 3 -->
</catalog>
~~~

Line-by-line explanation:

1. Declares a _collection_ `dataset` which acts as a container for the other `dataset` elements.
   Note that it ends in a `>` instead of `/>`, and does not have a `urlPath` attribute.
2. These `dataset` elements are used to directly point to data and are called _direct_ datasets.
3. This closes the _collection_ `dataset` element on line 1.

{%include note.html content="  
All of the above _direct_ datasets are made available using the OPeNDAP protocol (`serviceName=\"odap\"`).
" %} 

You can add any level of nesting you want, e.g.:

~~~xml
<?xml version="1.0" ?>
<catalog name="Example" xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" >
  <service name="odap" serviceType="OpenDAP"  base="/thredds/dodsC/" />

  <dataset name="SAGE III Ozone Loss Experiment" ID="Sage III">
    <dataset name="Monthly Averages">
      <dataset name="January Averages" serviceName="odap" urlPath="sage/avg/jan.nc" ID="jan.nc"/>
      <dataset name="February Averages" serviceName="odap" urlPath="sage/avg/feb.nc" ID="feb.nc"/>
      <dataset name="March Averages" serviceName="odap" urlPath="sage/avg/mar.nc" ID="mar.nc"/>
    </dataset>

    <dataset name="Daily Flight Data" ID="Daily Flight">
      <dataset name="January">
        <dataset name="Jan 1, 2001" serviceName="odap" urlPath="sage/daily/20010101.nc" ID="20010101.nc"/>
        <dataset name="Jan 2, 2001" serviceName="odap" urlPath="sage/daily/20010102.nc" ID="20010102.nc"/>
      </dataset>
    </dataset>
  </dataset>
</catalog>
~~~

## Next Step

Next, we'll look at how to add [additional descriptive information](client_catalog_metadata.html) about the datasets in our client catalog.  