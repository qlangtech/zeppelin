package org.apache.zeppelin.elasticsearch;

import java.util.Properties;

/**
 * 先占一个位置，说不定以后需要扩展可以派上用场
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-12-13 16:21
 **/
public class TISElasticsearchInterpreter extends ElasticsearchInterpreter {

    public TISElasticsearchInterpreter(Properties property) {
        super(property);
    }

    @Override
    public void open() {
        super.open();
    }
}
