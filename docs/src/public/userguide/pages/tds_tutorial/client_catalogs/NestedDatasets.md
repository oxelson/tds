---
title: Nested Datasets
last_updated: 2020-06-09
sidebar: tdsTutorial_sidebar
toc: false
permalink: nested_datasets.html
---


This section demonstrates how to organize multiple datasets into hierarchical, logical structure in a client catalog.

{%include note.html content="
This section assumes you have a basic understanding of a THREDDS [client catalog](basic_client_catalog.html), its structure, and how a client may use the data it contains to construct a URL to access a dataset.
" %}


## Hierarchical Structure

When you have multiple datasets to declare in each client catalog, you can use *nested* datasets:

~~~xml
 1    <?xml version="1.0" ?>
 2    <catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" >
 3      <service name="odap" serviceType="OpenDAP"  base="/thredds/dodsC/" />
 4
 5      <dataset name="SAGE III Ozone Loss Experiment" ID="Sage III"> 
 6        <dataset name="January Averages" serviceName="odap" urlPath="sage/avg/jan.nc" ID="jan.nc"/>  
 7        <dataset name="February Averages" serviceName="odap" urlPath="sage/avg/feb.nc" ID="feb.nc"/> 
 8        <dataset name="March Averages" serviceName="odap" urlPath="sage/avg/mar.nc" ID="mar.nc"/>    
 9      </dataset> 
10    </catalog>
~~~

An explanation of the relevant lines in the above client catalog:


| **line #**  | **Explanation**         
| 5 | Declares a *collection* `dataset` which acts as a container for the other `dataset` elements.<br> Note that it ends in a `>` instead of `/>`, and does not have a `urlPath` attribute.|
| 6-8 | These `dataset` elements are used to directly point to data and are called *direct* datasets.|
| 9 | This closes the *collection* `dataset` element on line 5.|

{%include note.html content="  
All of the above *direct* datasets are made available using the OPeNDAP protocol (`serviceName=\"odap\"`).
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

Next, we'll look at how to add [additional descriptive information](client_catalog_metadata.html) about the datasets that clients may find useful.  