package embedding;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.FileSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
//import org.nd4j.jita.conf.CudaEnvironment;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.util.DataTypeUtil;
//import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Learn deep walk embedding on the sample sequences.
 *
 * Created by kok on 1/2/17.
 */
public class DeepWalk {
    private static Logger log = LoggerFactory.getLogger(DeepWalk.class);

    public static void learnEmbedding() throws Exception {
        learnEmbedding("tract", "usespatial");
    }

    public static void learnEmbedding(String regionLevel, String spatialGF) throws Exception{
//        DataTypeUtil.setDTypeForContext(DataBuffer.Type.HALF);
//        Nd4j.create(32);
//
//        CudaEnvironment.getInstance().getConfiguration()
//                // key option enabled
//                .allowMultiGPU(false)
//
//                // we're allowing larger memory caches
//                .setMaximumDeviceCache(4L * 1024L * 1024L * 1024L)
//
//                // cross-device access is used for faster model averaging over pcie
//                .allowCrossDeviceAccess(false);


        File seqDir = new File(String.format("../miscs/deepwalkseq-%s", regionLevel));
        String out = String.format("../miscs/taxi-deepwalk-%s-%s.vec", regionLevel, spatialGF);
        int layerSize = 20;
        if (regionLevel.equals("CA"))
            layerSize = 8;
        else if (regionLevel.equals("tract"))
            layerSize = 20;

        log.info("Load and vectorize");
        SentenceIterator itr = new FileSentenceIterator(seqDir);
        TokenizerFactory t = new DefaultTokenizerFactory();

        log.info("Building model");
        Word2Vec w2v = new Word2Vec.Builder().minWordFrequency(2)
                .layerSize(layerSize).iterations(1).windowSize(LayeredGraph.numLayer)
                .iterate(itr).tokenizerFactory(t).build();

        log.info("Fitting w2v model");
        w2v.fit();

        log.info("Writing w2v vectors into files");
        WordVectorSerializer.writeWordVectors(w2v, out);
    }

    public static void checkInputFile(String regionLevel, String spatialGF) {
        File sgSeq = new File(String.format("../miscs/deepwalkseq-%s/taxi-spatial.seq", regionLevel));
        if (spatialGF.equals("usespatial") && !sgSeq.exists()) {
            System.out.format("The spatial graph samples for %s do not exist, but we need it! Generating ...\n", regionLevel);
            if (regionLevel.equals("tract")) {
                SpatialGraph.numSamples = 1_000_000;
                SpatialGraph.numLayer = 8;
            } else {
                SpatialGraph.numSamples = 80_000;
                SpatialGraph.numLayer = 24;
            }
            SpatialGraph.outputSampleSequence(regionLevel);
        } else if (spatialGF.equals("nospatial") && sgSeq.exists()) {
            System.out.format("The spatial graph samples for %s exist, but we do not need it! Deleting ...\n", regionLevel);
            sgSeq.delete();
        }
        File ctSeq = new File(String.format("../miscs/deepwalkseq-%s/taxi-crosstime.seq", regionLevel));
        if (!ctSeq.exists()) {
            System.out.format("The crosstime graph samples for %s do not exists! Generating ...\n", regionLevel);
            if (regionLevel.equals("tract")) {
                CrossTimeGraph.numSamples = 7_000_000;
                CrossTimeGraph.numLayer = 8;
            } else {
                CrossTimeGraph.numSamples = 2_000_000;
                CrossTimeGraph.numLayer = 24;
            }
            CrossTimeGraph.outputSampleSequence(regionLevel);
        }
    }

    /**
     * Train deepwalk on given graph.
     * @param argv
     *  argv[0] is either "tract" or "CA", which defines the regionLevel
     *  argv[1] is either "usespatial" or "nospatial", which defines the spatial graph flag.
     */
    public static void main(String[] argv) {
        try {
            String regionLevel = "tract";
            String spatialGF = "usespatial";
            if (argv.length > 0) {
                regionLevel = argv[0];
                System.out.format("word2vec learn embedding at %s level.\n", regionLevel);
            }
            if (argv.length > 1) {
                spatialGF = argv[1];
                System.out.println("word2vec use only crosstime graph.");
            }
            checkInputFile(regionLevel, spatialGF);
            learnEmbedding(regionLevel, spatialGF);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
