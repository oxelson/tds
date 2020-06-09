---
title: Catalog References
last_updated: 2020-05-08
sidebar: tdsTutorial_sidebar
toc: false
permalink: client_catalog_references.html
---

In this section we will look at how to manage large client catalogs by separating them into smaller, organized pieces.

{%include note.html content="
This section assumes you have a basic understanding of a THREDDS [client catalog](basic_client_catalog.html) and its structure.
" %}

## External Catalogs

If you offer a lot of data, client catalogs can grow to be large and unwieldy in size. Therefore, it is very useful to break up large catalogs into separately maintained *external catalogs*. 

One way to do this is to build each piece as a separate and logically-complete catalog, then create a **root** client catalog using *catalog references*.

Here is an example of a master client catalog utilizing *catalog references* to include smaller catalogs organized by data type:

~~~xml
1    <?xml version="1.0" encoding="UTF-8"?>
2    <catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" name="Top Catalog" xmlns:xlink="http://www.w3.org/1999/xlink"> 
3       <dataset name="Realtime data from IDD" ID="IDD"> 
4         <catalogRef xlink:href="idd/forecastModels.xml" xlink:title="Forecast Model Data"/> 
5         <catalogRef xlink:href="idd/forecastProdsAndAna.xml" xlink:title="Forecast Products and Analyses"/> 
6         <catalogRef xlink:href="idd/obsData.xml" xlink:title="Observation Data"/> 
7         <catalogRef xlink:href="idd/radars.xml" xlink:title="Radar Data"/> 
8         <catalogRef xlink:href="idd/satellite.xml" xlink:title="Satellite Data"/> 
9       </dataset>
10
11      <catalogRef xlink:title="Far Away Univ. catalog" xlink:href="http://farAway.edu/thredds/catalog.xml" /> 
12    </catalog>
~~~

Corresponding lines in the above catalog:

| **line #**  | **Explanation**                   |
| 2 |  We declare the `xlink` `namespace` in the master client `catalog` element. |
| 3 | The *collection* (or *container*) `dataset` element logically contains the `catalogRefs` elements, which are thought of as nested datasets whose contents are the contents of the external catalog. |
| 4-8 | Here are several `catalogRef` elements, each with a link to an external catalog, using the `xlink:href` attribute.<br>The `xlink:title` is used as the name of the dataset.<br> We need a `name` attribute (in order to validate, for obscure reasons), but it is ignored. <br>The `xlink:href` attributes are [relative URLs](https://www.w3.org/TR/WD-html40-970917/htmlweb.html#h-5.1.2){:target="_blank"} and are resolved against the catalog URL.<br> For example, if the URL of the client catalog, as shown above, is:<br> <https://thredds.ucar.edu/thredds/catalog.xml>{:target="_blank"}<br>then the resolved URL of the first `catalogRef` of `Forecast Model Data` will be:<br><https://thredds.ucar.edu/thredds/idd/forecastModels.xml>{:target="_blank"} |
| 11 |  `catalogRefs` needn't point to local catalogs only.  This one points to a remote client catalog at Far Away University. |

So as you can see, the term *external catalog* can refer to a separate client catalog living on the same servers the TDS, or a client catalog served from a remote location.


{%include important.html content="
The `metadata` elements with `inherited=\"true\"` are NOT copied across `catalogRefs`. The catalog that a `catalogRef` refers to is stand-alone in that sense.
" %}


## Next Step

Now that we know how to organize and manage multiple datasets in a client catalog, we will look at making them available through [more than one access method](compound_service_elements.html).  
