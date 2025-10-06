# Refactoring Proposals for Proton Mail Export to Gmail

This document contains 10 refactoring proposals to improve code quality, maintainability, and adherence to best practices in this Java 21 Spring Boot project.

## 1. Extract Configuration Constants to Dedicated Configuration Classes

**Current Issue:**
Constants are scattered throughout the codebase (e.g., `BATCH_SIZE = 1_000` in `EmlEmailLoggingRunner`, `MAIL_SESSION` in multiple places).

**Proposal:**
Create a dedicated `ApplicationConstants` class or domain-specific constant classes to centralize magic numbers and configuration values.

**Benefits:**
- Single source of truth for constants
- Easier to maintain and modify
- Better testability
- Improved code readability

**Example:**
```java
@Component
public class EmailProcessingConstants {
    public static final int DEFAULT_BATCH_SIZE = 1_000;
    public static final int MAX_BATCH_SIZE = 10_000;
    public static final String MESSAGE_ID_HEADER = "Message-ID";
    public static final String FROM_HEADER = "From";
    public static final String DATE_HEADER = "Date";
}
```

**Affected Files:**
- `EmlEmailLoggingRunner.java`
- `GmailImapFetcher.java`
- `GmailImapClientSupport.java`

---

## 2. Replace StringBuffer with StringBuilder in ProtonMailExportApplication

**Current Issue:**
`ProtonMailExportApplication.main()` uses `StringBuffer` for building version information, which is thread-safe but unnecessary for a local variable.

**Location:** `ProtonMailExportApplication.java:30`

**Proposal:**
Replace `StringBuffer` with `StringBuilder` for better performance, or better yet, use `String.format()` or Java Text Blocks.

**Benefits:**
- Better performance (StringBuilder is faster)
- More modern and idiomatic Java
- Clearer intent

**Example:**
```java
// Option 1: StringBuilder
final var out = new StringBuilder();
out.append("GITHUB_RUN_NUMBER=");
out.append(System.getenv("GITHUB_RUN_NUMBER"));
out.append(", DOCKER_TAG_VERSION=");
out.append(System.getenv("DOCKER_TAG_VERSION"));

// Option 2: String.format (preferred)
String versionInfo = String.format("GITHUB_RUN_NUMBER=%s, DOCKER_TAG_VERSION=%s",
    System.getenv("GITHUB_RUN_NUMBER"),
    System.getenv("DOCKER_TAG_VERSION"));
log.info("Application started. Versions: {}", versionInfo);
```

**Affected Files:**
- `ProtonMailExportApplication.java`

---

## 3. Create Dedicated Service for Application Version Information

**Current Issue:**
Version information logging is embedded in the main method, making it hard to test and reuse.

**Proposal:**
Extract version information logic into a dedicated `ApplicationVersionService` or `VersionInfoProvider` component.

**Benefits:**
- Separation of concerns
- Testability
- Reusability (version info can be exposed via REST endpoint)
- Cleaner main method

**Example:**
```java
@Service
@Slf4j(topic = "ApplicationVersionService")
public class ApplicationVersionService {
    
    private final String githubRunNumber;
    private final String dockerTagVersion;
    
    public ApplicationVersionService(
            @Value("${GITHUB_RUN_NUMBER:unknown}") String githubRunNumber,
            @Value("${DOCKER_TAG_VERSION:unknown}") String dockerTagVersion) {
        this.githubRunNumber = githubRunNumber;
        this.dockerTagVersion = dockerTagVersion;
    }
    
    public String getVersionInfo() {
        return String.format("GITHUB_RUN_NUMBER=%s, DOCKER_TAG_VERSION=%s",
            githubRunNumber, dockerTagVersion);
    }
    
    public void logVersionInfo() {
        log.info("Application started. Versions: {}", getVersionInfo());
    }
}
```

**Affected Files:**
- `ProtonMailExportApplication.java` (simplified)
- New file: `ApplicationVersionService.java`

---

## 4. Extract URL Resolution Logic to Utility Class in OpenApiConfig

**Current Issue:**
`OpenApiConfig.resolveOpenApiDocumentUrl()` contains business logic that could be reused and tested independently.

**Proposal:**
Create a `UrlUtils` or `OpenApiUrlResolver` utility class for URL manipulation logic.

**Benefits:**
- Single Responsibility Principle
- Easier to unit test
- Reusable across different configurations

**Example:**
```java
@Component
public class OpenApiUrlResolver {
    
    public String resolveDocumentUrl(String baseUrl, String documentPath) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return documentPath;
        }
        return baseUrl.endsWith("/")
                ? baseUrl + documentPath.replaceFirst("^/", "")
                : baseUrl + (documentPath.startsWith("/") ? documentPath : "/" + documentPath);
    }
}
```

