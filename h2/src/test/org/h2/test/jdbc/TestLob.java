/*
 * Copyright 2004-2010 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.jdbc;

import java.io.OutputStream;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import org.h2.jdbc.JdbcConnection;
import org.h2.test.TestBase;

/**
 * Test the Blob, Clob, and NClob implementations.
 */
public class TestLob extends TestBase {

    private JdbcConnection conn;
    private Statement stat;

    /**
     * Run just this test.
     *
     * @param a ignored
     */
    public static void main(String... a) throws Exception {
        // System.setProperty("h2.lobInDatabase", "true");
        TestBase.createCaller().init().test();
    }

    public void test() throws Exception {
        deleteDb("lob");
        conn = (JdbcConnection) getConnection("lob");
        stat = conn.createStatement();
        stat.execute("create table test(id int, x blob)");
        testBlob(0);
        testBlob(1);
        testBlob(100);
        testBlob(100000);
        stat.execute("drop table test");
        stat.execute("create table test(id int, x clob)");
        testClob(0);
        testClob(1);
        testClob(100);
        testClob(100000);
        stat.execute("drop table test");
        conn.close();
    }

    private void testBlob(int length) throws Exception {
        Random r = new Random(length);
        byte[] data = new byte[length];
        r.nextBytes(data);
        Blob b = conn.createBlob();
        OutputStream out = b.setBinaryStream(1);
        out.write(data, 0, data.length);
        out.close();
        stat.execute("delete from test");
        PreparedStatement prep = conn.prepareStatement("insert into test values(?, ?)");
        prep.setInt(1, 1);
        prep.setBlob(2, b);
        prep.execute();
        prep.setInt(1, 2);
        b = conn.createBlob();
        b.setBytes(1, data);
        prep.setBlob(2, b);
        prep.execute();
        ResultSet rs;
        rs = stat.executeQuery("select * from test");
        rs.next();
        Blob b2 = rs.getBlob(2);
        assertEquals(length, b2.length());
        byte[] bytes = b.getBytes(1, length);
        byte[] bytes2 = b2.getBytes(1, length);
        assertEquals(bytes, bytes2);
        rs.next();
        b2 = rs.getBlob(2);
        assertEquals(length, b2.length());
        bytes2 = b2.getBytes(1, length);
        assertEquals(bytes, bytes2);
    }

    private void testClob(int length) throws Exception {
        Random r = new Random(length);
        char[] data = new char[length];

        // Unicode problem:
        // The UCS code values 0xd800-0xdfff (UTF-16 surrogates)
        // as well as 0xfffe and 0xffff (UCS non-characters)
        // should not appear in conforming UTF-8 streams.
        // (String.getBytes("UTF-8") only returns 1 byte for 0xd800-0xdfff)
        for (int i = 0; i < length; i++) {
            char c;
            do {
                c = (char) r.nextInt();
            } while (c >= 0xd800 && c <= 0xdfff);
            data[i] = c;
        }
        Clob c = conn.createClob();
        Writer out = c.setCharacterStream(1);
        out.write(data, 0, data.length);
        out.close();
        stat.execute("delete from test");
        PreparedStatement prep = conn.prepareStatement("insert into test values(?, ?)");
        prep.setInt(1, 1);
        prep.setClob(2, c);
        prep.execute();
        c = conn.createClob();
        c.setString(1, new String(data));
        prep.setInt(1, 2);
        prep.setClob(2, c);
        prep.execute();
        ResultSet rs;
        rs = stat.executeQuery("select * from test");
        rs.next();
        Clob c2 = rs.getClob(2);
        assertEquals(length, c2.length());
        String s = c.getSubString(1, length);
        String s2 = c2.getSubString(1, length);
        rs.next();
        c2 = rs.getClob(2);
        assertEquals(length, c2.length());
        s2 = c2.getSubString(1, length);
        assertEquals(s, s2);
    }

}
