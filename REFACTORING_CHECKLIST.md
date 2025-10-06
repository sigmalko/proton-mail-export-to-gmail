# Refactoring Implementation Checklist

Use this checklist to track progress as you implement the refactoring proposals. Check off items as you complete them.

## Phase 1: High Priority - Foundation (Week 1-2)

### Refactoring #5: Custom Exceptions

- [ ] **Step 1.1**: Create base exception class
  - [ ] Create `EmailProcessingException.java`
  - [ ] Add constructor with message, cause, and context fields
  - [ ] Add getters for context information
  
- [ ] **Step 1.2**: Create specific exception classes
  - [ ] Create `ImapConnectionException.java`
  - [ ] Create `ImapAuthenticationException.java`
  - [ ] Create `EmlFileParsingException.java`
  - [ ] Create `MissingHeaderException.java`
  - [ ] Create `InvalidFormatException.java`
  
- [ ] **Step 1.3**: Replace generic exception handling
  - [ ] Update `GmailImapClientSupport.java`
  - [ ] Update `EmlEmailLoggingRunner.java`
  - [ ] Update `GmailImapFetcher.java`
  - [ ] Update other files with generic catches
  
- [ ] **Step 1.4**: Test and verify
  - [ ] Write unit tests for each exception
  - [ ] Run existing tests to ensure no regressions
  - [ ] Update exception handling documentation

**Status**: ⏳ Not Started | 🚧 In Progress | ✅ Complete

---

### Refactoring #7: Value Objects (Records)

- [ ] **Step 2.1**: Create MessageId value object
  - [ ] Create `MessageId.java` record
  - [ ] Add validation in compact constructor
  - [ ] Add tests for MessageId
  
- [ ] **Step 2.2**: Create EmailAddress value object
  - [ ] Create `EmailAddress.java` record
  - [ ] Add validation in compact constructor
  - [ ] Add email format validation (optional)
  - [ ] Add tests for EmailAddress
  
- [ ] **Step 2.3**: Update service signatures
  - [ ] Update `MigrationService.java` methods
  - [ ] Update `ProblemService.java` methods
  - [ ] Update `EmlEmailLoggingRunner.java`
  
- [ ] **Step 2.4**: Update repositories
  - [ ] Review `MigrationRepository.java` projections
  - [ ] Review `ProblemRepository.java` projections
  - [ ] Update queries if needed
  
- [ ] **Step 2.5**: Test and verify
  - [ ] Write unit tests for value objects
  - [ ] Update integration tests
  - [ ] Verify database operations still work
  - [ ] Run full test suite

**Status**: ⏳ Not Started | 🚧 In Progress | ✅ Complete

---

### Refactoring #10: Validation Annotations

- [ ] **Step 3.1**: Add dependency
  - [ ] Add `spring-boot-starter-validation` to `pom.xml` (if not present)
  - [ ] Run `mvn clean install` to verify
  
- [ ] **Step 3.2**: Annotate configuration classes
  - [ ] Add `@Validated` to `GmailImapProperties`
  - [ ] Add `@NotBlank`, `@Min`, `@Max` annotations
  - [ ] Add `@Validated` to `EmlReaderProperties`
  - [ ] Add validation annotations
  
- [ ] **Step 3.3**: Annotate service methods
  - [ ] Add `@Validated` to `MigrationService`
  - [ ] Add `@NotNull`, `@NotBlank` to method parameters
  - [ ] Add `@Validated` to `ProblemService`
  - [ ] Add parameter validation annotations
  
- [ ] **Step 3.4**: Test validation
  - [ ] Write tests for invalid configurations
  - [ ] Write tests for invalid service parameters
  - [ ] Verify error messages are clear
  - [ ] Test with actual invalid data
  
- [ ] **Step 3.5**: Integration
  - [ ] Test application startup with invalid config
  - [ ] Verify validation exceptions are properly handled
  - [ ] Update error handling if needed

**Status**: ⏳ Not Started | 🚧 In Progress | ✅ Complete

---

