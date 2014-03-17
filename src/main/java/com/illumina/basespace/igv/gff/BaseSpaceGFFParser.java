package com.illumina.basespace.igv.gff;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.broad.igv.Globals;
import org.broad.igv.exceptions.ParserException;
import org.broad.igv.feature.BasicFeature;
import org.broad.igv.feature.FeatureDB;
import org.broad.igv.feature.FeatureParser;
import org.broad.igv.feature.GFFParser;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.tribble.GFFCodec;
import org.broad.igv.renderer.GeneTrackRenderer;
import org.broad.igv.renderer.IGVFeatureRenderer;
import org.broad.igv.track.FeatureCollectionSource;
import org.broad.igv.track.FeatureTrack;
import org.broad.igv.track.GFFFeatureSource;
import org.broad.igv.track.TrackLoader;
import org.broad.igv.track.TrackProperties;
import org.broad.igv.ui.IGV;
import org.broad.igv.util.ResourceLocator;
import org.broad.tribble.Feature;

import com.illumina.basespace.ApiClient;
import com.illumina.basespace.igv.BaseSpaceMain;
import com.illumina.basespace.igv.BaseSpaceUtil;
import com.illumina.basespace.igv.gff.GFFLocatorFactory.GFFTrackLoader;
import com.illumina.basespace.igv.ui.ProgressReport;
import com.illumina.basespace.igv.ui.tree.BrowserDialog;

public class BaseSpaceGFFParser implements FeatureParser
{
    static Logger log = Logger.getLogger(GFFParser.class.getName());

    private TrackProperties trackProperties = null;
    
    public BaseSpaceGFFParser(String path)
    {
    }

    public List<FeatureTrack> loadTracks(ResourceLocator locator, Genome genome)
    {

        BufferedReader reader = null;
        try
        {
            GFFTrackLoader gffLocator = (GFFTrackLoader) locator;
            ApiClient client = BaseSpaceMain.instance().getApiClient(gffLocator.getClientId());

           // this.lineCount = count(gffLocator.getFile().getSize(),client.getFileInputStream(gffLocator.getFile()));
            reader = new BufferedReader(new InputStreamReader(client.getFileInputStream(gffLocator.getFile())));

            GFFCodec.Version version = locator.getPath().endsWith(".gff3") ? GFFCodec.Version.GFF3
                    : GFFCodec.Version.GFF2;
            List<org.broad.tribble.Feature> features = loadFeatures(reader, genome);

            FeatureTrack track = new FeatureTrack(locator, new FeatureCollectionSource(features, genome));
            track.setName(locator.getTrackName());
            track.setRendererClass(IGVFeatureRenderer.class);
            track.setMinimumHeight(35);
            track.setHeight(45);
            track.setRendererClass(GeneTrackRenderer.class);

            if (trackProperties != null)
            {
                track.setProperties(trackProperties);
                track.setName(trackProperties.getName());
            }

            List<FeatureTrack> tracks = new ArrayList<FeatureTrack>();
            tracks.add(track);
            return tracks;

        }
        catch (Throwable ex)
        {
            log.severe(ex.toString());
            throw new RuntimeException(ex);

        }
        finally
        {
            BaseSpaceUtil.dispose(reader);
        }
    }

    public List<org.broad.tribble.Feature> loadFeatures(final BufferedReader reader, final Genome genome)
    {
        BrowserDialog.instance().workInit(1000);
        SwingWorker<List<Feature>, ProgressReport> worker = new SwingWorker<List<Feature>, ProgressReport>()
        {
            @Override
            protected List<Feature> doInBackground() throws Exception
            {
                String line = null;
                int lineNumber = 0;
                int progressLineCount = 0;
                GFFCodec codec = new GFFCodec(genome);
                GFFFeatureSource.GFFCombiner combiner = new GFFFeatureSource.GFFCombiner();
                try
                {
                    while ((line = reader.readLine()) != null)
                    {
                        lineNumber++;
                        progressLineCount++;

                        if (line.startsWith("#"))
                        {
                            codec.readHeaderLine(line);
                        }
                        else
                        {
                            try
                            {
                                Feature f = codec.decode(line);
                                if (f != null)
                                {
                                    combiner.addFeature((BasicFeature) f);
                                }
                                List<ProgressReport> progress = new ArrayList<ProgressReport>();
                                progress.add(new ProgressReport("Decoding GFF Feature at line " + lineNumber,progressLineCount));
                                publish(progress.toArray(new ProgressReport[progress.size()]));

                                if (progressLineCount % 1000 == 0)
                                {
                                    progressLineCount = 0;
                                }
                            }
                            catch (Exception e)
                            {
                                log.severe("Error parsing: " + line);
                            }
                        }
                    }

                }
                catch (IOException ex)
                {
                    log.severe("Error reading GFF file");
                    if (line != null && lineNumber != 0)
                    {
                        throw new ParserException(ex.getMessage(), ex, lineNumber, line);
                    }
                    else
                    {
                        throw new RuntimeException(ex);
                    }
                }

                trackProperties = TrackLoader.getTrackProperties(codec.getHeader());

                // Combine the features
                List<Feature> iFeatures = combiner.combineFeatures();

                if (IGV.hasInstance())
                {
                    FeatureDB.addFeatures(iFeatures);
                }

                return iFeatures;
            }

            @Override
            protected void process(List<ProgressReport> chunks)
            {

                BrowserDialog.instance().workProgress(chunks);
            }

            @Override
            protected void done()
            {
                BrowserDialog.instance().workDone();
            }

        };
        BrowserDialog.instance().workStart();
        worker.execute();
        try
        {
            List<Feature> rtn = worker.get();
            return rtn;
        }
        catch (Throwable t)
        {
            throw new RuntimeException(t);
        }

    }

