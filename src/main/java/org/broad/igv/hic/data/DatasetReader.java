package org.broad.igv.hic.data;


import org.broad.igv.hic.tools.Preprocessor;
import org.broad.igv.util.CompressionUtils;
import org.broad.igv.util.FileUtils;
import org.broad.igv.util.HttpUtils;
import org.broad.tribble.util.LittleEndianInputStream;
import org.broad.tribble.util.SeekableStream;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**    1334372501047
 * @author jrobinso
 * @date Aug 17, 2010
 */
public class DatasetReader {

    private SeekableStream stream;

    private Map<String, Preprocessor.IndexEntry> masterIndex;
    private long totalCount;
    private DensityFunction densityFunction;
    private Dataset dataset;
    private int version;

    public DatasetReader(SeekableStream stream) {
        this.stream = stream;
        masterIndex = new HashMap<String, Preprocessor.IndexEntry>();
        dataset     = new Dataset(this);
      }

    public Dataset read() throws FileNotFoundException {


        try {
            // Read the header
            LittleEndianInputStream dis = new LittleEndianInputStream(new BufferedInputStream(stream));
            long masterIndexPos = dis.readLong();

            // Read chromosome dictionary
            int nchrs = dis.readInt();
            Chromosome[] chromosomes = new Chromosome[nchrs];
            for (int i = 0; i < nchrs; i++) {
                String name = dis.readString();
                int size = dis.readInt();
                chromosomes[i] = new Chromosome(i, name, size);
            }
            dataset.setChromosomes(chromosomes);

            // Read attribute dictionary.  Can contain arbitrary # of attributes as key-value pairs, including version
            version = 0;  // <= assumption
            int nAttributes = dis.readInt();
            for (int i = 0; i < nAttributes; i++) {
                String key = dis.readString();
                String value = dis.readString();
                if(key.equals("Version")) {
                    version = Integer.parseInt(value);
                }
            }

            readMasterIndex(masterIndexPos);


        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        return dataset;

    }

    public int getVersion() {
        return version;
    }

    private Map<String, Preprocessor.IndexEntry> readMasterIndex(long position) throws IOException {

        stream.seek(position);
        byte[] buffer = new byte[4];
        stream.readFully(buffer);
        LittleEndianInputStream dis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));
        int nBytes = dis.readInt();


        buffer = new byte[nBytes];
        stream.readFully(buffer);

        dis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));
        int nEntries = dis.readInt();
        for (int i = 0; i < nEntries; i++) {
            String key = dis.readString();
            long filePosition = dis.readLong();
            int sizeInBytes = dis.readInt();
            masterIndex.put(key, new Preprocessor.IndexEntry(filePosition, sizeInBytes));
        }

//        try {
//         //   readExpectedValues(dis);
//        } catch (IOException e) {
//            System.err.println("Warning: No expected value information available.");
//        }

        return masterIndex;

    }

    private void readExpectedValues(LittleEndianInputStream dis) throws IOException {

        totalCount = dis.readLong();

        int gridSize = dis.readInt();
        int nGrids = dis.readInt();
        double[] densities = new double[nGrids];
        for (int i = 0; i < nGrids; i++) {
            densities[i] = dis.readDouble();
        }

        int nNormFactors = dis.readInt();
        Map<Integer, Double> normFactors = new HashMap<Integer, Double>(nNormFactors);
        for (int i = 0; i < nNormFactors; i++) {
            Integer key = dis.readInt();
            Double norm = dis.readDouble();
            normFactors.put(key, norm);
        }
        densityFunction = new DensityFunction(gridSize, densities, normFactors);
    }


    public Matrix readMatrix(String key) throws IOException {
        Preprocessor.IndexEntry idx = masterIndex.get(key);
        if (idx == null) {
            return null;
        }

        byte[] buffer = new byte[idx.size];
        stream.seek(idx.position);
        stream.readFully(buffer);
        LittleEndianInputStream dis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));

        int c1 = dis.readInt();
        int c2 = dis.readInt();
        int nZooms = dis.readInt();

        Chromosome chr1 = dataset.getChromosomes()[c1];
        Chromosome chr2 = dataset.getChromosomes()[c2];

        MatrixZoomData[] zd = new MatrixZoomData[nZooms];
        for (int i = 0; i < nZooms; i++) {
            zd[i] = new MatrixZoomData(chr1, chr2, this, dis);
        }

        Matrix m = new Matrix(c1, c2, zd);
        return m;
    }

    public Block readBlock(int blockNumber, Preprocessor.IndexEntry idx) throws IOException {

        byte[] compressedBytes = new byte[idx.size];
        stream.seek(idx.position);
        stream.readFully(compressedBytes);

        byte[] buffer = CompressionUtils.decompress(compressedBytes);
        LittleEndianInputStream dis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));

        int nRecords = dis.readInt();
        ContactRecord[] records = new ContactRecord[nRecords];
        for (int i = 0; i < nRecords; i++) {
            try {
                int bin1 = dis.readInt();
                int bin2 = dis.readInt();
                int counts = dis.readInt();
                records[i] = new ContactRecord(blockNumber, bin1, bin2, counts);
            } catch (EOFException e) {
                nRecords = i;
                ContactRecord[] modifiedRecords = new ContactRecord[nRecords];
                System.arraycopy(records, 0, modifiedRecords, 0, nRecords);
                records = modifiedRecords;
                break;
            }

        }

        return new Block(blockNumber, records);

    }

}
