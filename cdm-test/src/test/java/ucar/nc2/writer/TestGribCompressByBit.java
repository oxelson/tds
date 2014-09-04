/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.writer;

import SevenZip.LzmaAlone;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;
import ucar.ma2.DataType;
import ucar.nc2.grib.GribData;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.Grib2SectionData;
import ucar.nc2.grib.grib2.Grib2SectionDataRepresentation;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.test.util.TestDir;

import java.io.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Try different compress algorithms on GRIB files
 *
 * @author caron
 * @since 8/12/2014
 */
public class TestGribCompressByBit {

  static enum Algo {deflate, bzip2, bzip2T, bzipScaled, xy, zip7}

  static File outDir;
  static int deflate_level = 3;
  static PrintStream detailOut;
  static PrintStream summaryOut;
  static final int showEvery = 1000;

  List<CompressAlgo> runAlg = new ArrayList<>();

  TestGribCompressByBit(Algo... wantAlgs) {
    for (Algo want : wantAlgs) {
      switch (want) {
        case deflate: runAlg.add( new JavaDeflate()); break;
        case bzip2: runAlg.add( new ApacheBzip2()); break;
        case bzip2T: runAlg.add( new ItadakiBzip2()); break;
        case bzipScaled: runAlg.add( new ScaleAndOffset()); break;
        case xy: runAlg.add( new TukaaniLZMA2()); break;
        case zip7: runAlg.add(new Zip7()); break;
      }
    }
    makeSummaryHeader();
  }

  int nfiles = 0;
  int tot_nrecords = 0;
  double file_gribsize;

  void reset() {
    for (CompressAlgo alg : runAlg) alg.reset();
  }

  private int doGrib2(String filename, boolean doInts) {
    int nrecords = 0;
    int npoints = 0;
    long start = System.nanoTime();
    file_gribsize = 0.0;

    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      Grib2RecordScanner reader = new Grib2RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib2.Grib2Record gr = reader.next();

        Grib2SectionData dataSection = gr.getDataSection();
        int gribMsgLength = dataSection.getMsgLength();

        Grib2SectionDataRepresentation drss = gr.getDataRepresentationSection();
        int template = drss.getDataTemplate();
        if (template != 40) continue; // skip all but JPEG-2000

        GribData.Info info = gr.getBinaryDataInfo(raf);
        int nBits = info.numberOfBits;
        if (nBits == 0) continue; // skip constant fields

        long start2 = System.nanoTime();
        float[] fdata = gr.readData(raf);
        long end2 = System.nanoTime();
        long gribReadTime = end2 - start2; // LOOK this include IO, can we just test uncompress ??
        if (npoints == 0) npoints = fdata.length;

        int[] rawData = gr.readRawData(raf);
        byte[] bdata = doInts ? GribData.convertToBytes(rawData) : GribData.convertToBytes(fdata);

        detailReport(nBits, fdata.length, gribMsgLength, gribReadTime, new Bean(info, fdata), bdata, rawData);

        nrecords++;
        tot_nrecords++;
        file_gribsize += gribMsgLength;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    summaryReport(filename, start, npoints, nrecords);

    nfiles++;
    return nrecords;
  }

  private void makeDetailHeader(boolean showDetail, String filename) throws FileNotFoundException {
    if (showDetail) {
      File f = new File(filename);
      detailOut = new PrintStream(new File(outDir, f.getName() + ".csv"));
    } else
      detailOut = null;

    Formatter f = new Formatter();

    // first header has the algo names
    f.format(",,,,,,");
    for (CompressAlgo alg : runAlg) {
      f.format("%s,,,,", alg.getAlgo());
    }
    f.format("%n");

    // second header
    f.format("%s, %s, %s, %s, %s, %s, ", "nbits", "npoints", "gribMsgLength", "gribRead", "entropyI", "entropyB");
    for (CompressAlgo alg : runAlg) {
      f.format("%s, %s, %s, %s, ", "compressSize", "ratio", "compressMsecs", "uncompressMsecs");
    }
    f.format("%n");

    // results
    if (detailOut != null) detailOut.printf("%s", f.toString());
    System.out.printf("%s", f.toString());
  }

