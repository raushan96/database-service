package com.monederobingo.database.common.db.adapter;

import com.monederobingo.database.common.db.util.DbBuilder;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.*;

public class QueryAgentIntegrationTest extends BaseRepositoryIntegrationTest
{

    private static final String CREATE_DUMMY_TABLE = "CREATE TABLE IF NOT EXISTS dummy (" +
            "  dummy_id    SERIAL PRIMARY KEY," +
            "  name        TEXT," +
            "  description TEXT," +
            "  value       TEXT" +
            ");";

    @Before
    public void setUp() throws Exception {
        executeFixture(CREATE_DUMMY_TABLE);
    }

    @Test
    public void testGetConnection() throws Exception {
        Connection connection = getQueryAgent().getConnection();
        assertNotNull(connection);
        assertFalse(connection.getAutoCommit());
        assertFalse(connection.isClosed());
    }

    @Test
    public void testExecuteInsert() throws Exception {
        final String sql = "INSERT INTO dummy (name, description, value) VALUES ('name1', 'desc1', 'value1');";
        long id = getQueryAgent().executeInsert(sql, "dummy_id");
        assertTrue(id > 0);
        Dummy dummy = getDummyById(id);
        assertEquals("name1", dummy.name);
        assertEquals("desc1", dummy.description);
        assertEquals("value1", dummy.value);
    }

    @Test
    public void testExecuteUpdate() throws Exception {
        final String insertSql = "INSERT INTO dummy (name, description, value) VALUES ('name1', 'desc1', 'value1');";
        long dummyId = executeInsert(insertSql);
        assertTrue(dummyId > 0);
        Dummy dummyBeforeUpdate = getDummyById(dummyId);
        assertEquals("name1", dummyBeforeUpdate.name);
        assertEquals("desc1", dummyBeforeUpdate.description);
        assertEquals("value1", dummyBeforeUpdate.value);
        final String updateSql = "UPDATE dummy SET name = 'name2', description = 'desc2', value = 'value2' WHERE dummy_id = " + dummyId + ";";
        long affectedRows = getQueryAgent().executeUpdate(updateSql);
        assertEquals(1, affectedRows);
        Dummy dummyAfterUpdate = getDummyById(dummyId);
        assertEquals("name2", dummyAfterUpdate.name);
        assertEquals("desc2", dummyAfterUpdate.description);
        assertEquals("value2", dummyAfterUpdate.value);
    }

    @Test
    public void testSelectList() throws Exception {
        List<Dummy> dummies = getQueryAgent().selectList(new DbBuilder<Dummy>() {
            @Override
            public String sql() {
                return "SELECT * FROM dummy;";
            }

            @Override
            public Object[] values() {
                return new Object[0];
            }

            @Override
            public Dummy build(ResultSet resultSet) throws SQLException
            {
                Dummy dummy = new Dummy();
                dummy.name = resultSet.getString("name");
                dummy.description = resultSet.getString("description");
                dummy.value = resultSet.getString("value");
                return dummy;
            }
        });
        assertNotNull(dummies);
        assertEquals(0, dummies.size());
        String insertSql = "INSERT INTO dummy (name, description, value) VALUES ('name1', 'desc1', 'value1');";
        executeInsert(insertSql);
        insertSql = "INSERT INTO dummy (name, description, value) VALUES ('name1', 'desc1', 'value1');";
        executeInsert(insertSql);
        dummies = getQueryAgent().selectList(new DbBuilder<Dummy>() {
            @Override
            public String sql() {
                return "SELECT * FROM dummy;";
            }

            @Override
            public Object[] values() {
                return new Object[0];
            }

            @Override
            public Dummy build(ResultSet resultSet) throws SQLException {
                Dummy dummy = new Dummy();
                dummy.name = resultSet.getString("name");
                dummy.description = resultSet.getString("description");
                dummy.value = resultSet.getString("value");
                return dummy;
            }
        });
        assertNotNull(dummies);
        assertEquals(2, dummies.size());
        assertEquals("name1", dummies.get(0).name);
        assertEquals("desc1", dummies.get(0).description);
        assertEquals("value1", dummies.get(0).value);
        assertEquals("name1", dummies.get(1).name);
        assertEquals("desc1", dummies.get(1).description);
        assertEquals("value1", dummies.get(1).value);
    }

    @Test
    public void testSelectObject() throws Exception {
        Dummy dummy = getQueryAgent().selectObject(new DbBuilder<Dummy>() {
            @Override
            public String sql() {
                return "SELECT * FROM dummy;";
            }

            @Override
            public Object[] values() {
                return new Object[0];
            }

            @Override
            public Dummy build(ResultSet resultSet) throws SQLException {
                Dummy dummy = new Dummy();
                dummy.name = resultSet.getString("name");
                dummy.description = resultSet.getString("description");
                dummy.value = resultSet.getString("value");
                return dummy;
            }
        });
        assertNull(dummy);
        String insertSql = "INSERT INTO dummy (name, description, value) VALUES ('name1', 'desc1', 'value1');";
        executeInsert(insertSql);
        dummy = getQueryAgent().selectObject(new DbBuilder<Dummy>() {
            @Override
            public String sql() {
                return "SELECT * FROM dummy;";
            }

            @Override
            public Object[] values() {
                return new Object[0];
            }

            @Override
            public Dummy build(ResultSet resultSet) throws SQLException {
                Dummy dummy = new Dummy();
                dummy.name = resultSet.getString("name");
                dummy.description = resultSet.getString("description");
                dummy.value = resultSet.getString("value");
                return dummy;
            }
        });
        assertNotNull(dummy);
        assertEquals("name1", dummy.name);
        assertEquals("desc1", dummy.description);
        assertEquals("value1", dummy.value);
    }

    @Test
    public void testBeginAndRollbackTransaction() throws Exception {
        String insertSql = "INSERT INTO dummy (name, description, value) VALUES ('name1', 'desc1', 'value1');";
        long dummyId = executeInsert(insertSql);
        Dummy dummy = getDummyById(dummyId);
        assertNotNull(dummy);
        assertTrue(getQueryAgent().isInTransaction());
        getQueryAgent().rollbackTransaction();
        assertFalse(getQueryAgent().isInTransaction());
        getQueryAgent().beginTransaction();
        executeFixture(CREATE_DUMMY_TABLE);
        dummy = getDummyById(dummyId);
        assertNull(dummy);
    }

    private Dummy getDummyById(long id) throws Exception {
        Dummy dummy = null;
        try (Statement st = getQueryAgent().getConnection().createStatement()) {
            ResultSet resultSet = st.executeQuery("SELECT * FROM dummy WHERE dummy_id = " + id);
            if (resultSet.next()) {
                dummy = new Dummy();
                dummy.name = resultSet.getString("name");
                dummy.description = resultSet.getString("description");
                dummy.value = resultSet.getString("value");
            }
        }
        return dummy;
    }

    private long executeInsert(String sql) throws Exception {
        try (Statement statement = getQueryAgent().getConnection().createStatement()) {
            statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet resultSet = statement.getGeneratedKeys();
            resultSet.next();
            return resultSet.getLong("dummy_id");
        }
    }

    private class Dummy {
        String name;
        String description;
        String value;
    }
}
