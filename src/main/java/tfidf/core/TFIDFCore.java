package tfidf.core;

import base.BaseFileUtil;
import tfidf.bean.TFIDFTextBean;
import tfidf.bean.WordTFIDFBean;
import tfidf.util.TFIDFFileUtil;
import util.SegmentUtil;
import util.bean.TermBean;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author yiding
 */
public class TFIDFCore {

    private HashMap<String, Integer> wordInDocNum = new HashMap<>();
    private HashSet<String> filePathSet = new HashSet<>();
    private int segType = 1;

    /**
     * 封装文本
     */
    public ArrayList<TFIDFTextBean> getTextBean(ArrayList<String> fileList) throws IOException {
        ArrayList<TFIDFTextBean> textBeanList = new ArrayList<>();
        for (String path : fileList) {
            TFIDFTextBean textBean = new TFIDFTextBean();
            // 每篇文章的词频统计
            HashMap<String, Integer> wordCount = new HashMap<>();
            // 每篇文章的词语数量
            int wordNumber = 0;
            String content = BaseFileUtil.readFileAllContent(path);
            // 这种分词方式，过滤了停止词，停止词可见：https://github.com/hankcs/HanLP/blob/master/data/dictionary/stopwords.txt
            List<TermBean> termList = SegmentUtil.segment(content, segType);
            for (TermBean term : termList) {
                // 文章词频统计
                if (!wordCount.containsKey(term.getWord())) {
                    wordCount.put(term.getWord(), 1);
                } else {
                    wordCount.put(term.getWord(), wordCount.get(term.getWord()) + 1);
                }
                // 统计文章词语总数
                wordNumber++;
                // 统计该词语在多少篇文章中出现过
                calculateWordInDocNum(term.getWord(), path);
            }

            textBean.setTextPath(path);
            textBean.setWordCount(wordCount);
            textBean.setWordNumber(wordNumber);
            textBeanList.add(textBean);
        }
        return textBeanList;
    }

    /**
     * 保存IDF模型
     */
    public void saveIDFModel(String outPath, ArrayList<String> fileList, int textNumber) throws IOException {
        for (String path : fileList) {
            String content = BaseFileUtil.readFileAllContent(path);
            // 这种分词方式，过滤了停止词，停止词可见：https://github.com/hankcs/HanLP/blob/master/data/dictionary/stopwords.txt
            List<TermBean> termList = SegmentUtil.segment(content, segType);
            for (TermBean term : termList) {
                // 统计该词语在多少篇文章中出现过
                calculateWordInDocNum(term.getWord(), path);
            }
        }
        TFIDFFileUtil.writeFileByLine(outPath, wordInDocNum, textNumber);
    }

    /**
     * 保存tf-idf模型
     */
    public void saveTFIDFModel(String outPath, ArrayList<TFIDFTextBean> arrTextBeans, int textNumber) throws IOException {
        List<WordTFIDFBean> wordTFIDFList = calculateTFIDF(arrTextBeans, textNumber);
        TFIDFFileUtil.writeFileByLine(outPath, wordTFIDFList);
    }

    /**
     * 计算tf-idf
     */
    private List<WordTFIDFBean> calculateTFIDF(ArrayList<TFIDFTextBean> arrTextBeans, int textNumber) {
        List<WordTFIDFBean> wordTFIDFList = new ArrayList<>();
        for (TFIDFTextBean textBean : arrTextBeans) {
            HashMap<String, Integer> wordCount = textBean.getWordCount();
            for (Entry entry : wordCount.entrySet()) {
                WordTFIDFBean wordTFIDF = new WordTFIDFBean();
                float tf = Float.parseFloat(entry.getValue().toString()) / (float) textBean.getWordNumber();
                float idf = (float) Math.log(textNumber / (float) wordInDocNum.get(entry.getKey().toString()));
                String word = entry.getKey().toString();

                wordTFIDF.setPath(textBean.getTextPath());
                wordTFIDF.setWord(word);
                wordTFIDF.setTf(tf);
                wordTFIDF.setIdf(idf);
                wordTFIDF.setTFIDF(tf * idf);

                wordTFIDFList.add(wordTFIDF);
            }
        }
        return wordTFIDFList;
    }


    /**
     * 计算一个词语在多少篇文本出现过
     */
    private void calculateWordInDocNum(String word, String filePath) {
        if (!wordInDocNum.containsKey(word)) {
            wordInDocNum.put(word, 1);
            filePathSet.add(filePath);
        } else {
            if (!filePathSet.contains(filePath)) {
                wordInDocNum.put(word, wordInDocNum.get(word) + 1);
            }
        }
    }
}
