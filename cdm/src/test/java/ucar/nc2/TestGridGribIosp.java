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
/**
 *
 * By:   Robb Kambic
 * Date: Mar 10, 2009
 * Time: 10:12:28 AM
 *
 */

package ucar.nc2;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.iosp.IOServiceProvider;

import java.io.IOException;
import java.io.File;

import junit.framework.TestCase;

public class TestGridGribIosp extends TestCase {

    public TestGridGribIosp(String name) {
      super(name);
    }

    public void testCompare() throws IOException {
      File where = new File("C:/data/grib/idd");
      if( where.exists() ) {
        String[] args = new String[ 1 ];
        args[ 0 ] = "C:/data/grib/idd";
        doAll( args );
      } else {
        doAll( null );
      }
    }

    void compareNC(String fileBinary, String fileText) throws IOException {

    long start = System.currentTimeMillis() ;
    //String fileBinary = "C:/data/GFS_Global_2p5deg_20090305_0000.grib2";
    //String fileBinary = "C:/data/GFS_Alaska_191km_20090307_1200.grib1";
    //String fileBinary = "C:/data/rotatedlatlon.grb";
    //String fileBinary = "C:/data/NDFD.grib2";
    //String fileBinary = "C:/data/ds.sky.bin";
    //String fileBinary = "C:/data/MPE_M7_57.grb";
    //fileBinary = "/local/robb/data/grib/idd/binary/RUC2_CONUS_20km_pressure_20090220_1900.grib2";
    //String fileText = "C:/data/text/GFS_Global_2p5deg_20090305_0000.grib2";
    //String fileText = "C:/data/text/GFS_Alaska_191km_20090307_1200.grib1";
    //String fileText = "C:/data/text/rotatedlatlon.grb";
    //String fileText = "C:/data/text/NDFD.grib2";
    //String fileText = "C:/data/text/ds.sky.bin";
    //String fileText = "C:/data/text/MPE_M7_57.grb";
    //fileText = "/local/robb/data/grib/idd/text/RUC2_CONUS_20km_pressure_20090220_1900.grib2";

    Class c = ucar.nc2.iosp.grib.GribGridServiceProvider.class;
    IOServiceProvider spiB = null;
    try {
      spiB = (IOServiceProvider) c.newInstance();
    } catch (InstantiationException e) {
      throw new IOException("IOServiceProvider " + c.getName() + "must have no-arg constructor.");
    } catch (IllegalAccessException e) {
      throw new IOException("IOServiceProvider " + c.getName() + " IllegalAccessException: " + e.getMessage());
    }
    ucar.unidata.io.RandomAccessFile rafB = new ucar.unidata.io.RandomAccessFile(fileBinary, "r");
    rafB.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
    NetcdfFile ncfileBinary = new NetcdfFile(spiB, rafB, fileBinary, null);
    //System.out.println( "Time to create Netcdf object using GridGrib Iosp "+
    //  (System.currentTimeMillis() - start) );
    System.out.println( "Binary Netcdf created" );

    start = System.currentTimeMillis();

    Class cT1 = ucar.nc2.iosp.grib.Grib1ServiceProvider.class;
    Class cT2 = ucar.nc2.iosp.grib.Grib2ServiceProvider.class;
    IOServiceProvider spiT = null;
    try {
      spiT = (IOServiceProvider) cT2.newInstance();
    } catch (InstantiationException e) {
      throw new IOException("IOServiceProvider " + cT2.getName() + "must have no-arg constructor.");
    } catch (IllegalAccessException e) {
      throw new IOException("IOServiceProvider " + cT2.getName() + " IllegalAccessException: " + e.getMessage());
    }
    ucar.unidata.io.RandomAccessFile rafT = new ucar.unidata.io.RandomAccessFile(fileText, "r");
    rafT.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
    NetcdfFile ncfileText;
    if( spiT.isValidFile( rafT )) {
      rafT.seek( 0L );
      ncfileText = new NetcdfFile(spiT, rafT, fileText, null);
    } else {
      rafT.seek( 0L );
      try {
        spiT = (IOServiceProvider) cT1.newInstance();
      } catch (InstantiationException e) {
        throw new IOException("IOServiceProvider " + cT1.getName() + "must have no-arg constructor.");
      } catch (IllegalAccessException e) {
        throw new IOException("IOServiceProvider " + cT1.getName() + " IllegalAccessException: " + e.getMessage());
      }
      ncfileText = new NetcdfFile(spiT, rafT, fileText, null);
    }
    System.out.println( "Text Netcdf created" );

      //System.out.println( "Time to create Netcdf object using Grid1 Grib2 Iosp "+
    //  (System.currentTimeMillis() - start) );
    // org,  copy,  _compareData,  _showCompare,  _showEach
    //ucar.nc2.TestCompare.compareFiles(ncfileBinary, ncfileText, true, true, true);
     TestCompare.compareFiles(ncfileBinary, ncfileText, false, true, false);
     ncfileBinary.close();
     ncfileText.close();
  }

  void doAll(String args[]) throws IOException {

    String dirB, dirT;
    if ( args == null || args.length < 1 ) {
      dirB = TestAll.upcShareTestDataDir +"test/motherlode/grid/grib/binary";
      dirT = TestAll.upcShareTestDataDir +"test/motherlode/grid/grib/text";
    } else {
      dirB = args[ 0 ] +"/binary"; // "/local/robb/data/grib/idd/binary";
      dirT = args[ 0 ] +"/text"; // "/local/robb/data/grib/idd/text";
    }
    File dir = new File( dirB );
    if (dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      String[] children = dir.list();
      for (String child : children) {
        //System.out.println( "children i ="+ children[ i ]);
        File aChild = new File(dir, child);
        //System.out.println( "child ="+ child.getName() );
        if (aChild.isDirectory()) {
          continue;
          // skip index *gbx and inventory *xml files
        } else if (
            // can't be displayed by Grib(1|2) iosp
            child.contains( "GFS_Global_1p0deg_Ensemble") ||
            child.contains( "SREF") ||    
            child.endsWith("gbx") ||
            child.endsWith("xml") ||
            child.endsWith("tmp") || //index in creation process
            child.length() == 0) { // zero length file, ugh...
        } else {
          System.out.println( "\n\nComparing File "+ child );
          compareNC( dirB +"/"+ child, dirT +"/"+ child);
        }
      }
    } else {
    }
  }

  static public void main2(String args[]) throws IOException {
      TestGridGribIosp ggi = new TestGridGribIosp( "" );
      if ( args.length < 1 ) {
        args[ 0 ] = "C:/data";
        ggi.doAll( args );
      }
  }
}