**Affected Files:**
- `OpenApiConfig.java`
- New file: `OpenApiUrlResolver.java`

---

## 5. Improve Exception Handling with Custom Exceptions

**Current Issue:**
Generic exceptions like `MessagingException`, `IOException`, and `Exception` are caught and logged without specific handling strategies.

**Proposal:**
Create domain-specific exception classes that provide better context and allow for more precise error handling.

**Benefits:**
- Better error messages
- Improved debugging
- Type-safe error handling
- Clear exception hierarchy

**Example:**
```java
public class EmailProcessingException extends RuntimeException {
    private final String messageId;
    private final String fileName;
    
    public EmailProcessingException(String message, String messageId, Throwable cause) {
        super(message, cause);
        this.messageId = messageId;
        this.fileName = null;
    }
    
    // Getters and additional constructors
}

public class ImapConnectionException extends EmailProcessingException {
    // Specific exception for IMAP issues
}

public class EmlFileParsingException extends EmailProcessingException {
    // Specific exception for EML file issues
}
```

**Affected Files:**
- `EmlEmailLoggingRunner.java`
- `GmailImapFetcher.java`
- `GmailImapClientSupport.java`
- New files: Exception hierarchy classes

---

## 6. Extract IMAP Connection Properties to Builder Pattern

**Current Issue:**
`GmailImapClientSupport.buildMailProperties()` manually constructs Properties with repetitive code.

**Proposal:**
Create a `MailPropertiesBuilder` class using the Builder pattern to construct mail properties in a more fluent and maintainable way.

**Benefits:**
- Fluent API
- Type safety
- Easier to add new properties
- Better testability

**Example:**
```java
@Component
public class MailPropertiesBuilder {
    
    public Properties buildImapProperties(GmailImapProperties props) {
        String protocol = props.sslEnabled() ? "imaps" : "imap";
        
        return new PropertiesBuilder()
                .withStoreProtocol(protocol)
                .withImapHost(props.host())
                .withImapPort(props.port())
                .withSslEnabled(props.sslEnabled())
                .withImapsHost(props.host())
                .withImapsPort(props.port())
                .withImapsSslEnabled(props.sslEnabled())
                .build();
    }
    
    private static class PropertiesBuilder {
        private final Properties properties = new Properties();
        
        PropertiesBuilder withStoreProtocol(String protocol) {
            properties.put("mail.store.protocol", protocol);
            return this;
        }
        
        // More fluent methods...
        
        Properties build() {
            return properties;
        }
    }
}
```

**Affected Files:**
- `GmailImapClientSupport.java`
- New file: `MailPropertiesBuilder.java`

---

## 7. Create Value Objects for Email Headers and Migration Status

**Current Issue:**
String-based message IDs and primitive types are passed around without type safety. The `MigrationStatus` interface is good, but could be enhanced.

**Proposal:**
Create proper value objects/record classes for domain entities like `MessageId`, `EmailAddress`, etc.

**Benefits:**
- Type safety
- Validation at construction time
- Immutability
- Self-documenting code

**Example:**
```java
public record MessageId(String value) {
    public MessageId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Message ID cannot be null or blank");
        }
        value = value.strip();
    }
    
    @Override
    public String toString() {
        return value;
    }
}

public record EmailAddress(String value) {
    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be null or blank");
        }
        // Add email validation if needed
        value = value.strip().toLowerCase();
    }
}

// Usage:
public MigrationEntity createFileMigration(MessageId messageId, OffsetDateTime messageDate) {
    return createMigration(messageId.value(), messageDate, builder -> builder.messageInFile(true));
}
```

**Affected Files:**
- `MigrationService.java`
- `ProblemService.java`
- `EmlEmailLoggingRunner.java`
- New files: Value object records

---

## 8. Implement Strategy Pattern for Problem Logging

**Current Issue:**
`ProblemService` has two similar methods (`logFileProblem` and `logRemoteProblem`) that differ only in their parameters.

**Proposal:**
Use Strategy pattern or Factory pattern to handle different types of problems more elegantly.

**Benefits:**
- Extensibility (easy to add new problem types)
- Reduced code duplication
- Better separation of concerns

**Example:**
```java
public interface ProblemType {
    ProblemEntity buildProblem(
            OffsetDateTime messageDate,
            String messageFrom,
            String diagnostics,
            ProblemRepository repository);
}

public class FileProblemType implements ProblemType {
    private final String messageFile;
    
    @Override
    public ProblemEntity buildProblem(...) {
        return ProblemEntity.builder()
                .messageDate(messageDate)
                .messageFile(messageFile)
                .messageIsFile(true)
                .messageIsRemote(false)
                .messageFrom(messageFrom)
                .messageDiagnostics(diagnostics)
                .build();
    }
}

// In ProblemService:
public ProblemEntity logProblem(ProblemType problemType, ...) {
    ProblemEntity saved = problemRepository.save(problemType.buildProblem(...));
    log.debug("Stored problem entry with id={}", saved.getId());
    return saved;
}
```

