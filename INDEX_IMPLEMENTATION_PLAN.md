# INDEX_IMPLEMENTATION_PLAN.md

## Oracle to PostgreSQL Index Migration Implementation Plan

### Overview
This plan implements Oracle to PostgreSQL index migration with a strategy-based approach. Focus on easily convertible indexes (B-tree, unique, partial, composite) while generating reports for unsupported index types that require manual implementation.

### Architecture Philosophy
- **Strategy Pattern**: Similar to data transfer, use IndexMigrationStrategy to handle different index types
- **PostgreSQL-First**: Generate PostgreSQL-compatible index DDL
- **Execution Timing**: Create indexes after data transfer for performance
- **Reporting**: Generate unsupported index reports for manual review

---

## Phase 1: Infrastructure Setup and Configuration ✅
**Goal**: Establish basic index infrastructure and configuration

### Phase 1.1: Index Metadata Model
- [ ] Create `IndexMetadata.java` class with properties:
  - `indexName`, `tableName`, `schemaName`
  - `indexType` (B-tree, unique, bitmap, functional, etc.)
  - `columns` (List<IndexColumn> with column name, order, expression)
  - `uniqueIndex`, `partialIndex` (WHERE clause)
  - `tablespace`, `properties` (Oracle-specific attributes)

### Phase 1.2: Index Column Model
- [ ] Create `IndexColumn.java` class with:
  - `columnName`, `columnExpression`
  - `sortOrder` (ASC/DESC)
  - `position` (column order in composite index)

### Phase 1.3: Configuration Integration
- [ ] Add `do.indexes=true` to `application.properties`
- [ ] Update `Config.java` to include index configuration flag
- [ ] Add index extraction to feature flag documentation

---

## Phase 2: Oracle Index Extraction ✅
**Goal**: Extract comprehensive index metadata from Oracle database

### Phase 2.1: Index Extractor Implementation
- [ ] Create `IndexExtractor.java` class following existing extractor patterns
- [ ] Implement Oracle query to extract from `USER_INDEXES` and `USER_IND_COLUMNS`:
  ```sql
  SELECT i.index_name, i.table_name, i.index_type, i.uniqueness,
         i.tablespace_name, i.status, i.partitioned,
         ic.column_name, ic.column_position, ic.descend,
         ic.column_expression
  FROM user_indexes i
  JOIN user_ind_columns ic ON i.index_name = ic.index_name
  WHERE i.table_name IN (user_tables)
  ORDER BY i.index_name, ic.column_position
  ```

### Phase 2.2: Index Metadata Population
- [ ] Parse Oracle index metadata into `IndexMetadata` objects
- [ ] Handle composite indexes (group columns by index name)
- [ ] Identify functional indexes (column_expression IS NOT NULL)
- [ ] Store extracted indexes in `Everything.java` context

### Phase 2.3: Integration with Extraction Phase
- [ ] Add index extraction to `Main.java` extraction pipeline
- [ ] Integrate with existing database connection and schema handling
- [ ] Add index statistics to extraction reporting

---

## Phase 3: Index Migration Strategy Implementation ✅
**Goal**: Implement strategy pattern for different index types

### Phase 3.1: Strategy Interface
- [ ] Create `IndexMigrationStrategy.java` interface with methods:
  - `boolean supports(IndexMetadata index)`
  - `PostgreSQLIndexDDL convert(IndexMetadata index)`
  - `String getStrategyName()`

### Phase 3.2: Supported Index Strategies
- [ ] Create `BTreeIndexStrategy.java` - Handles standard B-tree indexes
- [ ] Create `UniqueIndexStrategy.java` - Handles unique indexes
- [ ] Create `CompositeIndexStrategy.java` - Handles multi-column indexes
- [ ] Create `PartialIndexStrategy.java` - Handles conditional indexes (WHERE clause)

### Phase 3.3: Unsupported Index Strategy
- [ ] Create `UnsupportedIndexStrategy.java` - Generates report entries for:
  - Bitmap indexes
  - Function-based indexes (complex expressions)
  - Reverse key indexes
  - Invisible indexes
  - Domain indexes

