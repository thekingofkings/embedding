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
        learnEmbedding(true);
    }

    public static void learnEmbedding(boolean useSpatial) throws Exception{
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


        File seqDir;
        String out;
        if (useSpatial) {
            seqDir = new File("../miscs/deepwalkseq");
            out = "../miscs/taxi-deepwalk.vec";
        } else {
            seqDir = new File("../miscs/deepwalkseq/taxi-crosstime.seq");
            out = "../miscs/taxi-deepwalk-nospatial.vec";
        }

        log.info("Load and vectorize");
        SentenceIterator itr = new FileSentenceIterator(seqDir);
        TokenizerFactory t = new DefaultTokenizerFactory();

        log.info("Building model");
        Word2Vec w2v = new Word2Vec.Builder().minWordFrequency(2)
                .layerSize(20).iterations(1).windowSize(24)
                .iterate(itr).tokenizerFactory(t).build();

        log.info("Fitting w2v model");
        w2v.fit();

        log.info("Writing w2v vectors into files");
        WordVectorSerializer.writeWordVectors(w2v, out);
    }

    public static void main(String[] argv) {
        try {
            if (argv.length > 1 && argv[0].equals("nospatial"))
                learnEmbedding(false);
            else
                learnEmbedding();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