**Affected Files:**
- `ProblemService.java`
- New files: Problem type strategies

---

## 9. Extract Batch Processing Logic to Separate Service

**Current Issue:**
`EmlEmailLoggingRunner` contains batch processing logic mixed with command-line runner responsibilities.

**Proposal:**
Extract batch processing into a dedicated `EmlBatchProcessor` service.

**Benefits:**
- Single Responsibility Principle
- Better testability
- Reusability
- Clearer code structure

**Example:**
```java
@Service
@Slf4j(topic = "EmlBatchProcessor")
@RequiredArgsConstructor
public class EmlBatchProcessor {
    
    private static final int DEFAULT_BATCH_SIZE = 1_000;
    
    private final MigrationService migrationService;
    private final ProblemService problemService;
    private final EmlFileParser fileParser;
    
    public void processDirectory(Path directory, int batchSize) {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.eml")) {
            BatchAccumulator<Path> batch = new BatchAccumulator<>(batchSize);
            
            for (Path file : files) {
                if (isValidFile(file)) {
                    batch.add(file);
                    if (batch.isFull()) {
                        processBatch(batch.flush());
                    }
                }
            }
            
            if (!batch.isEmpty()) {
                processBatch(batch.flush());
            }
        } catch (IOException exception) {
            log.error("Failed to read EML files from directory: {}", directory, exception);
            throw new EmailProcessingException("Directory processing failed", exception);
        }
    }
    
    private void processBatch(List<Path> files) {
        files.forEach(this::processFile);
    }
    
    private boolean isValidFile(Path file) {
        return Files.isRegularFile(file) && Files.isReadable(file);
    }
}
```

**Affected Files:**
- `EmlEmailLoggingRunner.java` (simplified)
- New file: `EmlBatchProcessor.java`
- New file: `BatchAccumulator.java` (utility class)

---

## 10. Add Validation Annotations and Input Sanitization

**Current Issue:**
Input validation is done manually with if-statements (e.g., `StringUtils.hasText()`). No Bean Validation (JSR-380) is used.

**Proposal:**
Use Jakarta Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Valid`) and create custom validators for complex validation logic.

**Benefits:**
- Declarative validation
- Framework integration
- Consistent validation across the application
- Better error messages

**Example:**
```java
@ConfigurationProperties(prefix = "gmail.imap")
@Validated
public record GmailImapProperties(
        @NotBlank(message = "IMAP host cannot be blank")
        String host,
        
        @Min(1)
        @Max(65535)
        int port,
        
        boolean sslEnabled,
        
        @NotBlank(message = "Username cannot be blank")
        String username,
        
        @NotBlank(message = "Password cannot be blank")
        String password,
        
        @NotBlank(message = "Folder name cannot be blank")
        String folder,
        
        @Min(1)
        @Max(10000)
        int messageLimit
) {}

// In services:
@Service
@Validated
public class MigrationService {
    
    @Transactional
    public MigrationEntity createFileMigration(
            @NotBlank String messageId,
            @NotNull OffsetDateTime messageDate) {
        return createMigration(messageId, messageDate, builder -> builder.messageInFile(true));
    }
}
```

**Affected Files:**
- Configuration classes (`GmailImapProperties.java`, `EmlReaderProperties.java`)
- Service classes (`MigrationService.java`, `ProblemService.java`)
- Add dependency: `spring-boot-starter-validation` (if not present)

---

## Summary

These refactoring proposals focus on:

1. **Code organization**: Extracting constants, utilities, and business logic into appropriate classes
2. **Type safety**: Using value objects and records instead of primitives
3. **Design patterns**: Applying Strategy, Builder, and Factory patterns where appropriate
4. **Best practices**: Using modern Java features, proper exception handling, and validation
5. **Maintainability**: Improving testability, reducing duplication, and clarifying intent

## Implementation Priority

Recommended implementation order (from highest to lowest impact):

1. **High Priority**: #5 (Custom Exceptions), #7 (Value Objects), #10 (Validation)
2. **Medium Priority**: #9 (Batch Processing), #3 (Version Service), #1 (Constants)
3. **Low Priority**: #2 (StringBuilder), #4 (URL Resolver), #6 (Properties Builder), #8 (Strategy Pattern)

## Notes

- All refactorings should maintain backward compatibility where possible
- Each refactoring should be implemented incrementally with proper testing
- Code coverage should not decrease after refactoring
- Consider updating `pom.xml` version after implementing refactorings (current: 0.0.27-SNAPSHOT)