  private void detailReport(int nbits, int npoints, int gribMsgLength, long gribReadTime, GribData.Bean bean, byte[] bdata, int[] rawData) throws IOException {
    Formatter f = new Formatter();

    double entropyI = GribData.entropy(nbits, rawData);
    double entropyB = GribData.entropy(bdata);
    f.format("%d, %d, %d, %d, %f, %f, ", nbits, npoints, gribMsgLength, gribReadTime/1000/1000, entropyI, entropyB);

    for (CompressAlgo alg : runAlg) {
      if (alg instanceof ScaleAndOffset)
        alg.testScaleAndOffset(bean, bdata);
      else
        alg.test(bdata);
      double ratio = ((double) alg.compressSize) / gribMsgLength;
      f.format("%d, %f, %d, %d, ", alg.compressSize, ratio, alg.nsecsCompress / 1000 / 1000, alg.nsecsUncompress / 1000 / 1000);
    }
    f.format("%n");

    // results
    if (detailOut != null) detailOut.printf("%s", f.toString());
    if (tot_nrecords % showEvery == 0) System.out.printf("%s", f.toString());
  }

  private void makeSummaryHeader() {
    Formatter f = new Formatter();

    // first header has the algo names
    f.format(",,,,,");
    for (CompressAlgo alg : runAlg) {
      f.format("%s,,,,", alg.getAlgo());
    }
    f.format("%n");

    // second header
    f.format("%s, %s, %s, %s, %s, ", "file", "npoints", "nrecords", "gribFileSize", "took secs");
    for (CompressAlgo alg : runAlg) {
      f.format("%s, %s, %s, %s, ", "compressSize", "ratio", "avg compress msecs", "avg uncompress msecs");
    }
    f.format("%n");;

    // results
    if (summaryOut != null) summaryOut.printf("%s", f.toString());
    reset();
  }

  private void summaryReport(String filename, long start, int npoints, int nrecords) {
    double took = ((double)System.nanoTime() - start) * 1.0e-9;

    Formatter f = new Formatter();
    f.format("%s, %d, %d, %f, %f, ", filename, npoints, nrecords, file_gribsize, took);

    for (CompressAlgo alg : runAlg) {
      double ratio = ((double) alg.tot_size) / file_gribsize;
      double avg_compress_msecs = ((double)alg.tot_compressTime) * 1.0e-6 / nrecords;
      double avg_uncompress_msecs = ((double)alg.tot_uncompressTime) * 1.0e-6 / nrecords;
      f.format("%d, %f, %f, %f, ", alg.tot_size, ratio, avg_compress_msecs, avg_uncompress_msecs);
    }
    f.format("%n");

    System.out.printf("%s%n", f.toString());
    summaryOut.printf("%s", f.toString());
    summaryOut.flush();
  }

  class Bean implements GribData.Bean {
    GribData.Info info;
    float[] data;
    double minimum, maximum, scale;

    Bean(GribData.Info info, float[] data) {
      this.info = info;
      this.data = data;

      double pow10 =  Math.pow(10.0, -getDecimalScale());        // 1/10^D
      minimum = (float) (pow10 * info.referenceValue);          // R / 10^D
      scale = (float) (pow10 * Math.pow(2.0, getBinScale()));  // 2^E / 10^D

      double maxPacked = Math.pow(2.0, getNBits()) - 1;
      maximum = minimum +  scale * maxPacked;
    }

    @Override
    public float[] readData() throws IOException {
      return data;
    }

    @Override
    public int getNBits() {
      return info.numberOfBits;
    }

    @Override
    public int getDataLength() {
      return info.dataLength;
    }

    @Override
    public int getBinScale() {
      return info.binaryScaleFactor;
    }

    @Override
    public int getDecimalScale() {
      return info.decimalScaleFactor;
    }

    @Override
    public double getMinimum() {
      return minimum;
    }

    @Override
    public double getMaximum() {
      return maximum;
    }

    @Override
    public double getScale() {
      return scale;
    }
  }

  //////////////////////////////////////////////////////////////////////
  abstract class CompressAlgo {
    long nsecsCompress, nsecsUncompress;
    int orgSize, compressSize, uncompressSize;

    int tot_size;
    long tot_compressTime, tot_uncompressTime;

    void test(byte[] orgBytes) throws IOException {
      orgSize = orgBytes.length;

      long start = System.nanoTime();
      byte[] compressedBytes = compress(orgBytes);
      long end = System.nanoTime();
      nsecsCompress = end - start;
      compressSize = compressedBytes.length;

      start = end;
      byte[] uncompressedBytes = uncompress(compressedBytes);
      end = System.nanoTime();
      nsecsUncompress = end - start;
      uncompressSize = uncompressedBytes.length;
      if (uncompressSize != orgSize) {
        System.out.printf("HEY %s %d != %d%n", getAlgo(), uncompressSize, orgSize);
      }

      tot_size += compressSize;
      tot_compressTime += nsecsCompress;
      tot_uncompressTime += nsecsUncompress;
    }

