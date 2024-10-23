/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SQLiteSystemTablesContainer implements DBSObjectContainer {

    private static final Log log = Log.getLog(SQLiteSystemTablesContainer.class);

    private static final List<String> WELL_KNOWN_SYSTABLE_NAMES = List.of(
        "sqlite_master", "sqlite_schema",
        "sqlite_temp_master", "sqlite_temp_schema",
        "dbstat", "sqlite_sequence",
        "sqlite_stat1", "sqlite_stat2", "sqlite_stat3", "sqlite_stat4"
    );


    private final SQLiteDataSource sqliteDataSource;
    private final List<SQLiteSystemTable> systemTables;
    private final Map<String, SQLiteSystemTable> systemTableByName;

    private SQLiteSystemTablesContainer(@NotNull DBRProgressMonitor monitor, @NotNull SQLiteDataSource sqliteDataSource) throws DBCException {
        this.sqliteDataSource = sqliteDataSource;
        this.systemTables = this.loadSysTablesInformation(monitor);
        this.systemTableByName = this.systemTables.stream().collect(Collectors.toMap(a -> a.getName().toLowerCase(), a -> a));
    }

    public static SQLiteSystemTablesContainer collectSysTablesInformation(@NotNull DBRProgressMonitor monitor, @NotNull SQLiteDataSource sqliteDataSource) throws DBCException {
        return new SQLiteSystemTablesContainer(monitor, sqliteDataSource);
    }

    private List<SQLiteSystemTable> loadSysTablesInformation(@NotNull DBRProgressMonitor monitor) throws DBCException {
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "SQLite system objects discovery")) {
            return WELL_KNOWN_SYSTABLE_NAMES.stream().map(tableName -> {
                try {
                    return tryPrepareSysTableSchema(session, tableName);
                } catch (DBCException e) {
                    log.error("Error reflecting SQLite system table " + tableName, e);
                    return null;
                }
            }).filter(Objects::nonNull).toList();
        }
    }

    private SQLiteSystemTable tryPrepareSysTableSchema(DBCSession session, String tableName) throws DBCException {
        try (DBCStatement stmt = session.prepareStatement(DBCStatementType.QUERY, "select * from " + tableName, false, false, false)) {
            stmt.setLimit(0, 1);
            try (DBCResultSet rs = stmt.openResultSet()) {
                if (rs != null) {
                    return new SQLiteSystemTable(this.sqliteDataSource, tableName, rs.getMeta());
                }
            }
        } catch (DBCException ex) {
            // select from the system table failed because it doesn't exist;
            // it may still automatically appear later depending on the actual objects created by the user
        }
        return null;
    }

    @NotNull
    @Override
    public String getName() {
        return this.sqliteDataSource.getName();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return this.sqliteDataSource.getParentObject();
    }

    @Override
    public DBPDataSource getDataSource() {
        return this.sqliteDataSource;
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return this.systemTables;
    }

    @Nullable
    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return this.systemTableByName.get(childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return DBSTable.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {

    }

    public class SQLiteSystemTable implements DBSTable, DBDPseudoAttributeContainer {
        private final GenericDataSource dataSource;
        private final String name;
        private final List<DBSEntityAttribute> attributes;
        private final Map<String, DBSEntityAttribute> attributesByName;

        private DBDPseudoAttribute[] allPseudoAttributes = null;

        public SQLiteSystemTable(GenericDataSource dataSource, String name, DBCResultSetMetaData attributes) {
            this.dataSource = dataSource;
            this.name = name;
            this.attributes = attributes.getAttributes().stream().map(a -> new SQLiteSystemAttribute(
                a.getName(),
                a.getOrdinalPosition(),
                a.getTypeName(),
                a.getFullTypeName(),
                a.getTypeID(),
                a.getDataKind(),
                a.getScale(),
                a.getPrecision(),
                a.getMaxLength(),
                a.getTypeModifiers()
            )).collect(Collectors.toList());

            this.attributesByName = this.attributes.stream().collect(Collectors.toMap(a -> a.getName().toLowerCase(), a -> a));
        }

        public DBDPseudoAttribute[] getPseudoAttributes() throws DBException {
            return null;
        }

        @Override
        public DBDPseudoAttribute[] getAllPseudoAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
            return this.allPseudoAttributes != null ? this.allPseudoAttributes : (this.allPseudoAttributes = SQLiteTable.obtainAllPseudoAttributes(this.dataSource, this, monitor));
        }

        @NotNull
        @Override
        public String getName() {
            return this.name;
        }

        @Nullable
        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public boolean isPersisted() {
            return true;
        }

        @NotNull
        @Override
        public String getFullyQualifiedName(DBPEvaluationContext context) {
            return this.name;
        }

        @NotNull
        @Override
        public DBSEntityType getEntityType() {
            return DBSEntityType.TYPE;
        }

        @Nullable
        @Override
        public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
            return this.attributes;
        }

        @Nullable
        @Override
        public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
            return this.attributesByName.get(attributeName.toLowerCase());
        }

        @Nullable
        @Override
        public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
            return null;
        }

        @Nullable
        @Override
        public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
            return null;
        }

        @Override
        public DBSObject getParentObject() {
            return SQLiteSystemTablesContainer.this;
        }

        @Override
        public DBPDataSource getDataSource() {
            return this.dataSource;
        }

        @Override
        public boolean isView() {
            return false;
        }

        @Override
        public Collection<? extends DBSTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
            return null;
        }

        @Nullable
        @Override
        public Collection<? extends DBSTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
            return null;
        }

        @Nullable
        @Override
        public List<? extends DBSTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
            return null;
        }

        private class SQLiteSystemAttribute implements DBSEntityAttribute {
            private final String name;
            private final int ordinalPosition;
            private final String typeName;
            private final String fullTypeName;
            private final int typeId;
            private final DBPDataKind dataKind;
            private final Integer scale;
            private final Integer precision;
            private final long maxLength;
            private final long typeModifiers;

            private SQLiteSystemAttribute(
                String name,
                int ordinalPosition,
                String typeName,
                String fullTypeName,
                int typeId,
                DBPDataKind dataKind,
                Integer scale,
                Integer precision,
                long maxLength,
                long typeModifiers
            ) {
                this.name = name;
                this.ordinalPosition = ordinalPosition;
                this.typeName = typeName;
                this.fullTypeName = fullTypeName;
                this.typeId = typeId;
                this.dataKind = dataKind;
                this.scale = scale;
                this.precision = precision;
                this.maxLength = maxLength;
                this.typeModifiers = typeModifiers;
            }

            @Nullable
            @Override
            public String getDefaultValue() {
                return null;
            }

            @Override
            public boolean isRequired() {
                return true;
            }

            @Override
            public boolean isAutoGenerated() {
                return true;
            }

            @NotNull
            @Override
            public DBSEntity getParentObject() {
                return SQLiteSystemTable.this;
            }

            @Override
            public DBPDataSource getDataSource() {
                return SQLiteSystemTable.this.getDataSource();
            }

            @NotNull
            @Override
            public String getName() {
                return this.name;
            }

            @Nullable
            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public int getOrdinalPosition() {
                return this.ordinalPosition;
            }

            @Override
            public boolean isPersisted() {
                return true;
            }

            @NotNull
            @Override
            public String getTypeName() {
                return this.typeName;
            }

            @NotNull
            @Override
            public String getFullTypeName() {
                return this.fullTypeName;
            }

            @Override
            public int getTypeID() {
                return this.typeId;
            }

            @NotNull
            @Override
            public DBPDataKind getDataKind() {
                return this.dataKind;
            }

            @Nullable
            @Override
            public Integer getScale() {
                return this.scale;
            }

            @Nullable
            @Override
            public Integer getPrecision() {
                return this.precision;
            }

            @Override
            public long getMaxLength() {
                return this.maxLength;
            }

            @Override
            public long getTypeModifiers() {
                return this.typeModifiers;
            }
        }
    }
}