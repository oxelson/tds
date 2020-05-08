---
title: Client Catalogs in ToolsUI
last_updated: 2020-05-08
sidebar: tdsTutorial_sidebar
toc: false
permalink: client_catalog_via_toolsUI.html
---

Tired of looking at XML?  The NetCDF Tools User Interface (a.k.a. ToolsUI) can display local and remote THREDDS client catalogs in a much more friendly GUI format.

## Acquiring &amp; Running ToolsUI

ToolsUI is a Java application developed at Unidata. Download the latest [toolsUi.jar](https://www.unidata.ucar.edu/downloads/netcdf-java/){:target="_blank"} file from the Unidata website and start it [from the command line](https://docs.unidata.ucar.edu/netcdf-java/{{site.netcdf-java_docset_version}}/userguide/toolsui_ref.html){:target="_blank"}.

Example:

{%include image.html file="tds/tutorial/client_catalogs/ToolsUICL.png" alt="Start ToolsUI from the command line" caption="" %}

This will launch the ToolsUI application.  You will see something similar to the following appear:

{%include image.html file="tds/tutorial/client_catalogs/ToolsUIMain.png" alt="ToolsUI Main Interface" caption="" %}

## Viewing THREDDS Client Catalogs

To view a client catalog, click on the `THREDDS` tab in the ToolsUI interface.
 
You can view a _local_ client catalog file by clicking on the ![fileOpen](images/tds/tutorial/client_catalogs/fileIcon.jpg){:height="12px" width="12px"} button.  This will allow you to navigate to a local catalog XML file:

{%include image.html file="tds/tutorial/client_catalogs/ToolsUILocal.png" alt="Using ToolsUI to view a local client catalog." caption="" %}

Or enter in the URL of a remote client catalog in the Catalog URL location bar, and click the `Connect` button.

{%include note.html content="
Provide the **XML** version of the remote catalog and NOT the HTML page of the catalog.  E.g.: [https://thredds.ucar.edu/thredds/**catalog.xml**](https://thredds.ucar.edu/thredds/catalog.xml){:target='_blank'}
" %}

The remote catalog will be displayed in a tree widget on the left, and the selected dataset will be shown on the right:

{% include image.html file="tds/tutorial/client_catalogs/TUIthreddsTab.png" alt="Using ToolsUI to view a remote client catalog." caption="" %}

You can use ToolsUI to easily browse local and remote client catalogs and their [datasets](https://docs.unidata.ucar.edu/netcdf-java/5.4/userguide/toolsui_ref.html#thredds){:target="_blank"}.

{%include note.html content="
More information about ToolsUI is available in the [NetCDF-Java Reference Guide](https://docs.unidata.ucar.edu/netcdf-java/5.4/userguide/toolsui_ref.html){:target='_blank'}
" %}