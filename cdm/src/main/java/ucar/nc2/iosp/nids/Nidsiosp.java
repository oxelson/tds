// $Id:Nidsiosp.java 63 2006-07-12 21:50:51Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.nids;

import ucar.ma2.*;
import ucar.nc2.*;

import ucar.nc2.units.DateUnit;

import java.io.*;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.nio.ByteBuffer;

/**
 * IOServiceProvider implementation abstract base class to read/write "version 3" netcdf files.
 *  AKA "file format version 1" files.
 *
 *  see   concrete class
 */

public class Nidsiosp implements ucar.nc2.IOServiceProvider {

    protected boolean readonly;
    private ucar.nc2.NetcdfFile ncfile;
    private ucar.unidata.io.RandomAccessFile myRaf;
    // private Nidsheader.Vinfo myInfo;
    protected Nidsheader headerParser;

    final static int Z_DEFLATED = 8;
    final static int DEF_WBITS =  15;

    // used for writing
    protected int fileUsed = 0; // how much of the file is written to ?
    protected int recStart = 0; // where the record data starts

    protected boolean debug = false, debugSize = false, debugSPIO = false;
    protected boolean showHeaderBytes = false;

    public void setSpecial( Object special) {}

    public ucar.ma2.Array readNestedData(ucar.nc2.Variable v2, java.util.List section)
         throws java.io.IOException, ucar.ma2.InvalidRangeException {
       Variable vp = v2.getParentStructure();
       Object data ;
       Array outputData ;
       byte[] vdata ;
       Nidsheader.Vinfo vinfo ;
       ByteBuffer bos ;

       vinfo =  (Nidsheader.Vinfo) vp.getSPobject();

       vdata = headerParser.getUncompData( (int)vinfo.doff, 0);
       bos = ByteBuffer.wrap(vdata);

       if (vp.getName().startsWith("VADWindSpeed") )
       {
            return readNestedWindBarbData( vp.getShortName(), v2.getShortName(), bos, vinfo, section );
       }
       else if(vp.getName().startsWith("unlinkedVectorStruct"))
       {
           return readNestedDataUnlinkVector( vp.getShortName(), v2.getShortName(), bos, vinfo, section );
       }
       else if (vp.getName().equals( "linkedVectorStruct") )
       {
           return readNestedLinkedVectorData(vp.getShortName(), v2.getShortName(), bos, vinfo, section );
       }
       else if (vp.getName().startsWith("textStruct") )
       {
           return readNestedTextStringData(vp.getShortName(), v2.getShortName(), bos, vinfo, section );

       }
       else if (vp.getName().startsWith("VectorArrow") )
       {
           return readNestedVectorArrowData(vp.getShortName(), v2.getShortName(), bos, vinfo, section);
       }
       else if (vp.getName().startsWith("circleStruct") )
       {
           return  readNestedCircleStructData(vp.getShortName(), v2.getShortName(), bos, vinfo, section);
       }
       else
       {
           throw new UnsupportedOperationException("Nids IOSP does not support nested variables");
       }

       // return null;
    }

