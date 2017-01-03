package embedding;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
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

    public static void learnEmbedding() throws Exception{
        String filePath = new File("../miscs/taxi-crosstime-fifth.seq").getAbsolutePath();

        log.info("Load and vectorize");
        SentenceIterator itr = new BasicLineIterator(filePath);
        TokenizerFactory t = new DefaultTokenizerFactory();

        log.info("Building model");
        Word2Vec w2v = new Word2Vec.Builder().minWordFrequency(2)
                .layerSize(20).iterations(1).windowSize(24)
                .iterate(itr).tokenizerFactory(t).build();

        log.info("Fitting w2v model");
        w2v.fit();

        log.info("Writing w2v vectors into files");
        WordVectorSerializer.writeWordVectors(w2v, "../miscs/taxi-deepwalk.vec");
    }

    public static void main(String[] argv) {
        try {
            learnEmbedding();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
