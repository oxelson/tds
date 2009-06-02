/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.FileFilter;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Formatter;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateUnit;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.TestAll;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.EarthLocation;

/**
 * Test PointFeatureTypes.
 *
 * @author caron
 * @since Dec 16, 2008
 */
public class TestPointFeatureTypes  extends TestCase {
  //String topDir = ucar.nc2.TestAll.testdataDir+ "station/";
  public TestPointFeatureTypes( String name) {
    super(name);
  }

  public void testCF() throws IOException {
    String topdir = TestAll.testdataDir + "cdmUnitTest/";

    /////// POINT
    // CF 1.1 psuedo-structure
    testPointDataset(topdir+"cfPoint/point/filtered_apriori_super_calibrated_binned1.nc", FeatureType.POINT, true);

    // CF 1.5 psuedo-structure
    testPointDataset(topdir+"cfPoint/point/nmcbob.shp.nc", FeatureType.POINT, true);

    /////// STATION
    // CF 1.3 ragged contiguous
    testPointDataset(topdir+"cfPoint/station/rig_tower.2009-02-01.ncml", FeatureType.STATION, true);

    // CF 1.5 station unlimited, multidim
    testPointDataset(topdir+"cfPoint/station/billNewDicast.nc", FeatureType.STATION, true);

    // CF 1.5 station multdim
    testPointDataset(topdir+"cfPoint/station/billOldDicast.nc", FeatureType.STATION, true);


    // CF 1.0 multidim with dimensions reversed
    //testPointDataset(topdir+"cfPoint/station/solrad_point_pearson.ncml", FeatureType.STATION, true);

  }

  public void testGempak() throws IOException {
    // (GEMPAK IOSP) stn = psuedoStruct, obs = multidim Structure, time(time) as extraJoin
    //testPointDataset(TestAll.cdmUnitTestDir+"point/gempak/19580807_sao.gem", FeatureType.STATION, true);
    // stationAsPoint (GEMPAK IOSP) stn = psuedoStruct, obs = multidim Structure, time(time) as extraJoin
    testPointDataset(TestAll.cdmUnitTestDir+"formats/gempak/surface/20090521_sao.gem", FeatureType.POINT, true);
  }



  public void testCdmRemote() throws IOException {
    testPointDataset("cdmremote:http://localhost:8080/thredds/cdmremote/station/testCdmRemote/gempak/19580807_sao.gem", FeatureType.STATION, true);
  }

  public void testCdmRemoteCollection() throws IOException {
    testPointDataset("cdmremote:http://localhost:8080/thredds/cdmremote/gempakSurface.xml/collection", FeatureType.STATION, true);
  }

  public void utestReadAll() throws IOException {
    readAllDir(ucar.nc2.TestAll.testdataDir + "station/", new MyFileFilter() , FeatureType.ANY_POINT);
  }

  class MyFileFilter implements FileFilter {
    public boolean accept(File pathname) {
      String path = pathname.getPath();
      if (new File(path+"ml").exists()) return false;
      return path.endsWith(".nc") || path.endsWith(".ncml");
    }
  }

  public void testProblem() throws IOException {
    testPointDataset(ucar.nc2.TestAll.testdataDir+"point/gempak/19580807_sao.gem", FeatureType.STATION, true);
  }