### Phase 3.4: Strategy Manager
- [ ] Create `IndexMigrationStrategyManager.java` to:
  - Register all strategies
  - Select appropriate strategy for each index
  - Maintain supported/unsupported index lists

---

## Phase 4: PostgreSQL Index Generation ✅
**Goal**: Generate PostgreSQL-compatible index DDL

### Phase 4.1: PostgreSQL Index DDL Model
- [ ] Create `PostgreSQLIndexDDL.java` class with:
  - `createIndexSQL` (generated DDL)
  - `indexName` (possibly modified for PostgreSQL)
  - `dependencies` (tables, schemas)
  - `executionPhase` (when to create the index)

### Phase 4.2: Index Export Implementation
- [ ] Create `ExportIndex.java` class following existing export patterns
- [ ] Generate PostgreSQL CREATE INDEX statements
- [ ] Handle name truncation (PostgreSQL 63-character limit)
- [ ] Apply schema qualification for cross-schema references

### Phase 4.3: DDL Generation Logic
- [ ] Implement PostgreSQL index syntax generation:
  ```sql
  CREATE [UNIQUE] INDEX [IF NOT EXISTS] index_name 
  ON [schema.]table_name (column1 [ASC|DESC], column2, ...)
  [WHERE condition]
  [TABLESPACE tablespace_name];
  ```

### Phase 4.4: Index File Organization
- [ ] Create `step6indexes/` directory for generated index files
- [ ] Generate one SQL file per schema
- [ ] Include index creation order (simple to complex)

---

## Phase 5: Unsupported Index Reporting ✅
**Goal**: Generate comprehensive reports for manual review

### Phase 5.1: Report Generator
- [ ] Create `UnsupportedIndexReporter.java` class
- [ ] Generate detailed reports including:
  - Index name, table, type, and reason for non-support
  - Original Oracle DDL
  - Suggested PostgreSQL alternatives
  - Performance impact analysis

### Phase 5.2: Report Format
- [ ] Create structured report format:
  ```
  UNSUPPORTED INDEX REPORT
  =======================
  
  Index: SCHEMA.IDX_BITMAP_STATUS
  Table: SCHEMA.ORDERS
  Type: BITMAP
  Reason: PostgreSQL uses dynamic bitmap scans, no stored bitmap indexes
  
  Original Oracle DDL:
  CREATE BITMAP INDEX idx_bitmap_status ON orders (status);
  
  Suggested PostgreSQL Alternative:
  CREATE INDEX idx_status ON orders (status);
  -- Note: PostgreSQL will use bitmap scans automatically when beneficial
  
  Performance Impact: May require query analysis for low-cardinality columns
  ```

### Phase 5.3: Report Integration
- [ ] Generate report files in `reports/` directory
- [ ] Include report generation in export phase
- [ ] Add unsupported index count to migration statistics

---

## Phase 6: Execution Integration ✅
**Goal**: Integrate index creation with PostgreSQL execution pipeline

### Phase 6.1: Execution Phase Definition
- [ ] Add `POST_TRANSFER_INDEXES` phase to `PostgresExecuter.java`
- [ ] Position after `POST_TRANSFER_CONSTRAINTS` and before final steps
- [ ] Implement index-specific execution logic

### Phase 6.2: Index Creation Timing
- [ ] Ensure indexes are created after data transfer for performance
- [ ] Handle dependencies (constraint indexes already created)
- [ ] Implement proper error handling and rollback

### Phase 6.3: Execution Reporting
- [ ] Add index creation progress tracking
- [ ] Report successful/failed index creations
- [ ] Include index statistics in final migration report

---

## Phase 7: REST API and Frontend Integration ✅
**Goal**: Integrate index migration with web interface

### Phase 7.1: REST Endpoint Integration
- [ ] Add index extraction to `POST /migration/extract` endpoint
- [ ] Add index generation to `POST /migration/export` endpoint
- [ ] Add index execution to `POST /migration/execute-post` endpoint
- [ ] Update `POST /migration/full` to include index processing