    public int count(final double total,final InputStream in) throws IOException
    {
        BrowserDialog.instance().workInit(total);
        SwingWorker<Integer, ProgressReport> worker = new SwingWorker<Integer, ProgressReport>()
        {

            @Override
            protected Integer doInBackground() throws Exception
            {
                InputStream is = new BufferedInputStream(in);
                try
                {
                    byte[] c = new byte[1024];
                    int count = 0;
                    int readChars = 0;
                    int bytesRead = 0;
                    boolean empty = true;
                    while ((readChars = is.read(c)) != -1)
                    {
                        bytesRead += readChars;
                        List<ProgressReport> progress = new ArrayList<ProgressReport>();
                        progress.add(new ProgressReport("Read bytes " + bytesRead + " of " + total,bytesRead));
                        publish(progress.toArray(new ProgressReport[progress.size()]));
                        empty = false;
                        for (int i = 0; i < readChars; ++i)
                        {
                            if (c[i] == '\n')
                            {
                                ++count;
                            }
                        }
                    }
                    return (count == 0 && !empty) ? 1 : count;
                }
                finally
                {
                    BaseSpaceUtil.dispose(is);
                    BaseSpaceUtil.dispose(in);
                }
            }

            @Override
            protected void process(List<ProgressReport> chunks)
            {
                BrowserDialog.instance().workProgress(chunks);
            }

            @Override
            protected void done()
            {
                BrowserDialog.instance().workDone();
            }
            
        };
        BrowserDialog.instance().workStart();
        worker.execute();
        try
        {
            return worker.get();
        }
        catch (Throwable t)
        {
            throw new RuntimeException(t);
        }
        
       
    }

    public List<org.broad.tribble.Feature> loadFeaturesBackup(BufferedReader reader, Genome genome)
    {
        String line = null;
        int lineNumber = 0;
        GFFCodec codec = new GFFCodec(genome);
        GFFFeatureSource.GFFCombiner combiner = new GFFFeatureSource.GFFCombiner();
        try
        {
            while ((line = reader.readLine()) != null)
            {
                lineNumber++;

                if (line.startsWith("#"))
                {
                    codec.readHeaderLine(line);
                }
                else
                {
                    try
                    {
                        Feature f = codec.decode(line);
                        if (f != null)
                        {
                            combiner.addFeature((BasicFeature) f);
                        }
                    }
                    catch (Exception e)
                    {
                        log.severe("Error parsing: " + line);
                    }
                }
            }

        }
        catch (IOException ex)
        {
            log.severe("Error reading GFF file");
            if (line != null && lineNumber != 0)
            {
                throw new ParserException(ex.getMessage(), ex, lineNumber, line);
            }
            else
            {
                throw new RuntimeException(ex);
            }
        }

        trackProperties = TrackLoader.getTrackProperties(codec.getHeader());

        // Combine the features
        List<Feature> iFeatures = combiner.combineFeatures();

        if (IGV.hasInstance())
        {
            FeatureDB.addFeatures(iFeatures);
        }

        return iFeatures;
    }

    /**
     * Given a GFF File, creates a new GFF file for each type. Any feature type
     * which is part of a "gene" ( {@link GFFCodec#geneParts} ) are put in the
     * same file, others are put in different files. So features of type "gene",
     * "exon", and "mrna" would go in gene.gff, but features of type "myFeature"
     * would go in myFeature.gff.
     * 
     * @param gffFile
     * @param outputDirectory
     * @throws IOException
     */
    public static void splitFileByType(String gffFile, String outputDirectory) throws IOException
    {

        BufferedReader br = new BufferedReader(new FileReader(gffFile));
        String nextLine;
        String ext = "." + gffFile.substring(gffFile.length() - 4);

        Map<String, PrintWriter> writers = new HashMap<String, PrintWriter>();

        while ((nextLine = br.readLine()) != null)
        {
            nextLine = nextLine.trim();
            if (!nextLine.startsWith("#"))
            {
                String[] tokens = Globals.tabPattern.split(nextLine.trim().replaceAll("\"", ""), -1);

                String type = tokens[2];
                if (GFFCodec.geneParts.contains(type))
                {
                    type = "gene";
                }
                if (!writers.containsKey(type))
                {
                    writers.put(type, new PrintWriter(new FileWriter(new File(outputDirectory, type + ext))));
                }
            }
        }
        br.close();

        br = new BufferedReader(new FileReader(gffFile));
        PrintWriter currentWriter = null;
        while ((nextLine = br.readLine()) != null)
        {
            nextLine = nextLine.trim();
            if (nextLine.startsWith("#"))
            {
                for (PrintWriter pw : writers.values())
                {
                    pw.println(nextLine);
                }
            }
            else
            {
                String[] tokens = Globals.tabPattern.split(nextLine.trim().replaceAll("\"", ""), -1);
                String type = tokens[2];
                if (GFFCodec.geneParts.contains(type))
                {
                    type = "gene";
                }
                currentWriter = writers.get(type);

                if (currentWriter != null)
                {
                    currentWriter.println(nextLine);
                }
                else
                {
                    System.out.println("No writer for: " + type);
                }
            }

        }

        br.close();
        for (PrintWriter pw : writers.values())
        {
            pw.close();
        }
    }

    public TrackProperties getTrackProperties()
    {
        return trackProperties;
    }

    public static void main(String[] args) throws IOException
    {
        if (args.length < 2)
        {
            System.out.println("SpitFilesByType <gffFile> <outputDirectory>");
            return;
        }
        splitFileByType(args[0], args[1]);
    }
}
