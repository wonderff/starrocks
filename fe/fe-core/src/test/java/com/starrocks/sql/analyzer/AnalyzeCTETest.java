// This file is licensed under the Elastic License 2.0. Copyright 2021 StarRocks Limited.
package com.starrocks.sql.analyzer;

import com.starrocks.sql.analyzer.relation.QueryRelation;
import com.starrocks.utframe.UtFrameUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

import static com.starrocks.sql.analyzer.AnalyzeTestUtil.analyzeFail;
import static com.starrocks.sql.analyzer.AnalyzeTestUtil.analyzeSuccess;

public class AnalyzeCTETest {
    // use a unique dir so that it won't be conflict with other unit test which
    // may also start a Mocked Frontend
    private static String runningDir = "fe/mocked/AnalyzeCTE/" + UUID.randomUUID().toString() + "/";

    @BeforeClass
    public static void beforeClass() throws Exception {
        UtFrameUtils.createMinStarRocksCluster(runningDir);
        AnalyzeTestUtil.init();
    }

    @AfterClass
    public static void tearDown() {
        File file = new File(runningDir);
        file.delete();
    }

    @Test
    public void testSingle() {
        analyzeSuccess("with c1 as (select * from t0) select c1.* from c1");
        analyzeSuccess("with c1 as (select * from t0) select a.* from c1 a");
        analyzeSuccess("with c1 as (select * from t0) select a.* from c1 a");

        // original table name is not allowed to access when alias-name exists
        analyzeFail("with c1 as (select * from t0) select c1.* from c1 a");
    }

    @Test
    public void testMulti() {
        // without alias
        QueryRelation query = analyzeSuccess("with "
                + "tbl1 as (select v1, v2 from t0), "
                + "tbl2 as (select v4, v5 from t1)"
                + "select tbl1.*, tbl2.* from tbl1 join tbl2 on tbl1.v1 = tbl2.v4");
        Assert.assertEquals("v1,v2,v4,v5", String.join(",", query.getColumnOutputNames()));

        // with alias
        query = analyzeSuccess("with "
                + "tbl1 as (select v1, v2 from t0),"
                + "tbl2 as (select v4, v5 from t1)"
                + "select a.*, b.* from tbl1 a join tbl2 b on a.v1 = b.v4");
        Assert.assertEquals("v1,v2,v4,v5", String.join(",", query.getColumnOutputNames()));

        // partial alias
        query = analyzeSuccess("with "
                + "tbl1 as (select v1, v2 from t0),"
                + "tbl2 as (select v4, v5 from t1)"
                + "select a.*, tbl2.* from tbl1 a join tbl2 on a.v1 = tbl2.v4");
        Assert.assertEquals("v1,v2,v4,v5", String.join(",", query.getColumnOutputNames()));
    }
}