    void testScaleAndOffset(GribData.Bean bean, byte[] orgBytes) throws IOException {
      orgSize = orgBytes.length;

      ScaleAndOffset scaler = (ScaleAndOffset) this;

      long start = System.nanoTime();
      byte[] compressedBytes = scaler.compress(bean);
      long end = System.nanoTime();
      nsecsCompress = end - start;
      compressSize = compressedBytes.length;

      //start = end;
      //byte[] uncompressedBytes = uncompress(compressedBytes);
      //end = System.nanoTime();
      nsecsUncompress = 0;
      uncompressSize = 0;

      tot_size += compressSize;
      tot_compressTime += nsecsCompress;
      tot_uncompressTime += nsecsUncompress;
    }


    void reset() {
      this.tot_size = 0;
      this.tot_compressTime = 0;
      this.tot_uncompressTime = 0;
    }

    abstract byte[] compress(byte[] data) throws IOException;

    abstract byte[] uncompress(byte[] data) throws IOException;

    abstract Algo getAlgo();
  }

  class JavaDeflate extends CompressAlgo {
    Algo getAlgo() {
      return Algo.deflate;
    }

    byte[] compress(byte[] data) {
      Deflater compresser = new Deflater(deflate_level);
      compresser.setInput(data);
      compresser.finish();

      byte[] output = new byte[data.length * 2];
      int size = compresser.deflate(output);
      compresser.end();

      // copy just the resulting bytes
      byte[] out = new byte[size];
      System.arraycopy(output, 0, out, 0, size);
      return out;
    }

    byte[] uncompress(byte[] data) {
      byte[] result = new byte[data.length * 20];
      int resultLength;
      try {
        Inflater decompresser = new Inflater();
        decompresser.setInput(data, 0, data.length);
        resultLength = decompresser.inflate(result);
        decompresser.end();

      } catch (Exception e) {
        e.printStackTrace();
        return new byte[0];
      }

      // copy just the resulting bytes
      byte[] out = new byte[resultLength];
      System.arraycopy(result, 0, out, 0, resultLength);
      return out;
    }

  }

    /* private int deflateShave(float[] data, int bitN) {
    int bitMask = Grib2NetcdfWriter.getBitMask(bitN+1);
    Deflater compresser = new Deflater(deflate_level);
    ByteBuffer bb = ByteBuffer.allocate(data.length * 4);
    for (float d : data)
      bb.putFloat(Grib2NetcdfWriter.bitShave(d, bitMask));

    compresser.setInput(bb.array());
    compresser.finish();

    byte[] output = new byte[data.length * 4];
    int compressedDataLength = compresser.deflate(output);
    compresser.end();
    return compressedDataLength;
  } */

  class ApacheBzip2 extends CompressAlgo {
    Algo getAlgo() {
      return Algo.bzip2;
    }

    byte[] compress(byte[] bdata) {
      ByteArrayOutputStream out = new ByteArrayOutputStream(bdata.length);
      try (BZip2CompressorOutputStream bzOut = new BZip2CompressorOutputStream(out)) {
        bzOut.write(bdata, 0, bdata.length);
        bzOut.finish();

      } catch (Exception e) {
        e.printStackTrace();
      }

      return out.toByteArray();
    }

    byte[] uncompress(byte[] bdata) {
       ByteArrayOutputStream out = new ByteArrayOutputStream(20 * bdata.length);
       ByteArrayInputStream in = new ByteArrayInputStream(bdata);
       try (BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in, false)) {
         int bytesRead;
         byte[] decoded = new byte [524288];
         while ((bytesRead = bzIn.read (decoded)) != -1) {
           out.write (decoded, 0, bytesRead) ;
         }
         out.close();

       } catch (Exception e) {
         e.printStackTrace();
       }

