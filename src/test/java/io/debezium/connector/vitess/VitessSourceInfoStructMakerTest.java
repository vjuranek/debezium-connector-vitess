/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.vitess;

import static org.fest.assertions.Assertions.assertThat;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.junit.Test;

import io.debezium.relational.TableId;
import io.debezium.util.Collect;

public class VitessSourceInfoStructMakerTest {
    private static final String TEST_KEYSPACE = "test_keyspace";
    private static final String TEST_SHARD = "-80";
    private static final String TEST_GTID = "MySQL56/a790d864-9ba1-11ea-99f6-0242ac11000a:1-1513";
    private static final String TEST_SHARD2 = "80-";
    private static final String TEST_GTID2 = "MySQL56/a790d864-9ba1-11ea-99f6-0242ac11000b:1-1513";
    private static final String VGTID_JSON = String.format(
            "[" +
                    "{\"keyspace\":\"%s\",\"shard\":\"%s\",\"gtid\":\"%s\"}," +
                    "{\"keyspace\":\"%s\",\"shard\":\"%s\",\"gtid\":\"%s\"}" +
                    "]",
            TEST_KEYSPACE,
            TEST_SHARD,
            TEST_GTID,
            TEST_KEYSPACE,
            TEST_SHARD2,
            TEST_GTID2);

    @Test
    public void shouldGetCorrectSourceInfoSchema() {
        VitessSourceInfoStructMaker structMaker = new VitessSourceInfoStructMaker(
                "test_connector",
                "test_version",
                new VitessConnectorConfig(TestHelper.defaultConfig().build()));

        assertThat(structMaker.schema().field(SourceInfo.KEYSPACE_NAME_KEY).schema())
                .isEqualTo(Schema.STRING_SCHEMA);
        assertThat(structMaker.schema().field(SourceInfo.TABLE_NAME_KEY).schema())
                .isEqualTo(Schema.STRING_SCHEMA);
        assertThat(structMaker.schema().field(SourceInfo.VGTID_KEY).schema())
                .isEqualTo(Schema.STRING_SCHEMA);
        assertThat(structMaker.schema()).isNotNull();
    }

    @Test
    public void shouldGetCorrectSourceInfoStruct() {
        // setup fixture
        String schemaName = "test_schema";
        String tableName = "test_table";
        SourceInfo sourceInfo = new SourceInfo(new VitessConnectorConfig(TestHelper.defaultConfig().build()));
        sourceInfo.resetVgtid(
                Vgtid.of(
                        Collect.arrayListOf(
                                new Vgtid.ShardGtid(TEST_KEYSPACE, TEST_SHARD, TEST_GTID),
                                new Vgtid.ShardGtid(TEST_KEYSPACE, TEST_SHARD2, TEST_GTID2))),
                AnonymousValue.getInstant());
        sourceInfo.setTableId(new TableId(null, schemaName, tableName));
        sourceInfo.setTimestamp(AnonymousValue.getInstant());

        // exercise SUT
        Struct struct = new VitessSourceInfoStructMaker(
                "test_connector",
                "test_version",
                new VitessConnectorConfig(TestHelper.defaultConfig().build()))
                        .struct(sourceInfo);

        // verify outcome
        assertThat(struct.getString(SourceInfo.KEYSPACE_NAME_KEY)).isEqualTo(TestHelper.TEST_UNSHARDED_KEYSPACE);
        assertThat(struct.getString(SourceInfo.TABLE_NAME_KEY)).isEqualTo(tableName);
        assertThat(struct.getString(SourceInfo.VGTID_KEY)).isEqualTo(VGTID_JSON);
    }
}
