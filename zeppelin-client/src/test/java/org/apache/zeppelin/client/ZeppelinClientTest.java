package org.apache.zeppelin.client;

import org.apache.zeppelin.common.NotebookInfo;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-12-09 13:15
 **/
public class ZeppelinClientTest {
    private static ZeppelinClient zeppelinClient;

    @BeforeClass
    public static void init() throws Exception {
        ClientConfig clientConfig = new ClientConfig("http://localhost:8082/next");
        zeppelinClient = new ZeppelinClient(clientConfig);
    }

    @Test
    public void testCreateNote() throws Exception {
        System.out.println(zeppelinClient.createNoteWithParagraph("/tis/order4", "tis-order2"
                //  ZeppelinClient.TIS_JDBC_GROUP)
        ));
    }

    @Test
    public void testDeleteInterpreter() throws Exception {
        zeppelinClient.deleteInterpreter("order3");
    }


    @Test
    public void testNoteList() throws Exception {
        List<NotebookInfo> notebookInfos = zeppelinClient.listNotes();
        for (NotebookInfo note : notebookInfos) {
            System.out.println(note.getId() + " path:" + note.getPath());
        }
    }

    @Test
    public void testCreateInterpreter() throws Exception {

//        String md1Name = "md1Name";
//        String md1Dep = "md1Dep";

//        String reqBody1 = "{\"name\":\"" + md1Name + "\",\"group\":\"md\"," +
//                "\"properties\":{\"propname\": {\"value\": \"propvalue\", \"name\": \"propname\", " +
//                "\"type\": \"textarea\"}}," +
//                "\"interpreterGroup\":[{\"class\":\"org.apache.zeppelin.markdown.Markdown\"," +
//                "\"name\":\"md\"}]," +
//                "\"dependencies\":[ {\n" +
//                "      \"groupArtifactVersion\": \"" + md1Dep + "\",\n" +
//                "      \"exclusions\":[]\n" +
//                "    }]," +
//                "\"option\": { \"remote\": true, \"session\": false }}";

        //    System.out.println(reqBody1);
        // String jdbcUrl = "";
        String order2InterpreterId = zeppelinClient.createJDBCInterpreter("order3");
        System.out.println("order2InterpreterId:" + order2InterpreterId);
    }
}