       return out.toByteArray();
     }
  }

  class ItadakiBzip2 extends CompressAlgo {
    Algo getAlgo() {
      return Algo.bzip2T;
    }

    byte[] compress(byte[] bdata) throws IOException {
      int orgSize = bdata.length;
      ByteArrayOutputStream out = new ByteArrayOutputStream(2 * orgSize);
      try (org.itadaki.bzip2.BZip2OutputStream zipper = new org.itadaki.bzip2.BZip2OutputStream(out)) {
        InputStream fin = new ByteArrayInputStream(bdata);
        IO.copy(fin, zipper);

      } catch (Exception e) {
        e.printStackTrace();
      }

      return out.toByteArray();
    }

    byte[] uncompress(byte[] bdata) {
       ByteArrayOutputStream out = new ByteArrayOutputStream(20 * bdata.length);
       ByteArrayInputStream in = new ByteArrayInputStream(bdata);
       try (org.itadaki.bzip2.BZip2InputStream bzIn = new org.itadaki.bzip2.BZip2InputStream(in, false)) {
         int bytesRead;
         byte[] decoded = new byte [524288];
         while ((bytesRead = bzIn.read (decoded)) != -1) {
           out.write (decoded, 0, bytesRead) ;
         }
         out.close();

       } catch (Exception e) {
         e.printStackTrace();
       }

       return out.toByteArray();
     }

  }

  class ScaleAndOffset extends CompressAlgo {
    @Override
    byte[] compress(byte[] data) throws IOException {
      return new byte[0];
    }
    @Override
    byte[] uncompress(byte[] data) throws IOException {
      return new byte[0];
    }
    Algo getAlgo() { return Algo.bzipScaled; }

    byte[] compress(GribData.Bean bean) throws IOException {
      return GribData.compressScaled(bean); // LOOK could seperate from compression and try different ones
    }
  }

  class TukaaniLZMA2 extends CompressAlgo {
    Algo getAlgo() {
      return Algo.xy;
    }

    byte[] compress(byte[] bdata) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream(bdata.length * 2);
      try (XZOutputStream compressor = new XZOutputStream(out, new LZMA2Options())) {
        compressor.write(bdata, 0, bdata.length);
        compressor.finish();
      } catch (IOException e) {
        e.printStackTrace();
      }

      return out.toByteArray();
    }

    byte[] uncompress(byte[] bdata) {
       ByteArrayOutputStream out = new ByteArrayOutputStream(20 * bdata.length);
       ByteArrayInputStream in = new ByteArrayInputStream(bdata);
       try (XZInputStream bzIn = new XZInputStream(in)) {
         int bytesRead;
         byte[] decoded = new byte [524288];
         while ((bytesRead = bzIn.read (decoded)) != -1) {
           out.write (decoded, 0, bytesRead) ;
         }
         out.close();

       } catch (Exception e) {
         e.printStackTrace();
       }

       return out.toByteArray();
     }

  }

  class Zip7 extends CompressAlgo {
    Algo getAlgo() {
      return Algo.zip7;
    }

    byte[] compress(byte[] bdata) throws IOException {
      ByteArrayOutputStream outStream = new ByteArrayOutputStream(bdata.length * 2);
      InputStream inStream = new ByteArrayInputStream(bdata);

      // the compression params
      LzmaAlone.CommandLine params = new LzmaAlone.CommandLine();
      SevenZip.Compression.LZMA.Encoder encoder = new SevenZip.Compression.LZMA.Encoder();
      if (!encoder.SetAlgorithm(params.Algorithm))
 					throw new IOException("Incorrect compression mode");
 				if (!encoder.SetDictionarySize(params.DictionarySize))
 					throw new IOException("Incorrect dictionary size");
 				if (!encoder.SetNumFastBytes(params.Fb))
 					throw new IOException("Incorrect -fb value");
 				if (!encoder.SetMatchFinder(params.MatchFinder))
 					throw new IOException("Incorrect -mf value");
 				if (!encoder.SetLcLpPb(params.Lc, params.Lp, params.Pb))
 					throw new IOException("Incorrect -lc or -lp or -pb value");

 			encoder.SetEndMarkerMode(false);
 			encoder.WriteCoderProperties(outStream);

       // next 8 bytes are the length
      long msgSize = (long) bdata.length;
      for (int i = 0; i < 8; i++)
        outStream.write((int) (msgSize >>> (8 * i)) & 0xFF);

      // encode the data
      encoder.Code(inStream, outStream, -1, -1, null);

      return outStream.toByteArray();
    }

    byte[] uncompress(byte[] bdata) throws IOException {
      ByteArrayOutputStream outStream = new ByteArrayOutputStream(bdata.length * 20);
      InputStream inStream = new ByteArrayInputStream(bdata);

      // the compression params
      int propertiesSize = 5;
  		byte[] properties = new byte[propertiesSize];
  		if (inStream.read(properties, 0, propertiesSize) != propertiesSize)
  			throw new IOException("input .lzma file is too short");
  		SevenZip.Compression.LZMA.Decoder decoder = new SevenZip.Compression.LZMA.Decoder();
  		if (!decoder.SetDecoderProperties(properties))
  			throw new IOException("Incorrect stream properties");

      // next 8 bytes are the length
      long outSize = 0;
      for (int i = 0; i < 8; i++) {
        int v = inStream.read();
        if (v < 0) throw new IOException("Can't read stream size");
        outSize |= ((long) v) << (8 * i);
      }

       // decode the data
      if (!decoder.Code(inStream, outStream, outSize))
        throw new IOException("Error in data stream");

      return outStream.toByteArray();
    }

  }

  ///////////////////////////////////////////////////////


  /* private int doGrib1(String filename)  {
    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib1.Grib1Record gr = reader.next();

        Grib1SectionBinaryData dataSection = gr.getDataSection();
        Grib1SectionBinaryData.BinaryDataInfo info = dataSection.getBinaryDataInfo(raf);

        float[] data = gr.readData(raf);
        int compressDeflate = deflate( convertToBytes(data)); // run it through the deflator
        if (npoints == 0) npoints = data.length;

         // deflate the original (bit-packed) data
        int compressOrg = deflate(dataSection.getBytes(raf)); // run it through the deflator

        // results
        if (detailOut != null) detailOut.printf("%d, %d, %d, %d, %d%n", info.numbits, data.length, info.msgLength, compressDeflate, compressOrg);
        //System.out.printf("%d, %d, %d, %d, %f, %d%n", info.numbits, data.length, info.msgLength, compressBytes, r, compressOrg);
        nrecords++;
        totalBytesIn += info.msgLength;
        totDeflate += compressDeflate;
        totOrg += compressOrg;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return nrecords;
  }  */


  private static class CompressReportAction implements TestDir.Act {
    TestGribCompressByBit compressByBit;
    boolean showDetail;
    boolean doInts;

    private CompressReportAction(TestGribCompressByBit compressByBit, boolean showDetail, boolean doInts) {
      this.compressByBit = compressByBit;
      this.showDetail = showDetail;
      this.doInts = doInts;
    }

    public int doAct(String filename) throws IOException {
      compressByBit.makeDetailHeader(showDetail, filename);
      compressByBit.doGrib2(filename, doInts);

      if (detailOut != null)
        detailOut.close();

      return 1;
    }
  }


  public static void main2(String[] args) throws IOException {
    outDir = new File("G:/grib2nc/compressInts/");
    outDir.mkdirs();
    summaryOut = new PrintStream(new File(outDir, "summary.csv"));

    try {
      TestGribCompressByBit compressByBit = new TestGribCompressByBit( Algo.deflate, Algo.bzip2, Algo.bzip2T, Algo.xy, Algo.zip7);
      CompressReportAction test = new CompressReportAction(compressByBit, true, true);
      test.doAct("Q:\\cdmUnitTest\\tds\\ncep\\NAM_CONUS_20km_surface_20100913_0000.grib2");

    } finally {
      summaryOut.close();
    }
  }

  public static void main(String[] args) throws IOException {
    outDir = new File("G:/grib2nc/compressAllFiles/");
    outDir.mkdirs();
    summaryOut = new PrintStream(new File(outDir, "summary.csv"));

    try {
      TestGribCompressByBit compressByBit = new TestGribCompressByBit( Algo.bzip2T);
      CompressReportAction test = new CompressReportAction(compressByBit, true, true);
      String dirName = "Q:/cdmUnitTest/tds/ncep/";
      TestDir.actOnAll(dirName, new TestDir.FileFilterFromSuffixes("grib2"), test);

    } finally {
      summaryOut.close();
    }
  }

  /* public static void main2(String[] args) throws IOException {
    outDir = new File("G:/grib2nc/bzip2/");
    summaryOut = new PrintStream(new File(outDir, "summary.csv"));
    writeSummaryHeader();

    try {
      String dirName = "Q:/cdmUnitTest/tds/ncep/";
      TestDir.actOnAll(dirName, new TestDir.FileFilterFromSuffixes("grib2"), new CompressReportAction(true, false), false);

    } finally {
      summaryOut.close();
    }
  } */

}

