package org.apache.zeppelin.common;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-12-28 17:04
 **/
public class NotebookInfo {
    private final String path;
    private final String id;

    public NotebookInfo(String id, String path) {
        this.path = path;
        this.id = id;
    }

    public String getPath() {
        return this.path;
    }

    public String getId() {
        return this.id;
    }
}
