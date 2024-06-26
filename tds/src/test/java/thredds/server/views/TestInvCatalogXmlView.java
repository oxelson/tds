/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.views;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.View;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.builder.CatalogBuilder;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestInvCatalogXmlView {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testUnknownEncoding() {
    String catAsString = "<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"\n"
        + "         xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" + "         version=\"1.0.1\">\n"
        + "  <service name=\"ncDap\" serviceType=\"OPENDAP\" base=\"/thredds/dodsC/\" />\n"
        + "  <dataset name=\"some data\" ID=\"SomeData\">\n" + "     <metadata inherited=\"true\">\n"
        + "       <serviceName>ncDap</serviceName>\n" + "     </metadata>\n"
        + "     <dataset name=\"data one\" id=\"data1\" urlPath=\"some/data/one.nc\" />"
        + "     <dataset name=\"data two\" id=\"data2\" urlPath=\"some/data/two.nc\" />" + "  </dataset>\n"
        + "</catalog>";
    String catUri = "Cat.TestInvCatalogXmlView.testNoDeclaredEncoding";

    URI catURI;
    try {
      catURI = new URI(catUri);
    } catch (URISyntaxException e) {
      Assert.fail("URISyntaxException: " + e.getMessage());
      return;
    }

    CatalogBuilder builder = new CatalogBuilder();
    Catalog cat = builder.buildFromString(catAsString, catURI);
    Map<String, Catalog> model = Collections.singletonMap("catalog", cat);

    View view = new InvCatalogXmlView();
    try {
      view.render(model, new MockHttpServletRequest(), new MockHttpServletResponse());
    } catch (Exception e) {
      e.printStackTrace();
    }


  }


}