/*

GFS_Alaska_191km_20100913_0000.grib1
GFS_CONUS_191km_20100519_1800.grib1
GFS_CONUS_80km_20100513_0600.grib1
GFS_CONUS_95km_20100506_0600.grib1
GFS_Hawaii_160km_20100428_0000.grib1
GFS_N_Hemisphere_381km_20100516_0600.grib1
GFS_Puerto_Rico_191km_20100515_0000.grib1
NAM_Alaska_22km_20100504_0000.grib1
NAM_Alaska_45km_noaaport_20100525_1200.grib1
NAM_Alaska_95km_20100502_0000.grib1
NAM_CONUS_20km_noaaport_20100602_0000.grib1
NAM_CONUS_80km_20100508_1200.grib1
RUC2_CONUS_40km_20100515_0200.grib1
RUC2_CONUS_40km_20100914_1200.grib1
RUC_CONUS_80km_20100430_0000.grib1

-----------------
D = has duplicates

DRS template
     0: count = 30
     2: count = 283
     3: count = 15017
    40: count = 384980


    DGEX_Alaska_12km_20100524_0000.grib2
    DGEX_CONUS_12km_20100514_1800.grib2
    GEFS_Global_1p0deg_Ensemble_20120215_0000.grib2
    GEFS_Global_1p0deg_Ensemble_derived_20120214_0000.grib2
    GFS_Global_0p5deg_20100913_0000.grib2
    GFS_Global_0p5deg_20140804_0000.grib2
3   GFS_Global_2p5deg_20100602_1200.grib2
    GFS_Global_onedeg_20100913_0000.grib2
    GFS_Puerto_Rico_0p5deg_20140106_1800.grib2
3   HRRR_CONUS_3km_wrfprs_201408120000.grib2
    NAM_Alaska_11km_20100519_0000.grib2
    NAM_Alaska_45km_conduit_20100913_0000.grib2
    NAM_CONUS_12km_20100915_1200.grib2
    NAM_CONUS_12km_20140804_0000.grib2
    NAM_CONUS_12km_conduit_20140804_0000.grib2
    NAM_CONUS_20km_selectsurface_20100913_0000.grib2
    NAM_CONUS_20km_surface_20100913_0000.grib2
    NAM_CONUS_40km_conduit_20100913_0000.grib2
    NAM_Firewxnest_20140804_0000.grib2
    NAM_Polar_90km_20100913_0000.grib2
2   NDFD_CONUS_5km_20140805_1200.grib2
2/3 NDFD_CONUS_5km_conduit_20140804_0000.grib2
2   NDFD_Fireweather_CONUS_20140804_1800.grib2
    RR_CONUS_13km_20121028_0000.grib2
    RR_CONUS_20km_20140804_1900.grib2
    RR_CONUS_40km_20140805_1600.grib2
0/2 RTMA_CONUS_2p5km_20111221_0800.grib2
0   RTMA_GUAM_2p5km_20140803_0600.grib2
    RUC2_CONUS_20km_hybrid_20100913_0000.grib2
    RUC2_CONUS_20km_pressure_20100509_1300.grib2
    RUC2_CONUS_20km_surface_20100516_1600.grib2
    SREF_Alaska_45km_ensprod_20120213_1500.grib2
    SREF_CONUS_40km_ensprod_20120214_1500.grib2
    SREF_CONUS_40km_ensprod_20140804_1500.grib2
    SREF_CONUS_40km_ensprod_biasc_20120213_2100.grib2
D   SREF_CONUS_40km_ensprod_biasc_20140805_1500.grib2
    SREF_CONUS_40km_pgrb_biasc_nmm_n2_20100602_1500.grib2
    SREF_CONUS_40km_pgrb_biasc_rsm_p2_20120213_1500.grib2
    SREF_PacificNE_0p4_ensprod_20120213_2100.grib2
D   WW3_Coastal_Alaska_20140804_0000.grib2
D   WW3_Coastal_US_East_Coast_20140804_0000.grib2
D   WW3_Coastal_US_West_Coast_20140804_1800.grib2
D   WW3_Global_20140804_0000.grib2
D   WW3_Regional_Alaska_20140804_0000.grib2
D   WW3_Regional_Eastern_Pacific_20140804_0000.grib2
D   WW3_Regional_US_East_Coast_20140804_0000.grib2
D   WW3_Regional_US_West_Coast_20140803_0600.grib2


 */