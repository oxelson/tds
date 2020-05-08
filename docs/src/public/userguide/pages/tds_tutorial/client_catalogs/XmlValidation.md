---
title: XML Validation
last_updated: 2020-05-08
sidebar: tdsTutorial_sidebar
toc: false
permalink: client_catalog_xml_validation.html
---

As client catalogs become more complicated, it is a good idea to check them for errors.

## What Makes A Catalog Valid?

There are three components to check to ensure your client catalog is valid:

1. Is the XML well-formed?
2. Is the XML valid against the [catalog schema](http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.7.xsd)?
3. Does it have everything it needs to be read by a THREDDS client?

## Online Validation Tools

You can check _well-formedness_ of the XML in your catalog by using online validation tools such as [xmlvalidation.com](https://www.xmlvalidation.com/){:target="_blank"}.  

{%include tip.html content="
Using an [XML editor](https://en.wikipedia.org/wiki/Comparison_of_XML_editors){:target='_blank'} to write and modify your client catalogs will help to keep your XML well-formed.
" %}


Many of these tools also will allow you to also check the _validity_ of your XML, in which case  you will need to declare the catalog schema location like so:

~~~xml
<catalog name="Validation" xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0
    http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.7.xsd">
  ...
</catalog>
~~~

Regarding the above declaration:

* The `xmlns:xsi` attribute  declares the schema-instance namespace.
* The `xsi:schemaLocation` attribute tells your XML validation tool where to find the THREDDS XML schema document.
  
*Copy both of these attributes exactly as they appear here and paste them into the validation tool.*
  
## THREDDS Catalog Validation Service

Perhaps the easier way of making sure your client catalog is valid is to use the [THREDDS Catalog Validation Service](https://thredds.ucar.edu/thredds/remoteCatalogValidation.html){:target="_blank"} which checks all three aforementioned components at once.

(This service already knows where the schemas are located, so it's not necessary to add that information to the catalog; you only need it if you want to do your own validation with another validation tool.)

{%include note.html content="
The schema referenced in the example can be found [here](https://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.7.xsd){:target='_blank'}.  However, you'll probably want to study the [catalog specification](client_catalog_specification.html) instead, as it is much more digestible.
" %}


## Next Step

Next, we'll see how to view THREDDS client catalogs in a GUI format using the [ToolsUI](client_catalog_via_toolsUI.html) application.