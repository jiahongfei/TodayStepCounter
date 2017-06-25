package com.today.step.lib;

import android.content.Context;

import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.DataBaseConfig;
import com.litesuits.orm.db.assit.QueryBuilder;
import com.litesuits.orm.db.model.ConflictAlgorithm;

import java.util.Collection;
import java.util.List;

public class StepDbUtils {

    public static String DB_NAME;
    public static LiteOrm liteOrm;

    public synchronized static void createDb(Context _activity, String DB_NAME) {
        DB_NAME = DB_NAME + ".db";
        if (liteOrm == null) {
            DataBaseConfig config = new DataBaseConfig(_activity, DB_NAME);
            config.dbVersion = 2;
            liteOrm = LiteOrm.newCascadeInstance(config);
//            liteOrm = LiteOrm.newCascadeInstance(_activity, DB_NAME);
            liteOrm.setDebugged(true);
        }
    }

    public synchronized static LiteOrm getLiteOrm() {
        return liteOrm;
    }

    /**
     * 插入一条记录
     *
     * @param t
     */
    public synchronized static <T> void insert(T t) {
        liteOrm.save(t);
    }

    /**
     * 插入所有记录
     *
     * @param list
     */
    public synchronized static <T> void insertAll(List<T> list) {
        liteOrm.save(list);
    }

    /**
     * 查询所有
     *
     * @param cla
     * @return
     */
    public synchronized static <T> List<T> getQueryAll(Class<T> cla) {
        return liteOrm.query(cla);
    }

    /**
     * 查询  某字段 等于 Value的值
     *
     * @param cla
     * @param field
     * @param value
     * @return
     */
    public synchronized static <T> List<T> getQueryByWhere(Class<T> cla, String field, String[] value) {
        return liteOrm.<T>query(new QueryBuilder(cla).where(field + "=?", value));
    }

    /**
     * 查询  某字段 等于 Value的值  可以指定从1-20，就是分页
     *
     * @param cla
     * @param field
     * @param value
     * @param start
     * @param length
     * @return
     */
    public synchronized static <T> List<T> getQueryByWhereLength(Class<T> cla, String field, String[] value, int start, int length) {
        return liteOrm.<T>query(new QueryBuilder(cla).where(field + "=?", value).limit(start, length));
    }

    /**
     * 删除所有 某字段等于 Vlaue的值
     * @param cla
     * @param field
     * @param value
     */
//        public static <T> void deleteWhere(Class<T> cla,String field,String [] value){
//            liteOrm.delete(cla, WhereBuilder.create().where(field + "=?", value));
//        }

    /**
     * 删除所有
     *
     * @param cla
     */
    public synchronized static <T> void deleteAll(Class<T> cla) {
        liteOrm.deleteAll(cla);
    }

    public synchronized static <T> void delete(Collection<T> collection){
        liteOrm.delete(collection);
    }

    /**
     * 仅在以存在时更新
     *
     * @param t
     */
    public synchronized static <T> void update(T t) {
        liteOrm.update(t, ConflictAlgorithm.Replace);
    }


    public synchronized static <T> void updateALL(List<T> list) {
        liteOrm.update(list);
    }

    public synchronized static void closeDb(){
        liteOrm.close();
    }

}