### Phase 7.2: Statistics Integration
- [ ] Add index counting to `PostgresStatsService.java`
- [ ] Include supported/unsupported index counts in statistics
- [ ] Add index progress tracking to job status

### Phase 7.3: Configuration UI
- [ ] Add index extraction checkbox to frontend (if applicable)
- [ ] Display index migration status in progress indicators
- [ ] Include unsupported index reports in results

---

## Phase 8: Testing and Validation ✅
**Goal**: Comprehensive testing of index migration functionality

### Phase 8.1: Unit Testing
- [ ] Create `IndexExtractorTest.java` with Oracle test cases
- [ ] Create `IndexMigrationStrategyTest.java` for strategy validation
- [ ] Create `ExportIndexTest.java` for DDL generation testing

### Phase 8.2: Integration Testing
- [ ] Test end-to-end index migration pipeline
- [ ] Validate PostgreSQL index creation and functionality
- [ ] Test unsupported index reporting accuracy

### Phase 8.3: Performance Testing
- [ ] Compare index performance Oracle vs PostgreSQL
- [ ] Validate index usage in PostgreSQL query plans
- [ ] Test index creation timing and performance impact

---

## Implementation Notes

### Database Compatibility
- **Oracle Version**: Tested with Oracle 11g, 12c, 19c
- **PostgreSQL Version**: Target PostgreSQL 12+ for modern index features

### Performance Considerations
- **Index Creation Timing**: Always after data transfer to avoid performance penalty
- **Memory Usage**: Stream index metadata to avoid memory issues with large schemas
- **Parallel Creation**: Consider parallel index creation for large schemas

### Error Handling Strategy
- **Graceful Degradation**: Continue migration even if some indexes fail
- **Detailed Logging**: Log all index creation attempts and failures
- **Rollback Support**: Provide index cleanup on migration failure

### Configuration Flags
```properties
# Index migration configuration
do.indexes=true
do.index.unique=true
do.index.composite=true
do.index.partial=true
do.index.reports=true
```

### File Structure
```
target/
├── step6indexes/
│   ├── schema1_indexes.sql
│   ├── schema2_indexes.sql
│   └── ...
├── reports/
│   ├── unsupported_indexes_report.txt
│   └── index_migration_summary.txt
└── ...
```

---

## Success Criteria

### Phase Completion Indicators
- [ ] All supported index types successfully extracted and converted
- [ ] Unsupported indexes properly identified and reported
- [ ] Generated PostgreSQL indexes create successfully
- [ ] Index functionality verified in PostgreSQL
- [ ] Performance impact documented and acceptable
- [ ] Full integration with existing migration pipeline

### Quality Metrics
- **Coverage**: 90%+ of Oracle indexes handled (converted or reported)
- **Accuracy**: Generated PostgreSQL indexes are functionally equivalent
- **Performance**: Index creation doesn't significantly impact migration time
- **Usability**: Clear reports for manual index implementation

---

## Future Enhancements (Post-Implementation)

### Advanced Index Support
- **Functional Indexes**: Enhanced expression transformation
- **Partial Bitmap Equivalent**: PostgreSQL optimization strategies
- **Index Recommendations**: PostgreSQL-specific index suggestions

### Performance Optimization
- **Index Analysis**: Query pattern analysis for index effectiveness
- **Automatic Optimization**: PostgreSQL-specific index recommendations
- **Index Maintenance**: VACUUM/ANALYZE recommendations

### Monitoring Integration
- **Index Usage Tracking**: Monitor index usage post-migration
- **Performance Metrics**: Compare pre/post migration performance
- **Optimization Suggestions**: Ongoing index optimization recommendations

---

## Implementation Status: Ready to Begin
This plan provides a comprehensive roadmap for implementing Oracle to PostgreSQL index migration. Each phase builds on previous work and can be implemented incrementally with testing at each stage.