## Phase 2: Medium Priority - Structure (Week 3-4)

### Refactoring #9: Batch Processing Service

- [ ] **Step 4.1**: Create utility classes
  - [ ] Create `BatchAccumulator.java` utility
  - [ ] Add tests for BatchAccumulator
  
- [ ] **Step 4.2**: Create parser service
  - [ ] Create `EmlFileParser.java`
  - [ ] Move parsing logic from EmlEmailLoggingRunner
  - [ ] Add tests for parser
  
- [ ] **Step 4.3**: Create batch processor
  - [ ] Create `EmlBatchProcessor.java`
  - [ ] Move batch processing logic
  - [ ] Inject dependencies (MigrationService, ProblemService)
  - [ ] Add tests for processor
  
- [ ] **Step 4.4**: Simplify runner
  - [ ] Update `EmlEmailLoggingRunner.java`
  - [ ] Delegate to EmlBatchProcessor
  - [ ] Remove old processing code
  - [ ] Update tests
  
- [ ] **Step 4.5**: Integration testing
  - [ ] Test end-to-end with real EML files
  - [ ] Verify batch processing still works
  - [ ] Check performance (should be same or better)

**Status**: ⏳ Not Started | 🚧 In Progress | ✅ Complete

---

### Refactoring #3: Version Service

- [ ] **Step 5.1**: Create version service
  - [ ] Create `ApplicationVersionService.java`
  - [ ] Inject environment variables via `@Value`
  - [ ] Add `getVersionInfo()` method
  - [ ] Add `logVersionInfo()` method
  
- [ ] **Step 5.2**: Update main application
  - [ ] Inject `ApplicationVersionService` in main
  - [ ] Replace inline version logging
  - [ ] Simplify main method
  
- [ ] **Step 5.3**: Add REST endpoint (optional)
  - [ ] Create `VersionController.java`
  - [ ] Add GET `/api/version` endpoint
  - [ ] Return version info as JSON
  
- [ ] **Step 5.4**: Test
  - [ ] Write unit tests for service
  - [ ] Test with different environment variables
  - [ ] Test REST endpoint if created

**Status**: ⏳ Not Started | 🚧 In Progress | ✅ Complete

---

### Refactoring #1: Configuration Constants

- [ ] **Step 6.1**: Create constants classes
  - [ ] Create `EmailProcessingConstants.java`
  - [ ] Move BATCH_SIZE constant
  - [ ] Move other batch-related constants
  
- [ ] **Step 6.2**: Create header constants
  - [ ] Add MESSAGE_ID_HEADER constant
  - [ ] Add FROM_HEADER constant
  - [ ] Add DATE_HEADER constant
  
- [ ] **Step 6.3**: Update references
  - [ ] Update `EmlEmailLoggingRunner.java`
  - [ ] Update `GmailImapFetcher.java`
  - [ ] Update other files using magic numbers
  
- [ ] **Step 6.4**: Review and verify
  - [ ] Search for remaining magic numbers
  - [ ] Run tests to ensure no regressions
  - [ ] Update documentation

**Status**: ⏳ Not Started | 🚧 In Progress | ✅ Complete

---

## Phase 3: Low Priority - Polish (Week 5)

### Refactoring #2: StringBuilder

- [ ] **Step 7.1**: Update ProtonMailExportApplication
  - [ ] Replace `StringBuffer` with `StringBuilder` or `String.format()`
  - [ ] Test application startup
  - [ ] Verify logging output is unchanged

**Status**: ⏳ Not Started | 🚧 In Progress | ✅ Complete

---

### Refactoring #4: URL Resolver

- [ ] **Step 8.1**: Create resolver class
  - [ ] Create `OpenApiUrlResolver.java`
  - [ ] Move URL resolution logic
  - [ ] Add tests
  
- [ ] **Step 8.2**: Update OpenApiConfig
  - [ ] Inject OpenApiUrlResolver
  - [ ] Use resolver in customOpenAPI()
  - [ ] Remove old method
  
- [ ] **Step 8.3**: Test
  - [ ] Test with different URL configurations
  - [ ] Verify OpenAPI spec is correct