  int readAllDir(String dirName, FileFilter ff, FeatureType type) throws IOException {
    int count = 0;

    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID "+dirName);
      return count;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if (((ff == null) || ff.accept(f)) && !name.endsWith(".exclude")) {
        try {
          testPointDataset(name, type, false);
        } catch (Throwable t)  {
          t.printStackTrace();
        }
        count++;
      }
    }

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude"))
        count += readAllDir(f.getAbsolutePath(), ff, type);
    }

    return count;
  }

  private void testPointDataset(String location, FeatureType type, boolean show) throws IOException {
    System.out.printf("----------- Read %s %n", location);
    long start = System.currentTimeMillis();

    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(type, location, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s %n --> %s %n", location, out);
      assert false;
    }

    // FeatureDataset
    if (show) {
      fdataset.getDetailInfo(out);
      System.out.printf("%s %n", out);
    } else {
      System.out.printf("  Feature Type %s %n", fdataset.getFeatureType());
    }

    Date d1 = fdataset.getStartDate();
    Date d2 = fdataset.getEndDate();
    if ((d1 != null) && (d2 != null))
      assert d1.before(d2) || d1.equals( d2);

    List dataVars =  fdataset.getDataVariables();
    assert dataVars != null;
    for (int i = 0; i < dataVars.size(); i++) {
      VariableSimpleIF v = (VariableSimpleIF) dataVars.get(i);
      assert null != fdataset.getDataVariable( v.getShortName());
    }

    // FeatureDatasetPoint
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      // PointFeatureCollection;
      assert (fc instanceof PointFeatureCollection) || (fc instanceof NestedPointFeatureCollection) : fc.getClass().getName();
      if (fc instanceof PointFeatureCollection) {
        PointFeatureCollection pfc = (PointFeatureCollection) fc;
        int count = testPointFeatureCollection(pfc, true);
        System.out.println(" getData count= "+count+" size= "+pfc.size());
        assert count == pfc.size();
      }  else
        testNestedPointFeatureCollection((NestedPointFeatureCollection) fc);
    }

    fdataset.close();
    long took = System.currentTimeMillis() - start;
    System.out.println(" took= "+took+" msec");
  }

  void testNestedPointFeatureCollection( NestedPointFeatureCollection npfc) throws IOException {
    PointFeatureCollectionIterator iter = npfc.getPointFeatureCollectionIterator(-1);
    while (iter.hasNext()) {
      PointFeatureCollection pfc = iter.next();
      testPointFeatureCollection(pfc, false);
    }
  }


  int testPointFeatureCollection( PointFeatureCollection pfc, boolean needBB) throws IOException {
    LatLonRect bb = pfc.getBoundingBox();

    long start = System.currentTimeMillis();
    int count = 0;
    pfc.resetIteration();
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      if (bb != null)
        assert bb.contains( pf.getLocation().getLatLon());
      count++;
    }
    long took = System.currentTimeMillis() - start;
    System.out.println("complete count= "+count);
    System.out.println(" full iter took= "+took+" msec");


    bb = pfc.getBoundingBox();
    if (needBB) {
      assert bb != null;
      System.out.println("bb= "+bb.toString2());
    }

    int count2 = 0;
    PointFeatureIterator iter = pfc.getPointFeatureIterator(-1);
    while (iter.hasNext()) {
      PointFeature pf = iter.next();

      if (needBB) {
        assert bb.contains( pf.getLocation().getLatLon()) : bb.toString2() + " does not contains point "+pf.getLocation().getLatLon();
      }

      testPointFeature( pf);
      count2++;
    }
    assert count == count2;

    if (bb != null) {
      // try a subset
      LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getHeight()/2, bb.getWidth()/2);
      PointFeatureCollection subset = pfc.subset(bb2, null);
      System.out.println("subset= "+bb2.toString2());

      start = System.currentTimeMillis();
      int counts = 0;
      PointFeatureIterator iters = subset.getPointFeatureIterator(-1);
      while (iters.hasNext()) {
        PointFeature pf = iters.next();

        if (needBB) {
          assert bb2.contains( pf.getLocation().getLatLon()) : bb2.toString2() + " does not contains point "+pf.getLocation().getLatLon();
        }
        //System.out.printf(" contains point %s%n",pf.getLocation().getLatLon());

        testPointFeature( pf);
        counts++;
      }
      System.out.println("subset count= "+counts);
      took = System.currentTimeMillis() - start;
      System.out.println(" subset iter took= "+took+" msec");
    }

    return count;
  }

  private void testPointFeature( PointFeature pobs) throws java.io.IOException {

    EarthLocation loc = pobs.getLocation();
    assert loc != null;

    assert null != pobs.getNominalTimeAsDate();
    assert null != pobs.getObservationTimeAsDate();

    DateUnit timeUnit = pobs.getTimeUnit();
    assert timeUnit.makeDate( pobs.getNominalTime()).equals( pobs.getNominalTimeAsDate());
    assert timeUnit.makeDate( pobs.getObservationTime()).equals( pobs.getObservationTimeAsDate());

    StructureData sdata = pobs.getData();
    assert null != sdata;
    testData( sdata);
  }

  private void testData( StructureData sdata) {

    for (StructureMembers.Member member : sdata.getMembers()) {
      DataType dt = member.getDataType();
      if (dt == DataType.FLOAT) {
        sdata.getScalarFloat(member);
        sdata.getJavaArrayFloat(member);
      } else if (dt == DataType.DOUBLE) {
        sdata.getScalarDouble(member);
        sdata.getJavaArrayDouble(member);
      } else if (dt == DataType.BYTE) {
        sdata.getScalarByte(member);
        sdata.getJavaArrayByte(member);
      } else if (dt == DataType.SHORT) {
        sdata.getScalarShort(member);
        sdata.getJavaArrayShort(member);
      } else if (dt == DataType.INT) {
        sdata.getScalarInt(member);
        sdata.getJavaArrayInt(member);
      } else if (dt == DataType.LONG) {
        sdata.getScalarLong(member);
        sdata.getJavaArrayLong(member);
      } else if (dt == DataType.CHAR) {
        sdata.getScalarChar(member);
        sdata.getJavaArrayChar(member);
        sdata.getScalarString(member);
      } else if (dt == DataType.STRING) {
        sdata.getScalarString(member);
      }

      if ((dt != DataType.STRING) && (dt != DataType.CHAR) && (dt != DataType.STRUCTURE)) {
        sdata.convertScalarFloat(member.getName());
      }

    }
  }


}