    public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf )
    {
        Nidsheader localHeader = new Nidsheader();
        return( localHeader.isValidFile( raf ));
    }

  /////////////////////////////////////////////////////////////////////////////
  // reading

    public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile file,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {
        ncfile = file;
        myRaf = raf;

        headerParser = new Nidsheader();
        headerParser.read(myRaf, ncfile );
        //myInfo = headerParser.getVarInfo();

        ncfile.finish();
    }


    public Array readData(Variable v2, List sectionList) throws IOException, InvalidRangeException  {
    // subset
        Object data ;
        Array outputData ;
        byte[] vdata ;
        Nidsheader.Vinfo vinfo ;
        ByteBuffer bos ;

        vinfo =  (Nidsheader.Vinfo) v2.getSPobject();

       /*
        if (vinfo.isZlibed  )
            vdata = readCompData(vinfo.hoff, vinfo.doff);
        else
            vdata = readUCompData(vinfo.hoff, vinfo.doff);

        ByteBuffer bos = ByteBuffer.wrap(vdata);     */

        vdata = headerParser.getUncompData( (int)vinfo.doff, 0);
        bos = ByteBuffer.wrap(vdata);


        if ( v2.getName().equals( "azimuth") )
        {
            data = readRadialDataAzi(bos, vinfo);
            outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if (v2.getName().equals( "gate")  )
        {
            data = readRadialDataGate(vinfo);
            outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if ( v2.getName().equals( "elevation"))
        {
            data = readRadialDataEle(bos, vinfo);
            outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if ( v2.getName().equals( "latitude"))
        {
            double lat = ncfile.findGlobalAttribute("RadarLatitude").getNumericValue().doubleValue();
            data = readRadialDataLatLonAlt(lat, vinfo);
            outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if ( v2.getName().equals( "longitude"))
        {
            double lon = ncfile.findGlobalAttribute("RadarLongitude").getNumericValue().doubleValue();
            data = readRadialDataLatLonAlt(lon, vinfo);
            outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if ( v2.getName().equals( "altitude"))
        {
            double alt = ncfile.findGlobalAttribute("RadarAltitude").getNumericValue().doubleValue();
            data = readRadialDataLatLonAlt(alt, vinfo);
            outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if (v2.getName().equals( "distance"))
        {
            data = readDistance(vinfo);
            outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if ( v2.getName().equals( "rays_time"))
        {
            String rt = ncfile.findGlobalAttribute("DateCreated").getStringValue();
            java.util.Date pDate = DateUnit.getStandardOrISO(rt);
            double lt = pDate.getTime();
            double [] dd = new double[ vinfo.yt];
            for (int radial=0; radial<vinfo.yt; radial++) {
                dd[radial] = (float) lt;
            }
            outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), dd);
        }
        else if (v2.getName().startsWith( "EchoTop") || v2.getName().startsWith( "VertLiquid") || v2.getName().startsWith( "BaseReflectivityComp"))
        {
            data = readOneArrayData(bos, vinfo, v2.getName() );
            outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if (v2.getName().startsWith( "PrecipArray") )
        {
            data = readOneArrayData1(bos, vinfo );
            outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if (v2.getName().equals( "unlinkedVectorStruct") )
        {
            return readUnlinkedVectorData(v2.getName(),bos, vinfo );
            // JOHN outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if (v2.getName().equals( "linkedVectorStruct") )
        {
            return readLinkedVectorData(v2.getName(),bos, vinfo );
        }
        else if (v2.getName().startsWith("textStruct") )
        {
            return readTextStringData(v2.getName(),bos, vinfo );
            // JOHN outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if (v2.getName().startsWith("VADWindSpeed") )
        {
            return readWindBarbData(v2.getName(), bos, vinfo, null );
            // JOHN outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if (v2.getName().startsWith("VectorArrow") )
        {
            return readVectorArrowData(v2.getName(),bos, vinfo );
            // JOHN outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if (v2.getName().startsWith("TabMessagePage") )
        {
            data = readTabAlphaNumData(bos, vinfo );
            outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }
        else if (v2.getName().startsWith("circleStruct") )
        {
              return  readCircleStructData(v2.getName(),bos, vinfo );
        }
        else if (v2.getName().startsWith("hail") || v2.getName().startsWith("TVS") )
        {
              return  readGraphicSymbolData(v2.getName(),bos, vinfo );
        }
        else
        {

            data = readOneScanData(bos, vinfo, v2.getName() );
            outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        }

        return outputData;
  }
   /**
     * Read nested data
     *
     * @param  name Variable name,
    *  @param  m Structure mumber name,
    *  @param  bos data buffer,
    *  @param   vinfo variable info,
    *  @param   section variable section
     * @return the array  of member variable data
  */
   public Array readNestedGraphicSymbolData( String name, StructureMembers.Member m, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                            java.util.List section ) throws IOException, InvalidRangeException  {
      int[] pos = vinfo.pos;
      int size = pos.length;
      Structure pdata = (Structure) ncfile.findVariable( name);

      ArrayStructure ma = readCircleStructData(name, bos, vinfo);

      short [] pa = new short[size];
      for(int i = 0; i<size; i++ ){
         pa[i] = ma.getScalarShort(i, m);
      }

      Array ay = Array.factory( short.class, pdata.getShape(), pa);
      return ay.sectionNoReduce(section);
   }
  /**
     * Read data
    *  @param  name Variable name
    *  @param  bos data buffer,
    *  @param   vinfo variable info,
     * @return the arraystructure of graphic symbol data
  */
  public ArrayStructure readGraphicSymbolData( String name, ByteBuffer bos, Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {

      int[] pos = vinfo.pos;
      int[] sizes = vinfo.len;
      int size = pos.length;

      Structure pdata = (Structure) ncfile.findVariable( name);

      StructureMembers members = pdata.makeStructureMembers();
      members.findMember("x_start").setDataParam(0);
      members.findMember("y_start").setDataParam(2);

      return new MyArrayStructureBBpos(members, new int[] { size}, bos, pos, sizes);


      /*
      int[] pos = vinfo.pos;
      int[] dlen = vinfo.len;
      int size = pos.length;
      int vlen = 0;

      for(int i=0; i< size ; i++ ){
             vlen = vlen + dlen[i];
      }

      Structure pdata = (Structure) ncfile.findVariable( name);
      StructureMembers members = pdata.makeStructureMembers();
      members.findMember("x_start");
      members.findMember("y_start");

     // return new ArrayStructureBBpos(members, new int[] {size}, bos, pos );
      short istart;
      short jstart;

     ArrayStructureW asw = new ArrayStructureW(members, new int[] {vlen});

      int ii = 0;
      for (int i=0; i< size; i++) {
         bos.position( pos[i] );

         for( int j = 0; j < dlen[i]; j++ ) {
           StructureDataW sdata = new StructureDataW(asw.getStructureMembers());
           Iterator memberIter = sdata.getMembers().iterator();

           ArrayShort.D0 sArray ;
           istart = bos.getShort();
           jstart = bos.getShort();

           sArray = new ArrayShort.D0();
           sArray.set( istart );
           sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

           sArray = new ArrayShort.D0();
           sArray.set( jstart );
           sdata.setMemberData((StructureMembers.Member) memberIter.next(), sArray);

           asw.setStructureData(sdata, ii);
           ii++;
         }
      }   //end of for loop

      return asw;
      */
   }

  /**
     * Read nested data
     *
    *  @param  name Variable name,
    *  @param  memberName mumber name,
    *  @param  bos data buffer,
    *  @param  vinfo variable info,
    *  @param  section variable section
     * @return the array  of member variable data
  */
  public Array readNestedLinkedVectorData( String name, String memberName, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                            java.util.List section ) throws IOException, InvalidRangeException  {

      Structure pdata = (Structure) ncfile.findVariable( name);
      ArrayStructure ma = readLinkedVectorData(name, bos, vinfo);
      int size = (int) pdata.getSize();
      StructureMembers members = ma.getStructureMembers();
      StructureMembers.Member m = members.findMember(memberName);

      short [] pa = new short[size];
      for(int i = 0; i<size; i++ ){
         pa[i] = ma.getScalarShort(i, m);
      }

      Array ay = Array.factory( short.class, pdata.getShape(), pa);
      return ay.sectionNoReduce(section);

  }
   /**
     * Read data
    *  @param  name Variable name,
    *  @param  bos data buffer,
    *  @param   vinfo variable info,
    *  @return the arraystructure of linked vector data
  */
  public ArrayStructure readLinkedVectorData( String name, ByteBuffer bos, Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {
      int[] pos = vinfo.pos;
      int[] dlen = vinfo.len;
      bos.position(0);

      int size = pos.length;
      int vlen = 0;
      for(int i=0; i< size ; i++ ){
             vlen = vlen + dlen[i];
      }

      Structure pdata = (Structure) ncfile.findVariable( name);
      StructureMembers members = pdata.makeStructureMembers();
      // Structure pdata = new Structure(ncfile, null, null,"unlinkedVector" );
      short istart;
      short jstart;
      short iend;
      short jend;
      short sValue = 0;
      int ii = 0;
      short[][] sArray = new short[5][vlen];
      for (int i=0; i< size; i++) {
         bos.position( pos[i] );
         if(vinfo.code == 9) {
            sValue = bos.getShort();
         }
         istart = bos.getShort();
         jstart = bos.getShort();

         for( int j = 0; j < dlen[i]; j++ ) {
           iend = bos.getShort();
           jend = bos.getShort();

           if(vinfo.code == 9) {
               sArray[0][ii] = sValue;
            }
           sArray[1][ii] = istart;
           sArray[2][ii] = jstart;
           sArray[3][ii] = iend;
           sArray[4][ii] = jend;

           ii++;
         }
      }   //end of for loop of read data

      ArrayStructureMA asma = new ArrayStructureMA( members, new int[] {vlen});
      Array data;
   // these are the offsets into the record
      data = Array.factory( short.class,  new int[] { vlen}, sArray[0]);
      StructureMembers.Member m = members.findMember("sValue");
      if( m != null ) m.setDataObject( data);
      data = Array.factory( short.class,  new int[] { vlen}, sArray[1]);
      m = members.findMember("x_start");
      m.setDataObject( data);
      data = Array.factory( short.class,  new int[] { vlen}, sArray[2]);
      m = members.findMember("y_start");
      m.setDataObject( data);
      data = Array.factory( short.class,  new int[] { vlen}, sArray[3]);
      m = members.findMember("x_end");
      m.setDataObject( data);
      data = Array.factory( short.class,  new int[] { vlen}, sArray[4]);
      m = members.findMember("y_end");
      m.setDataObject( data);
      return asma;
  }
   /**
     * Read nested data
    *  @param  name Variable name,
    *  @param  memberName Structure mumber name,
    *  @param  bos Data buffer,
    *  @param   vinfo variable info,
    *  @param   section variable section
     * @return the array  of member variable data
  */
   public Array readNestedCircleStructData( String name, String memberName, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                            java.util.List section) throws IOException, InvalidRangeException  {

      Structure pdata = (Structure) ncfile.findVariable( name);
      ArrayStructure ma = readCircleStructData(name, bos, vinfo);
      int size = (int) pdata.getSize();
      StructureMembers members = ma.getStructureMembers();
      StructureMembers.Member m = members.findMember(memberName);

      short [] pa = new short[size];
      for(int i = 0; i<size; i++ ){
         pa[i] = ma.getScalarShort(i, m);
      }

      Array ay = Array.factory( short.class, pdata.getShape(), pa);
      return ay.sectionNoReduce(section);

   }
  /**
     * Read data
     *
     *  @param  name Variable name,
     *  @param  bos Data buffer,
     *  @param   vinfo variable info,
     * @return the arraystructure of circle struct data
  */
  public ArrayStructure readCircleStructData( String name, ByteBuffer bos, Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {
      int[] pos = vinfo.pos;
      int size = pos.length;

      Structure pdata = (Structure) ncfile.findVariable( name);

      int recsize = pos[1] - pos[0]; // each record  must be all the same size
      for (int i=1; i< size; i++) {
        int r = pos[i] - pos[i-1];
        if (r != recsize) System.out.println(" PROBLEM at "+i+" == "+r);
      }

      StructureMembers members = pdata.makeStructureMembers();

      members.findMember("x_center").setDataParam(0);
      members.findMember("y_center").setDataParam(2);
      members.findMember("radius").setDataParam(4);
      members.setStructureSize( recsize);
      return new ArrayStructureBBpos(members, new int[] { size}, bos, pos );

   }

   /**
     * Read data

    *  @param  bos Data buffer,
    *  @param   vinfo variable info,
     * @return the array of tab data
  */
   public Object readTabAlphaNumData( ByteBuffer bos, Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {
      int plen = vinfo.xt;
      int tablen = vinfo.yt;
      String[] pdata = new String[ plen];
      bos.position(0);
      int llen ;
      int ipage = 0;
      int icnt = 4;
      StringBuffer sbuf = new StringBuffer();


      while( ipage < plen && (tablen > 128 + icnt )) {
          llen = bos.getShort();
          if( llen == -1 ) {
              pdata[ipage] = new String(sbuf);
              sbuf = new StringBuffer();
              ipage++;
              icnt = icnt + 2;
              continue;
          }

          byte[] b = new byte[llen];
          bos.get(b);
          String sl = new String(b) + "\n";
          sbuf.append(sl);
          icnt = icnt + llen + 2;
      }

      return pdata;

    }
    /**
     * Read data
     *
     * @param  bos Data buffer
     * @param  vinfo variable info
     * @return the data object of scan data
  */
    // all the work is here, so can be called recursively
    public Object readOneScanData( ByteBuffer bos, Nidsheader.Vinfo vinfo, String vName ) throws IOException, InvalidRangeException  {
      int doff = 0;
      int npixel = vinfo.yt * vinfo.xt;
      byte[] odata = new byte[ vinfo.xt];
      byte[] pdata = new byte[ npixel];

      bos.position(0);
      for (int radial=0; radial<vinfo.yt; radial++) {
        //bos.get(b2, 0, 2);
        //int test = getInt(b2, 0, 2);
        int runLen = bos.getShort();   //   getInt(vdata, doff, 2 );
        doff += 2;
        if(vinfo.isRadial){
            int radialAngle = bos.getShort();
            doff += 2;
            int radialAngleD = bos.getShort();
            doff += 2;
        }
        byte[] rdata = new byte[runLen*2];

        int tmpp = bos.remaining();
        bos.get(rdata, 0, runLen*2);
        doff += runLen * 2;
        byte[] bdata = readOneBeamData(rdata, runLen, vinfo.xt, doff );


        if ( vinfo.x0 > 0 ) {
            for (int i= 0 ; i<vinfo.x0; i++ ) {
                odata[i] = 0;
            }
        }
        System.arraycopy(bdata, 0, odata, vinfo.x0, bdata.length);

        // copy into odata
        System.arraycopy(odata, 0, pdata, vinfo.xt * radial, vinfo.xt);

      }   //end of for loop
      int offset = 0;
      if( vName.endsWith( "_RAW" ) ) {
           return pdata;
      }
      else if ( vName.startsWith( "BaseReflectivity" ) ) {
          int [] levels = vinfo.len;
          int iscale = vinfo.code;
          float [] fdata = new float[npixel];
          for (int i = 0; i < npixel; i++ ) {
            int ival = levels[pdata[i]];
            if ( ival > -9997 )
              fdata[i] = (float)ival / (float)iscale + (float )offset;
            else
              fdata[i] = Float.NaN;
          }

          return fdata;

      } else if ( vName.startsWith( "RadialVelocity" )  ) {
          int [] levels = vinfo.len;
          int iscale = vinfo.code;
          float [] fdata = new float[npixel];
          for (int i = 0; i < npixel; i++ ) {
                 int ival = levels[pdata[i]];
                 if ( ival > -9996 )
                   fdata[i] = (float)ival / (float)iscale + (float )offset;
                 else
                   fdata[i] = Float.NaN;
          }
          return fdata;

      } else if ( vName.startsWith( "StormMeanVelocity" ) ) {
          int [] levels = vinfo.len;
          int iscale = vinfo.code;
          float [] fdata = new float[npixel];
          for (int i = 0; i < npixel; i++ ) {
                 int ival = levels[pdata[i]];
                 if ( ival > -9996 )
                   fdata[i] = (float)ival / (float)iscale + (float )offset;
                 else
                   fdata[i] = Float.NaN;
           }
           return fdata;

      }  else if (vName.startsWith("Precip") ) {
          int [] levels = vinfo.len;
          int iscale = vinfo.code;
          float [] fdata = new float[npixel];
          for (int i = 0; i < npixel; i++ ) {
            int ival = levels[pdata[i]];
            if ( ival > -9996 )
                fdata[i] =  ((float)ival / (float)iscale + (float)offset);
            else
              fdata[i] = Float.NaN; //100 * ival;
          }
          return fdata;
    }



        /*else if(vName.endsWith( "_Brightness" )){
         float ratio = 256.0f/vinfo.level;

         float [] fdata = new float[npixel];
         for ( int i = 0; i < vinfo.yt * vinfo.xt; i++ ) {
                fdata[i] = pdata[i] * ratio;
          }
         return fdata;

     }  else if ( vName.endsWith( "_VIP" )) {
         int [] levels = vinfo.len;
         int iscale = vinfo.code;
         int [] dvip ={ 0, 30, 40, 45, 50, 55 };
         float [] fdata = new float[npixel];
         for (int i = 0; i < npixel; i++ ) {
           float dbz = levels[pdata[i]] / iscale + offset;
           for (int j = 0; j <= 5; j++ ) {
             if ( dbz > dvip[j] ) fdata[i] = j + 1;
           }
         }
         return fdata;

     }   */
       return null;
    }



    public byte[] readOneBeamData(byte[] ddata, int rLen, int xt, int off ) throws IOException, InvalidRangeException  {
         int run ;
         byte[] bdata = new byte[xt];

         int nbin = 0;
         int total = 0;
         for ( run = 0; run < rLen*2; run++)
         {
             int drun = convertunsignedByte2Short(ddata[run]) >> 4;
             byte dcode1 = (byte) (convertunsignedByte2Short(ddata[run]) & 0Xf );
             for (int i = 0; i< drun; i++ ) {
                  bdata[nbin++]= dcode1;
                  total++;
             }
         }

         if(total < xt) {
            for ( run = total; run < xt; run++)
            {
                bdata[run]= 0;
            }
         }

         return bdata;
     }
    /**
     * Read nested data
    *  @param  name Variable name,
    *  @param  memberName Structure mumber name,
    *  @param  bos Data buffer,
    *  @param   vinfo variable info,
    *  @param   section variable section
     * @return the array  of member variable data
   */
    public Array readNestedWindBarbData( String name, String memberName, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                         java.util.List section ) throws IOException, InvalidRangeException  {

      Structure pdata = (Structure) ncfile.findVariable( name);
      ArrayStructure ma = readWindBarbData(name, bos, vinfo, null);
      int size = (int) pdata.getSize();
      StructureMembers members = ma.getStructureMembers();
      StructureMembers.Member m = members.findMember(memberName);

      short [] pa = new short[size];
      for(int i = 0; i<size; i++ ){
         pa[i] = ma.getScalarShort(i, m);
      }

      Array ay = Array.factory( short.class, pdata.getShape(), pa);
      return ay.sectionNoReduce(section);

      //return asbb;

    }

    /**
     * Read data
    *  @param  name Variable name,
    *  @param  bos Data buffer,
    *  @param   vinfo variable info,
     * @return the arraystructure of wind barb data
  */
   public ArrayStructure readWindBarbData( String name, ByteBuffer bos, Nidsheader.Vinfo vinfo, List sList ) throws IOException, InvalidRangeException  {
      int[] pos = vinfo.pos;
      int size = pos.length;

      Structure pdata = (Structure) ncfile.findVariable( name);


      int recsize;
       if(size > 1) {
          recsize = pos[1] - pos[0]; // each record  must be all the same size
          for (int i=1; i< size; i++) {
             int r = pos[i] - pos[i-1];
             if (r != recsize) System.out.println(" PROBLEM at "+i+" == "+r);
          }
       }
       else
          recsize = 1;


      StructureMembers members = pdata.makeStructureMembers();
      members.findMember("value").setDataParam(0); // these are the offsets into the record
      members.findMember("x_start").setDataParam(2);
      members.findMember("y_start").setDataParam(4);
      members.findMember("direction").setDataParam(6);
      members.findMember("speed").setDataParam(8);
      members.setStructureSize( recsize);

      ArrayStructure ay =  new ArrayStructureBBpos( members, new int[] {size}, bos, pos);

      return (sList != null) ? (ArrayStructure) ay.sectionNoReduce(sList): ay;

      // return new ArrayStructureBBpos( members, new int[] { size}, bos, pos);
      //return asbb;

    }
    /**
     * Read nested data
    *  @param  name Variable name,
    *  @param  memberName Structure mumber name,
    *  @param  bos Data buffer,
    *  @param   vinfo variable info,
    *  @param   section variable section
     * @return the array  of member variable data
   */
    public Array readNestedVectorArrowData( String name, String memberName, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                            java.util.List section  ) throws IOException, InvalidRangeException  {

      Structure pdata = (Structure) ncfile.findVariable( name);
      ArrayStructure ma = readVectorArrowData(name, bos, vinfo);
      int size = (int) pdata.getSize();
      StructureMembers members = ma.getStructureMembers();
      StructureMembers.Member m = members.findMember(memberName);


      short [] pa = new short[size];
      for(int i = 0; i<size; i++ ){
         pa[i] = ma.getScalarShort(i, m);
      }

      Array ay = Array.factory( short.class, pdata.getShape(), pa);
      return ay.sectionNoReduce(section);

    }
    /**
     * Read data
    *  @param  name Variable name,
    *  @param  bos Data buffer,
    *  @param   vinfo variable info,
     * @return the arraystructure of vector arrow data
  */
    public ArrayStructure readVectorArrowData( String name, ByteBuffer bos, Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {
      int[] pos = vinfo.pos;
      int size = pos.length;
     /* short istart = 0;
      short jstart = 0;
      short direction = 0;
      short arrowvalue = 0;
      short arrowHeadValue = 0;    */

      Structure pdata = (Structure) ncfile.findVariable( name);
      int recsize = pos[1] - pos[0]; // each record  must be all the same size
      for (int i=1; i< size; i++) {
        int r = pos[i] - pos[i-1];
        if (r != recsize) System.out.println(" PROBLEM at "+i+" == "+r);
      }

      StructureMembers members = pdata.makeStructureMembers();
      members.findMember("x_start").setDataParam(0);
      members.findMember("y_start").setDataParam(2);
      members.findMember("direction").setDataParam(4);
      members.findMember("arrowLength").setDataParam(6);
      members.findMember("arrowHeadLength").setDataParam(8);

      members.setStructureSize( recsize);
      return new ArrayStructureBBpos( members, new int[] { size}, bos, pos);
        /*
      Structure pdata = new Structure(ncfile, null, null,"vectorArrow" );

      Variable ii0 = new Variable(ncfile, null, pdata, "x_start");
      ii0.setDimensions((String)null);
      ii0.setDataType(DataType.SHORT);
      pdata.addMemberVariable(ii0);

      Variable ii1 = new Variable(ncfile, null, pdata, "y_start");
      ii1.setDimensions((String)null);
      ii1.setDataType(DataType.SHORT);
      pdata.addMemberVariable(ii1);

      Variable direct = new Variable(ncfile, null, pdata, "direction");
      direct.setDimensions((String)null);
      direct.setDataType(DataType.SHORT);
      pdata.addMemberVariable(direct);

      Variable speed = new Variable(ncfile, null, pdata, "arrowLength");
      speed.setDimensions((String)null);
      speed.setDataType(DataType.SHORT);
      pdata.addMemberVariable(speed);

      Variable  v = new Variable(ncfile, null, null, "arrowHeadLength");
      v.setDataType(DataType.SHORT);
      v.setDimensions((String)null);
      pdata.addMemberVariable(v);

     StructureMembers members = pdata.makeStructureMembers();
     ArrayStructureW asw = new ArrayStructureW(members, new int[] {size});

      for (int i=0; i< size; i++) {
          bos.position( pos[i]);

          istart = bos.getShort();
          jstart = bos.getShort();
          direction = bos.getShort();
          arrowvalue = bos.getShort();
          arrowHeadValue = bos.getShort();

          ArrayStructureW.StructureDataW sdata = asw.new StructureDataW();
          Iterator memberIter = sdata.getMembers().iterator();

          ArrayObject.D0 sArray = new ArrayObject.D0(Short.class);
          sArray.set(new Short(istart));
          sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

          sArray = new ArrayObject.D0(Short.class);
          sArray.set(new Short(jstart));
          sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

          sArray = new ArrayObject.D0(String.class);
          sArray.set(new Short(direction));
          sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

          sArray = new ArrayObject.D0(Short.class);
          sArray.set(new Short(arrowvalue));
          sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

          sArray = new ArrayObject.D0(String.class);
          sArray.set(new Short(arrowHeadValue));
          sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

          asw.setStructureData(sdata, i);
      }   //end of for loop
       */
     // return asw;

    }

    /**
     * Read nested data
    *  @param  name Variable name,
    *  @param  memberName Structure mumber name,
    *  @param  bos Data buffer,
    *  @param   vinfo variable info,
    *  @param   section variable section
     * @return the array  of member variable data
   */
    public Array readNestedTextStringData( String name, String memberName, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                            java.util.List section  ) throws IOException, InvalidRangeException  {

      Structure pdata = (Structure) ncfile.findVariable( name);
      ArrayStructure ma = readTextStringData(name, bos, vinfo);
      int size = (int) pdata.getSize();
      StructureMembers members = ma.getStructureMembers();
      StructureMembers.Member m = members.findMember(memberName);

      Array ay ;
      short [] pa = new short[size];
      String [] ps = new String[size];
      if( m.getName().equalsIgnoreCase("testString")) {
        for(int i = 0; i<size; i++ ){
           ps[i] = ma.getScalarString(i, m);
        }

        ay = Array.factory( String.class, pdata.getShape(), ps);

      } else  {
         for(int i = 0; i<size; i++ ){
           pa[i] = ma.getScalarShort(i, m);
        }

        ay = Array.factory( short.class, pdata.getShape(), pa);
      }
       return ay.sectionNoReduce(section);
    }

    /**
     * Read data
    *  @param  name Variable name,
    *  @param  bos Data buffer,
    *  @param   vinfo variable info
     * @return the arraystructure of text string data
  */
    public ArrayStructure readTextStringData( String name, ByteBuffer bos, Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {
      int[] pos = vinfo.pos;
      int[] sizes = vinfo.len;
      int size = pos.length;

      Structure pdata = (Structure) ncfile.findVariable( name);

      StructureMembers members = pdata.makeStructureMembers();
      if(vinfo.code == 8){
          members.findMember("strValue").setDataParam(0);
          members.findMember("x_start").setDataParam(2);
          members.findMember("y_start").setDataParam(4);
          members.findMember("textString").setDataParam(6);
      }
      else
      {
          members.findMember("x_start").setDataParam(0);
          members.findMember("y_start").setDataParam(2);
          members.findMember("textString").setDataParam(4);
      }

      return new MyArrayStructureBBpos(members, new int[] { size}, bos, pos, sizes);
      //StructureData[] outdata = new StructureData[size];
      // Structure pdata = new Structure(ncfile, null, null,"textdata" );
   /*   short istart = 0;
      short jstart = 0;
      short sValue = 0;

     Variable ii0 = new Variable(ncfile, null, pdata, "x_start");
      ii0.setDimensions((String)null);
      ii0.setDataType(DataType.SHORT);
      pdata.addMemberVariable(ii0);
      Variable ii1 = new Variable(ncfile, null, pdata, "y_start");
      ii1.setDimensions((String)null);
      ii1.setDataType(DataType.SHORT);
      pdata.addMemberVariable(ii1);
      Variable jj0 = new Variable(ncfile, null, pdata, "textString");
      jj0.setDimensions((String)null);
      jj0.setDataType(DataType.STRING);
      pdata.addMemberVariable(jj0);

      if(vinfo.code == 8){
          Variable  v = new Variable(ncfile, null, null, "strValue");
          v.setDataType(DataType.SHORT);
          v.setDimensions((String)null);
          pdata.addMemberVariable(v);
      }

     StructureMembers members = pdata.makeStructureMembers();
     ArrayStructureW asw = new ArrayStructureW(members, new int[] {size});

      for (int i=0; i< size; i++) {
          bos.position( pos[i] - 2);    //re read the length of block
          int strLen = bos.getShort();

          if(vinfo.code == 8) {
              strLen = strLen - 6;
              sValue = bos.getShort();
          } else {
              strLen = strLen - 4;
          }
          byte[] bb = new byte[strLen];
          ArrayStructureW.StructureDataW sdata = asw.new StructureDataW();
          Iterator memberIter = sdata.getMembers().iterator();

          ArrayObject.D0 sArray = new ArrayObject.D0(Short.class);
          istart = bos.getShort();
          jstart = bos.getShort();
          bos.get(bb);
          String tstring = new String(bb);

          sArray.set(new Short(istart));
          sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

          sArray = new ArrayObject.D0(Short.class);
          sArray.set(new Short(jstart));
          sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

          sArray = new ArrayObject.D0(String.class);
          sArray.set(new String(tstring));
          sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

          if(vinfo.code == 8) {
              sArray = new ArrayObject.D0(Short.class);
              sArray.set(new Short(sValue));
              sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);
          }

          asw.setStructureData(sdata, i);

      }   //end of for loop
                                     */
      //return asw;

    }

    private class MyArrayStructureBBpos extends ArrayStructureBBpos {
      int[] size;

      MyArrayStructureBBpos(StructureMembers members, int[] shape, ByteBuffer bbuffer, int[] positions, int[] size) {
        super(members, shape, bbuffer, positions);
        this.size = size;
      }

      public String getScalarString(int recnum, StructureMembers.Member m) {
        if ((m.getDataType() == DataType.CHAR) || (m.getDataType() == DataType.STRING)) {
          int offset = calcOffset(recnum, m);
          int count = size[recnum];
          byte[] pa = new byte[count];
          int i;
          for (i = 0; i < count; i++) {
            pa[i] = bbuffer.get(offset + i);
            if (0 == pa[i]) break;
          }
          return new String(pa, 0, i);
        }

        throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be String or char");
      }

    }

    /**
     * Read nested data
    *  @param  name Variable name,
    *  @param  memberName Structure mumber name,
    *  @param  bos Data buffer,
    *  @param   vinfo variable info,
    *  @param   section variable section
     * @return the array  of member variable data
   */
    public Array readNestedDataUnlinkVector(String name, String memberName, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                            java.util.List section) throws java.io.IOException, ucar.ma2.InvalidRangeException {

      Structure pdata = (Structure) ncfile.findVariable( name);
      ArrayStructure ma = readUnlinkedVectorData(name, bos, vinfo);
      int size = (int) pdata.getSize();
      StructureMembers members = ma.getStructureMembers();
      StructureMembers.Member m = members.findMember(memberName);

      short [] pa = new short[size];
      for(int i = 0; i<size; i++ ){
         pa[i] = ma.getScalarShort(i, m);
      }

      Array ay = Array.factory( short.class, pdata.getShape(), pa);
      return ay.sectionNoReduce(section);
    }

    /**
     * Read data
     *
    *  @param  name Variable name,
    *  @param  bos Data buffer,
    *  @param   vinfo variable info,
     * @return the arraystructure of unlinked vector data
   */
    public ArrayStructure readUnlinkedVectorData( String name, ByteBuffer bos, Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {
      int[] pos = vinfo.pos;
      int[] dlen = vinfo.len;
      bos.position(0);

      int size = pos.length;
      int vlen = 0;
      for(int i=0; i< size ; i++ ){
             vlen = vlen + dlen[i];
      }

      Structure pdata = (Structure) ncfile.findVariable( name);
      StructureMembers members = pdata.makeStructureMembers();

      // Structure pdata = new Structure(ncfile, null, null,"unlinkedVector" );
      short istart ;
      short jstart ;
      short iend ;
      short jend ;
      short vlevel ;

      ArrayStructureMA asma = new ArrayStructureMA( members, new int[] {vlen});
      int ii = 0;
      short[][] sArray = new short[5][vlen];
      for (int i=0; i< size; i++) {
         bos.position( pos[i] );
         vlevel = bos.getShort();

         for( int j = 0; j < dlen[i]; j++ ) {

           istart = bos.getShort();
           jstart = bos.getShort();
           iend = bos.getShort();
           jend = bos.getShort();
           sArray[0][ii] = vlevel;
           sArray[1][ii] = istart;
           sArray[2][ii] = jstart;
           sArray[3][ii] = iend;
           sArray[4][ii] = jend;

           ii++;
         }
      }   //end of for loop

      Array data;
   // these are the offsets into the record
      data = Array.factory( short.class,  new int[] { vlen}, sArray[0]);
      StructureMembers.Member m = members.findMember("iValue");
      m.setDataObject( data);
      data = Array.factory( short.class,  new int[] { vlen}, sArray[1]);
      m = members.findMember("x_start");
      m.setDataObject( data);
      data = Array.factory( short.class,  new int[] { vlen}, sArray[2]);
      m = members.findMember("y_start");
      m.setDataObject( data);
      data = Array.factory( short.class,  new int[] { vlen}, sArray[3]);
      m = members.findMember("x_end");
      m.setDataObject( data);
      data = Array.factory( short.class,  new int[] { vlen}, sArray[4]);
      m = members.findMember("y_end");
      m.setDataObject( data);
      return asma;

    }


    // all the work is here, so can be called recursively
    public Object readOneArrayData( ByteBuffer bos, Nidsheader.Vinfo vinfo, String vName ) throws IOException, InvalidRangeException  {
      int doff = 0;
      int offset = 0;
      //byte[] odata = new byte[ vinfo.xt];
      byte[] pdata = new byte[ vinfo.yt * vinfo.xt];
      byte[] b2 = new byte[2];
      int npixel = vinfo.yt * vinfo.xt;
      //int t = 0;
      bos.position(0);

      for (int radial=0; radial<vinfo.yt; radial++) {

        bos.get(b2);
        int runLen = getUInt(b2, 0, 2); //bos.getShort();   //   getInt(vdata, doff, 2 );
        doff += 2;

        byte[] rdata = new byte[runLen];

        int tmpp = bos.remaining();
        bos.get(rdata, 0, runLen);
        doff += runLen;
        byte[] bdata = readOneRowData(rdata, runLen, vinfo.xt );

        // copy into odata
        System.arraycopy(bdata, 0, pdata, vinfo.xt * radial, vinfo.xt);

      }   //end of for loop

      if( vName.endsWith( "_RAW" ) ) {
           return pdata;
      }
      else if ( vName.equals( "EchoTop" ) || vName.equals( "VertLiquid" ) ) {
          int [] levels = vinfo.len;
          int iscale = vinfo.code;
          float [] fdata = new float[npixel];
          for (int i = 0; i < npixel; i++ ) {
            int ival = levels[pdata[i]];
            if ( ival > -9996 )
              fdata[i] = (float)ival / (float)iscale + (float )offset;
            else
              fdata[i] = Float.NaN;
          }

          return fdata;

      } else if ( vName.startsWith( "BaseReflectivityComp" )  ) {
          int [] levels = vinfo.len;
          int iscale = vinfo.code;
          float [] fdata = new float[npixel];
          for (int i = 0; i < npixel; i++ ) {
                 int ival = levels[pdata[i]];
                 if ( ival > -9997 )
                   fdata[i] = (float)ival / (float)iscale + (float )offset;
                 else
                   fdata[i] = Float.NaN;
          }
          return fdata;

      }

      return null;

    }
    /**
     * Read data
     *
     * @param  bos is data buffer
     * @param  vinfo is variable info
     * @return the data object
  */
    public Object readOneArrayData1( ByteBuffer bos, Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {
      int doff = 0;
      //byte[] odata = new byte[ vinfo.xt];
      byte[] pdata = new byte[ vinfo.yt * vinfo.xt];
      //byte[] b2 = new byte[2];
      //int t = 0;
      bos.position(0);

      for (int row=0; row<vinfo.yt; row++) {
        int runLen = bos.getShort();   //   getInt(vdata, doff, 2 );
        doff += 2;

        byte[] rdata = new byte[runLen];
        int tmpp = bos.remaining();
        bos.get(rdata, 0, runLen);
        doff += runLen;
        byte[] bdata ;
        if(vinfo.code == 17){
             bdata = readOneRowData1(rdata, runLen, vinfo.xt );
        } else {
             bdata = readOneRowData(rdata, runLen, vinfo.xt );
        }
        // copy into odata
        System.arraycopy(bdata, 0, pdata, vinfo.xt * row, vinfo.xt);
      }   //end of for loop

      return pdata;

    }

    /**
     * Read data from encoded values and run len into regular data array
     *
     * @param  ddata is encoded data values
     * @return the data array of row data
  */
    public byte[] readOneRowData1(byte[] ddata, int rLen, int xt ) throws IOException, InvalidRangeException  {
         int run ;
         byte[] bdata = new byte[xt];

         int nbin = 0;
         int total = 0;
         for ( run = 0; run < rLen/2; run++)
         {
             int drun = convertunsignedByte2Short(ddata[run]);
             run++;
             byte dcode1 = (byte) (convertunsignedByte2Short(ddata[run]) );
             for (int i = 0; i< drun; i++ ) {
                  bdata[nbin++]= dcode1;
                  total++;
             }
         }

        if(total < xt) {
             for ( run = total; run < xt; run++)
             {
                 bdata[run]= 0;
             }
        }

         return bdata;
    }

    /**
     * Read data from encoded values and run len into regular data array
     *
     * @param  ddata is encoded data values
     * @return the data array of row data
  */
    public byte[] readOneRowData(byte[] ddata, int rLen, int xt ) throws IOException, InvalidRangeException  {
         int run ;
         byte[] bdata = new byte[xt];

         int nbin = 0;
         int total = 0;
         for ( run = 0; run < rLen; run++)
         {
             int drun = convertunsignedByte2Short(ddata[run])>>4;
             byte dcode1 = (byte) (convertunsignedByte2Short(ddata[run])& 0Xf );
             for (int i = 0; i< drun; i++ ) {
                  bdata[nbin++]= dcode1;
                  total++;
             }
         }

        if(total < xt) {
             for ( run = total; run < xt; run++)
             {
                 bdata[run]= 0;
             }
        }

         return bdata;
    }

    public Object readRadialDataEle( ByteBuffer bos, Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {

      float[] elvdata = new float[ vinfo.yt];
      float elvAngle = vinfo.y0 * 0.1f ;
      //Float ra = new Float(elvAngle);

      for (int radial=0; radial<vinfo.yt; radial++) {
         elvdata[radial] = elvAngle;
      }   //end of for loop

      return elvdata;

    }

    public Object readRadialDataLatLonAlt( double t, Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {

      float[] vdata = new float[ vinfo.yt];

      for (int radial=0; radial<vinfo.yt; radial++) {
         vdata[radial] = (float) t;
      }   //end of for loop

      return vdata;

    }

    public Object readRadialDataAzi( ByteBuffer bos, Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {
      int doff = 0;
      float[] azidata = new float[ vinfo.yt];

      for (int radial=0; radial<vinfo.yt; radial++) {

        int runLen = bos.getShort();   //   getInt(vdata, doff, 2 );
        doff += 2;
        float radialAngle =(float) bos.getShort()/ 10.0f;
        doff += 2;
        int radialAngleD = bos.getShort();
        doff += 2;
        doff += runLen * 2;
        bos.position(doff);
        Float ra = new Float(radialAngle);
        azidata[radial] = ra.floatValue();

      }   //end of for loop

      return azidata;

    }

    public Object readDistance( Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {
      //int doff = 0;
      int[] data = new int[ vinfo.yt * vinfo.xt];

      for (int row=0; row<vinfo.yt; row++) {
          for ( int col = 0; col<vinfo.xt; col++) {
             int i = row * vinfo.yt + col;
             data[i] = col + vinfo.x0;
             //data[i] = val;
          }
      }   //end of for loop

      return data;

    }

    public Object readRadialDataGate( Nidsheader.Vinfo vinfo ) throws IOException, InvalidRangeException  {
      //int doff = 0;
      float[] gatedata = new float[ vinfo.xt];

      float sc = vinfo.y0 * 1.0f;
      for (int rad=0; rad<vinfo.xt; rad++) {
        gatedata[rad] = (vinfo.x0 + rad)* sc;

      }   //end of for loop

      return gatedata;

    }

   // for the compressed data read all out into a array and then parse into requested
   // This routine reads compressed image data for Level III formatted file.
   // We referenced McIDAS GetNexrLine function
   public byte[] readCompData1(byte[] uncomp, long hoff, long doff ) throws IOException {
      int off ;
      byte   b1, b2;
      b1 = uncomp[0];
      b2 = uncomp[1];
      off  = 2 * (((b1 & 63) << 8) + b2);
      /* eat WMO and PIL */
      for ( int i = 0; i < 2; i++ ) {
        while ( (off < uncomp.length ) && (uncomp[off] != '\n') ) off++;
        off++;
      }

      byte[] data = new byte[ (int)(uncomp.length - off - doff)];

      //byte[] hedata = new byte[(int)doff];

     // System.arraycopy(uncomp, off, hedata, 0, (int)doff);
      System.arraycopy(uncomp, off+ (int)doff, data, 0, uncomp.length - off -(int)doff);

      return data;
   }
   /**
     * Read compressed data
     *
     * @param  hoff header offset
     * @param  doff data offset
     * @return the array  of data
  */
   public byte[] readCompData(long hoff, long doff ) throws IOException {
       int  numin;                /* # input bytes processed       */
       long pos = 0;
       long len = myRaf.length();
       myRaf.seek(pos);
       numin = (int)(len - hoff);
       // Read in the contents of the NEXRAD Level III product header

        // nids header process
       byte[] b = new byte[(int)len];
       myRaf.readFully(b);

       /* a new copy of buff with only compressed bytes */

      // byte[] comp = new byte[numin - 4];
      // System.arraycopy( b, (int)hoff, comp, 0, numin -4 );

       // decompress the bytes
       Inflater inf = new Inflater( false);

       int resultLength;
       int result = 0;
       //byte[] inflateData = null;
       byte[] tmp ;
       int  uncompLen = 24500;        /* length of decompress space    */
       byte[] uncomp = new byte[uncompLen];

       inf.setInput(b, (int)hoff, numin-4);
       int limit = 20000;

       while ( inf.getRemaining() > 0 )
       {
          try {
            resultLength = inf.inflate(uncomp, result, 4000);
          }
          catch (DataFormatException ex) {
            System.out.println("ERROR on inflation "+ex.getMessage());
            ex.printStackTrace();
            throw new IOException( ex.getMessage());
          }

          result = result + resultLength;
          if( result > limit ) {
              // when uncomp data larger then limit, the uncomp need to increase size
              tmp = new byte[ result];
              System.arraycopy(uncomp, 0, tmp, 0, result);
              uncompLen = uncompLen + 10000;
              uncomp = new byte[uncompLen];
              System.arraycopy(tmp, 0, uncomp, 0, result);
          }
          if( resultLength == 0 ) {
               int tt = inf.getRemaining();
               byte [] b2 = new byte[2];
               System.arraycopy(b,(int)hoff+numin-4-tt, b2, 0, 2);
               if( headerParser.isZlibHed( b2 ) == 0 ) {
                  System.arraycopy(b, (int)hoff+numin-4-tt, uncomp, result, tt);
                  result = result + tt;
                  break;
               }
               inf.reset();
               inf.setInput(b, (int)hoff+numin-4-tt, tt);
          }

       }
       /*
       while ( inf.getRemaining() > 0) {
           try{
             resultLength = inf.inflate(uncomp);
           }
           catch (DataFormatException ex) {
            System.out.println("ERROR on inflation");
            ex.printStackTrace();
          }
           if(resultLength > 0 ) {
               result = result + resultLength;
               inflateData = new byte[result];
               if(tmp != null) {
                  System.arraycopy(tmp, 0, inflateData, 0, tmp.length);
                  System.arraycopy(uncomp, 0, inflateData, tmp.length, resultLength);
               } else {
                  System.arraycopy(uncomp, 0, inflateData, 0, resultLength);
               }
               tmp = new byte[result];
               System.arraycopy(inflateData, 0, tmp, 0, result);
               uncomp = new byte[(int)uncompLen];
           } else {
               int tt = inf.getRemaining();
               byte [] b2 = new byte[2];
               System.arraycopy(b,(int)hoff+numin-4-tt, b2, 0, 2);
               if( headerParser.isZlibHed( b2 ) == 0 ) {
                  result = result + tt;
                  inflateData = new byte[result];
                  System.arraycopy(tmp, 0, inflateData, 0, tmp.length);
                  System.arraycopy(b, (int)hoff+numin-4-tt, inflateData, tmp.length, tt);
                  break;
              }
               inf.reset();
               inf.setInput(b, (int)hoff+numin-4-tt, tt);

           }
        }  */
        inf.end();

      int off ;
      byte   b1, b2;
      b1 = uncomp[0];
      b2 = uncomp[1];
      off  = 2 * (((b1 & 63) << 8) + b2);
      /* eat WMO and PIL */
      for ( int i = 0; i < 2; i++ ) {
        while ( (off < result ) && (uncomp[off] != '\n') ) off++;
        off++;
      }

      byte[] data = new byte[ (int)(result - off - doff)];

      //byte[] hedata = new byte[(int)doff];

     // System.arraycopy(uncomp, off, hedata, 0, (int)doff);
      System.arraycopy(uncomp, off+ (int)doff, data, 0, result - off -(int)doff);

      return data;

   }

   /**
     * Read uncompressed data
     *
     * @param  hoff header offset
    *  @param  doff data offset
     * @return the array  of data
  */
   public byte[] readUCompData(long hoff, long doff ) throws IOException {
    int  numin;
    long pos = 0;
    long len = myRaf.length();
    myRaf.seek(pos);

    numin = (int)(len - hoff );
    // Read in the contents of the NEXRAD Level III product header

    // nids header process
    byte[] b = new byte[(int)len];
    myRaf.readFully(b);
    /* a new copy of buff with only compressed bytes */

    byte[] ucomp = new byte[numin - 4];
    System.arraycopy( b, (int)hoff, ucomp, 0, numin -4 );

    byte[] data = new byte[ (int)(ucomp.length - doff)];

    System.arraycopy(ucomp, (int)doff, data, 0, ucomp.length - (int)doff);

    return data;

   }

   // convert byte array to char array
  static protected char[] convertByteToChar( byte[] byteArray) {
    int size = byteArray.length;
    char[] cbuff = new char[size];
    for (int i=0; i<size; i++)
      cbuff[i] = (char) byteArray[i];
    return cbuff;
  }

   // convert char array to byte array
  static protected byte[] convertCharToByte( char[] from) {
    int size = from.length;
    byte[] to = new byte[size];
    for (int i=0; i<size; i++)
      to[i] = (byte) from[i];
    return to;
  }

  /*
  ** Name:       IsZlibed
  **
  ** Purpose:    Check a two-byte sequence to see if it indicates the start of
  **             a zlib-compressed buffer
  **
  ** Parameters:
  **             buf     - buffer containing at least two bytes
  **
  ** Returns:
  **             SUCCESS 1
  **             FAILURE 0
  **
  */
  int issZlibed( byte[] buf )
  {

      if ( (buf[0] & 0xf) == Z_DEFLATED ) {
        if ( (buf[0] >> 4) + 8 <= DEF_WBITS ) {
          if ( (((buf[0] << 8) + (buf[1])) % 31) == 0 ) {
            return 1;
          }
        }
      }

      return 0;
  }

  int getUInt( byte[] b, int offset, int num )
  {
      int            base=1;
      int            i;
      int            word=0;

      int bv[] = new int[num];

      for (i = 0; i<num; i++ )
      {
        bv[i] = convertunsignedByte2Short(b[offset+i]);
      }

      /*
      ** Calculate the integer value of the byte sequence
      */

      for ( i = num-1; i >= 0; i-- ) {
        word += base * bv[i];
        base *= 256;
      }

      return word;

  }
  int getInt( byte[] b, int offset, int num )
  {
      int            base=1;
      int            i;
      int            word=0;

      int bv[] = new int[num];

      for (i = 0; i<num; i++ )
      {
        bv[i] = convertunsignedByte2Short(b[offset+i]);
      }

      if( bv[0] > 127 )
      {
         bv[0] -= 128;
         base = -1;
      }
      /*
      ** Calculate the integer value of the byte sequence
      */

      for ( i = num-1; i >= 0; i-- ) {
        word += base * bv[i];
        base *= 256;
      }

      return word;

  }

  public short convertunsignedByte2Short(byte b)
  {
     return (short)((b<0)? (short)b + 256 : (short)b);
  }
  protected boolean fill;
  protected HashMap dimHash = new HashMap(50);

  public void flush() throws java.io.IOException {
    myRaf.flush();
  }

  public void close() throws java.io.IOException {
    myRaf.close();
  }

  public boolean syncExtend() { return false; }
  public boolean sync() { return false; }

  /** Debug info for this object. */
  public String toStringDebug(Object o) { return null; }
  public String getDetailInfo() { return ""; }

  public static void main(String args[]) throws Exception, IOException, InstantiationException, IllegalAccessException {
    String fileIn = "/home/yuanho/NIDS/N0R_20041102_2111";
    //String fileIn = "c:/data/image/Nids/n0r_20041013_1852";
    ucar.nc2.NetcdfFile.registerIOProvider( ucar.nc2.iosp.nids.Nidsiosp.class);
    ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open(fileIn);

    //List alist = ncf.getGlobalAttributes();

    ucar.nc2.Variable v = ncf.findVariable("BaseReflectivity");

    int[] origin  = {0, 0};
    int[] shape = {300, 36};

    ArrayByte data = (ArrayByte)v.read(origin,shape);

    ncf.close();


  }


}

/* Change History:
   $Log: Nidsiosp.java,v $
   Revision 1.39  2006/06/28 21:37:55  yuanho
   changing  raster data product setting to include calibrated output

   Revision 1.38  2006/06/05 22:38:02  yuanho
   changing  variable name (removing _ from all coordinate var)

   Revision 1.37  2006/05/30 21:51:43  yuanho
   adding calibrated radial data for precip which is radial image

   Revision 1.36  2006/05/30 16:10:53  yuanho
   adding calibrated radial data, the previous changing to _RAW

   Revision 1.35  2006/05/12 20:19:28  caron
   dapper sequences
   nexrad station db
   Aggregation stride bug

   Revision 1.34  2006/05/10 17:13:55  yuanho
   section to sectionNoReduce

   Revision 1.33  2006/05/09 17:56:17  yuanho
   stuctureMA replace StructureW, and doc

   Revision 1.32  2006/05/05 20:46:10  yuanho
   nested variable api

   Revision 1.31  2006/04/20 20:52:54  yuanho
   radial dataset sweep for all radar dataset

   Revision 1.30  2006/04/19 20:22:46  yuanho
   radial dataset sweep for all radar dataset

   Revision 1.29  2006/04/03 22:59:33  caron
   IOSP.readNestedData() remove flatten, handle flatten=false in NetcdfFile.readMemberData(); this allows IOSPs to be simpler
   add metar decoder from Robb's thredds.servlet.ldm package

   Revision 1.28  2006/02/16 23:02:37  caron
   *** empty log message ***

   Revision 1.27  2006/02/03 00:59:31  caron
   ArrayStructure refactor.
   DODS parsing refactor.

   Revision 1.26  2006/01/17 23:07:14  caron
   *** empty log message ***

   Revision 1.25  2006/01/04 00:02:34  caron
   dods src under our CVS
   forecastModelRun aggregation
   substitute M3IOVGGrid for M3IO coordSysBuilder
   iosp setProperties uses list.
   use jdom 1.0

   Revision 1.24  2005/12/16 20:42:17  yuanho
   fixing to read incomplete tabular data and VAD wind data

   Revision 1.23  2005/12/15 00:29:12  caron
   *** empty log message ***

   Revision 1.22  2005/12/09 04:24:38  caron
   Aggregation
   caching
   sync

   Revision 1.21  2005/11/09 21:09:38  yuanho
   adding new pkcode api, but with no availbale test file

   Revision 1.20  2005/11/08 23:11:00  yuanho
   adding new pkcode api, but with no availbale test file

   Revision 1.19  2005/11/07 23:22:06  yuanho
   change to read variable size string

   Revision 1.18  2005/11/07 23:03:09  caron
   need variable length text string

   Revision 1.17  2005/08/30 17:35:45  yuanho
   changed ArrayObject to ArrayShort

   Revision 1.16  2005/07/28 20:56:59  yuanho
   redo the structure variables read processes

   Revision 1.15  2005/07/26 22:36:23  caron
   bug in ByteBuffer.wrap()

   Revision 1.14  2005/07/26 21:22:16  caron
   redo nids structures, add new ArrayStructureBB constructor.

   Revision 1.13  2005/07/25 23:27:43  yuanho
   fix compressed bug, the end of some nids files have uncompressed data.

   Revision 1.12  2005/07/25 22:20:07  caron
   add iosp.synch()

   Revision 1.11  2005/07/08 21:42:11  yuanho
   nids global atts added

   Revision 1.10  2005/06/23 20:03:19  caron
   bug fix

   Revision 1.9  2005/06/23 19:18:44  caron
   no message

   Revision 1.8  2005/06/11 18:42:01  caron
   no message

   Revision 1.7  2005/05/23 21:52:56  caron
   add getDetailInfo() to IOSP for error/debug info

   Revision 1.6  2005/05/11 00:10:05  caron
   refactor StuctureData, dt.point

   Revision 1.5  2005/02/02 22:53:43  yuanho
   Graphic alphanumeric block fixed

   Revision 1.4  2005/01/20 21:24:50  yuanho
   adding reader to 18 nids products

   Revision 1.3  2004/12/08 21:38:23  yuanho
   read gate data

   Revision 1.2  2004/12/07 21:52:06  yuanho
   test nids code

   Revision 1.1  2004/12/07 21:51:41  yuanho
   test nids code

   Revision 1.3  2004/10/15 23:18:44  yuanho
   Nids projection update

   Revision 1.2  2004/10/14 17:14:31  caron
   add Nids reader
   add imageioreader for PNG

   Revision 1.1  2004/10/13 22:58:14  yuanho
   no message

   Revision 1.6  2004/08/17 19:20:04  caron
   2.2 alpha (2)

   Revision 1.5  2004/08/17 00:09:13  caron
   *** empty log message ***

   Revision 1.4  2004/08/16 20:53:45  caron
   2.2 alpha (2)

   Revision 1.3  2004/07/12 23:40:17  caron
   2.2 alpha 1.0 checkin

   Revision 1.2  2004/07/06 19:28:10  caron
   pre-alpha checkin

   Revision 1.1.1.1  2003/12/04 21:05:27  caron
   checkin 2.2

 */