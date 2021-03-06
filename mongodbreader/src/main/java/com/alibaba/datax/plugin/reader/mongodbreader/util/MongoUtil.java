package com.alibaba.datax.plugin.reader.mongodbreader.util;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.reader.mongodbreader.KeyConstant;
import com.alibaba.datax.plugin.reader.mongodbreader.MongoDBReaderErrorCode;

import com.mongodb.*;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jianying.wcj on 2015/3/17 0017.
 * Modified by mingyan.zc on 2016/6/13.
 */
public class MongoUtil {
    private static final Logger LOG = LoggerFactory
            .getLogger(MongoUtil.class);
    public static MongoClient initMongoClient(Configuration conf) {

        List<Object> addressList = conf.getList(KeyConstant.MONGO_ADDRESS);
        if (addressList == null || addressList.size() <= 0) {
            throw DataXException.asDataXException(MongoDBReaderErrorCode.ILLEGAL_VALUE, "不合法参数");
        }
        try {
            return new MongoClient(parseServerAddress(addressList));
        } catch (UnknownHostException e) {
            throw DataXException.asDataXException(MongoDBReaderErrorCode.ILLEGAL_ADDRESS, "不合法的地址");
        } catch (NumberFormatException e) {
            throw DataXException.asDataXException(MongoDBReaderErrorCode.ILLEGAL_VALUE, "不合法参数");
        } catch (Exception e) {
            throw DataXException.asDataXException(MongoDBReaderErrorCode.UNEXCEPT_EXCEPTION, "未知异常");
        }
    }

    // 增加2种mongo鉴权方式,并设置 read preference
    public static MongoClient initCredentialMongoClient(Configuration conf, String userName, String password, String database) {
        List addressList = conf.getList("address");
        if (!isHostPortPattern(addressList)) {
            throw DataXException.asDataXException(MongoDBReaderErrorCode.ILLEGAL_VALUE, "不合法参数");
        }
        try {
            String addressListStr = org.apache.commons.lang3.StringUtils.join(addressList.toArray(), ",");
            String uri= null;
            MongoClientURI mongoClientURI  =null;
            MongoClient mongoClient  = null;
            MongoDatabase db = null;






            try {

                uri= "mongodb://" + userName + ":" + password + "@" + addressListStr + "/?authMechanism=SCRAM-SHA-1";
                mongoClientURI = new MongoClientURI(uri);
                mongoClient = new MongoClient(mongoClientURI);
                Document document = mongoClient.getDatabase(database).runCommand(new Document("ping", 1));
            } catch (Exception e) {
                LOG.error(e.getMessage(),e);
            }


            if(null==mongoClient){
                try {
                    uri = "mongodb://" + userName + ":" + password + "@" + addressListStr + "/" + database;

                    mongoClientURI = new MongoClientURI(uri);
                    mongoClient = new MongoClient(mongoClientURI);
                    Document document = mongoClient.getDatabase(database).runCommand(new Document("ping", 1));
                } catch (Exception e) {
                    LOG.error(e.getMessage(),e);
                }
            }


            if(null==mongoClient){
                MongoCredential credential = MongoCredential.createCredential(userName, database, password.toCharArray());
                mongoClient = new MongoClient(parseServerAddress(addressList), Arrays.asList(new MongoCredential[]{credential}));
                mongoClient.setReadPreference(com.mongodb.ReadPreference.secondaryPreferred());
                Document document = mongoClient.getDatabase(database).runCommand(new Document("ping", 1));
            }




            return mongoClient;


        } catch (UnknownHostException var14) {
            throw DataXException.asDataXException(MongoDBReaderErrorCode.ILLEGAL_ADDRESS, "不合法的地址");
        } catch (NumberFormatException var15) {
            throw DataXException.asDataXException(MongoDBReaderErrorCode.ILLEGAL_VALUE, "不合法参数");
        } catch (Exception var16) {
            throw DataXException.asDataXException(MongoDBReaderErrorCode.UNEXCEPT_EXCEPTION, "未知异常");
        }

    }

    /**
     * 判断地址类型是否符合要求
     *
     * @param addressList
     * @return
     */
    private static boolean isHostPortPattern(List<Object> addressList) {
        for (Object address : addressList) {
            String regex = "(\\S+):([0-9]+)";
            if (!((String) address).matches(regex)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 转换为mongo地址协议
     *
     * @param rawAddressList
     * @return
     */
    private static List<ServerAddress> parseServerAddress(List<Object> rawAddressList) throws UnknownHostException {
        List<ServerAddress> addressList = new ArrayList<ServerAddress>();
        for (Object address : rawAddressList) {
            String[] tempAddress = ((String) address).split(":");
            try {
                ServerAddress sa = new ServerAddress(tempAddress[0], Integer.valueOf(tempAddress[1]));
                addressList.add(sa);
            } catch (Exception e) {
                throw new UnknownHostException();
            }
        }
        return addressList;
    }
}
