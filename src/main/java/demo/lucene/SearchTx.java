package demo.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.nio.file.Paths;

public class SearchTx {
    private Directory dir;
    private IndexReader reader;
    private IndexSearcher is;
    private static final Logger log = LoggerFactory.getLogger(SearchTx.class);

    @Before
    public void setUp() throws Exception {
        dir= FSDirectory.open(Paths.get("D:\\lucene\\dataindex"));
        reader= DirectoryReader.open(dir);
        is=new IndexSearcher(reader);
    }

    @After
    public void tearDown() throws Exception {
        reader.close();
    }

    /**
     * 对特定单词查询及模糊查询
     *
     * @throws Exception
     */
    @Test
    public void testTermQuery() throws Exception {
        // 标准分词器
        Analyzer analyzer = new StandardAnalyzer();
        String searchField = "fileName";
        // TermQuery所给出的必须是单词，不然查不到
        String q = "monitor";
        String str = "spring and vehicle";

        // 建立查询解析器
        //searchField:要查询的字段；
        //analyzer:标准分词器实例
        QueryParser parser = new QueryParser(searchField, analyzer);
        Query query = parser.parse(q);
        // 一个Term表示来自文本的一个单词。
//        Term t = new Term(searchField, q);
        // 为Term构造查询。
//        Query query = new TermQuery(t);
        /**
         * 1.需要根据条件查询          *
         * 2.最大可编辑数，取值范围0，1，2 
         * 允许我的查询条件的值，可以错误几个字符
         *
         */
//        Query query2 = new FuzzyQuery(new Term(searchField,str),2);
        QueryParser parser2 = new QueryParser(searchField, analyzer);
        Query query2 = parser2.parse(str);
        TopDocs hits = is.search(query, 10);
        // hits.totalHits：查询的总命中次数。即在几个文档中查到给定单词
        log.info("匹配 '" + q + "'，总共查询到" + hits.totalHits + "个文档");
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = is.doc(scoreDoc.doc);
            log.info(doc.get("fullPath"));
        }
        TopDocs hits2 = is.search(query2, 10);
        // hits.totalHits：查询的总命中次数。即在几个文档中查到给定单词
        log.info("匹配 '" + str + "'，总共查询到" + hits2.totalHits + "个文档");
        for (ScoreDoc scoreDoc : hits2.scoreDocs) {
            Document doc = is.doc(scoreDoc.doc);
            log.info(doc.get("fullPath"));
        }
    }

    /**
     * 搜索并将结果中的关键词高亮显示
     *
     * @param indexDir
     * @param par
     * @throws Exception
     */
    public static void highlightSearch(String indexDir, String keyword) throws Exception{

        //得到读取索引文件的路径
        Directory dir = FSDirectory.open(Paths.get(indexDir));

        //通过dir得到的路径下的所有的文件
        IndexReader reader = DirectoryReader.open(dir);

        //建立索引查询器
        IndexSearcher searcher = new IndexSearcher(reader);

        //中文分词器
        SmartChineseAnalyzer analyzer=new SmartChineseAnalyzer();

        //建立查询解析器
        /**
         * 第一个参数是要查询的字段；
         * 第二个参数是分析器Analyzer
         * */
        QueryParser parser = new QueryParser("fileName", analyzer);

        //根据传进来的par查找
        Query query = parser.parse(keyword);

        //计算索引开始时间
        long start = System.currentTimeMillis();

        //开始查询
        /**
         * 第一个参数是通过传过来的参数来查找得到的query；
         * 第二个参数是要出查询的行数
         * */
        TopDocs topDocs = searcher.search(query, 10);

        //索引结束时间
        long end = System.currentTimeMillis();

        System.out.println("匹配"+ keyword +",总共花费了"+(end-start)+"毫秒,共查到"+topDocs.totalHits+"条记录。");


        //高亮显示start

        //算分
        QueryScorer scorer=new QueryScorer(query);

        //显示得分高的片段
        Fragmenter fragmenter=new SimpleSpanFragmenter(scorer);

        //设置标签内部关键字的颜色
        //第一个参数：标签的前半部分；第二个参数：标签的后半部分。
        SimpleHTMLFormatter simpleHTMLFormatter=new SimpleHTMLFormatter("<b><font color='red'>","</font></b>");

        //第一个参数是对查到的结果进行实例化；第二个是片段得分（显示得分高的片段，即摘要）
        Highlighter highlighter=new Highlighter(simpleHTMLFormatter, scorer);

        //设置片段
        highlighter.setTextFragmenter(fragmenter);

        //高亮显示end

        //遍历topDocs
        /**
         * ScoreDoc:是代表一个结果的相关度得分与文档编号等信息的对象。
         * scoreDocs:代表文件的数组
         * @throws Exception
         * */
        for(ScoreDoc scoreDoc : topDocs.scoreDocs){

            //获取文档
            Document document = searcher.doc(scoreDoc.doc);

            //输出全路径
//            System.out.println(document.get("city"));
            System.out.println(document.get("fileName"));

            String fileName = document.get("fileName");
            if(fileName!=null){

                //把全部得分高的摘要给显示出来

                //第一个参数是对哪个参数进行设置；第二个是以流的方式读入
                TokenStream tokenStream=analyzer.tokenStream("fileName", new StringReader(fileName));

                //获取最高的片段
                System.out.println(highlighter.getBestFragment(tokenStream, fileName));
            }
        }

        reader.close();
    }

    /**
     * 中文搜索并高亮显示
     *
     */
    @Test
    public void cnSearch(){
        String indexDir ="D:\\lucene\\dataindex";
        String keyword = "工作";
        try{
            highlightSearch(indexDir,keyword);
        }catch (Exception e){
            log.info("中文搜索时报错",e);
        }

    }
}