**Status**: ⏳ Not Started | 🚧 In Progress | ✅ Complete

---

### Refactoring #6: Properties Builder

- [ ] **Step 9.1**: Create builder class
  - [ ] Create `MailPropertiesBuilder.java`
  - [ ] Implement fluent builder methods
  - [ ] Add tests
  
- [ ] **Step 9.2**: Update client support
  - [ ] Inject MailPropertiesBuilder in GmailImapClientSupport
  - [ ] Use builder in buildMailProperties()
  - [ ] Remove old implementation
  
- [ ] **Step 9.3**: Test
  - [ ] Test with different configurations
  - [ ] Verify IMAP connections still work

**Status**: ⏳ Not Started | 🚧 In Progress | ✅ Complete

---

### Refactoring #8: Strategy Pattern

- [ ] **Step 10.1**: Create interface
  - [ ] Create `ProblemType.java` interface
  - [ ] Define buildProblem() method
  
- [ ] **Step 10.2**: Create implementations
  - [ ] Create `FileProblemType.java`
  - [ ] Create `RemoteProblemType.java`
  - [ ] Add tests for each type
  
- [ ] **Step 10.3**: Update service
  - [ ] Update `ProblemService.java`
  - [ ] Add logProblem(ProblemType, ...) method
  - [ ] Deprecate old methods (optional)
  - [ ] Update callers to use new method
  
- [ ] **Step 10.4**: Test
  - [ ] Test with different problem types
  - [ ] Verify database operations
  - [ ] Run integration tests

**Status**: ⏳ Not Started | 🚧 In Progress | ✅ Complete

---

## Final Steps

- [ ] **Code Review**: Review all changes
- [ ] **Documentation**: Update JavaDocs and README
- [ ] **Testing**: Run full test suite
  - [ ] Unit tests: `mvn test`
  - [ ] Integration tests: `mvn verify`
  - [ ] Manual testing with sample data
- [ ] **Performance**: Verify no performance regression
- [ ] **Version**: Update to 0.0.28 (already done) or next version
- [ ] **Cleanup**: Remove deprecated code if any
- [ ] **Commit**: Final commit with all changes

---

## Progress Summary

| Phase | Refactorings | Status | Completion |
|-------|--------------|--------|------------|
| Phase 1 | #5, #7, #10 | ⏳ | 0/3 |
| Phase 2 | #9, #3, #1 | ⏳ | 0/3 |
| Phase 3 | #2, #4, #6, #8 | ⏳ | 0/4 |
| **Total** | **10** | **⏳** | **0/10** |

---

## Notes and Observations

### Blockers
_(List any blockers encountered during implementation)_

- None yet

### Decisions Made
_(Document key decisions made during refactoring)_

- None yet

### Lessons Learned
_(Capture insights for future refactorings)_

- None yet

### Technical Debt
_(Track new technical debt introduced, if any)_

- None yet

---

## Code Review Checklist

Before marking the refactoring as complete, ensure:

- [ ] All unit tests pass
- [ ] Integration tests pass
- [ ] Code coverage maintained or improved
- [ ] No new compiler warnings
- [ ] No new static analysis warnings
- [ ] JavaDocs updated where needed
- [ ] README.md updated if needed
- [ ] CHANGELOG.md updated (if exists)
- [ ] Code reviewed by at least one other developer
- [ ] Performance benchmarks pass (if applicable)

---

## Useful Commands

```bash
# Navigate to project
cd /home/runner/work/proton-mail-export-to-gmail/proton-mail-export-to-gmail/modules/export-to-gmail

# Run tests
mvn clean test

# Run with coverage
mvn clean test jacoco:report

# Build project
mvn clean package

# Run application
mvn spring-boot:run

# Check for updates
mvn versions:display-dependency-updates

# Format code (if formatter configured)
mvn formatter:format
```

---

**Last Updated**: 2025-10-06  
**Version**: 0.0.28-SNAPSHOT  
**Status**: Ready for Implementation
