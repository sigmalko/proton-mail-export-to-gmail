# Propozycje Refaktoringu dla Proton Mail Export to Gmail

Ten dokument zawiera 10 propozycji refaktoringu mających na celu poprawę jakości kodu, łatwości utrzymania i zgodności z najlepszymi praktykami w tym projekcie Java 21 Spring Boot.

## 1. Wydzielenie Stałych Konfiguracyjnych do Dedykowanych Klas

**Obecny Problem:**
Stałe są rozproszone po całej bazie kodu (np. `BATCH_SIZE = 1_000` w `EmlEmailLoggingRunner`, `MAIL_SESSION` w wielu miejscach).

**Propozycja:**
Utworzenie dedykowanej klasy `ApplicationConstants` lub klas stałych domenowych do centralizacji magicznych liczb i wartości konfiguracyjnych.

**Korzyści:**
- Pojedyncze źródło prawdy dla stałych
- Łatwiejsze utrzymanie i modyfikacja
- Lepsza testowalność
- Poprawiona czytelność kodu

**Przykład:**
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

**Dotknięte Pliki:**
- `EmlEmailLoggingRunner.java`
- `GmailImapFetcher.java`
- `GmailImapClientSupport.java`

---

## 2. Zamiana StringBuffer na StringBuilder w ProtonMailExportApplication

**Obecny Problem:**
`ProtonMailExportApplication.main()` używa `StringBuffer` do budowania informacji o wersji, co jest bezpieczne wątkowo, ale niepotrzebne dla zmiennej lokalnej.

**Lokalizacja:** `ProtonMailExportApplication.java:30`

**Propozycja:**
Zamienić `StringBuffer` na `StringBuilder` dla lepszej wydajności, lub jeszcze lepiej, użyć `String.format()` lub Java Text Blocks.

**Korzyści:**
- Lepsza wydajność (StringBuilder jest szybszy)
- Bardziej nowoczesna i idiomatyczna Java
- Jaśniejsza intencja

**Przykład:**
```java
// Opcja 1: StringBuilder
final var out = new StringBuilder();
out.append("GITHUB_RUN_NUMBER=");
out.append(System.getenv("GITHUB_RUN_NUMBER"));
out.append(", DOCKER_TAG_VERSION=");
out.append(System.getenv("DOCKER_TAG_VERSION"));

// Opcja 2: String.format (preferowana)
String versionInfo = String.format("GITHUB_RUN_NUMBER=%s, DOCKER_TAG_VERSION=%s",
    System.getenv("GITHUB_RUN_NUMBER"),
    System.getenv("DOCKER_TAG_VERSION"));
log.info("Application started. Versions: {}", versionInfo);
```

**Dotknięte Pliki:**
- `ProtonMailExportApplication.java`

---

## 3. Utworzenie Dedykowanego Serwisu dla Informacji o Wersji Aplikacji

**Obecny Problem:**
Logowanie informacji o wersji jest osadzone w metodzie main, co utrudnia testowanie i ponowne wykorzystanie.

**Propozycja:**
Wydzielenie logiki informacji o wersji do dedykowanego komponentu `ApplicationVersionService` lub `VersionInfoProvider`.

**Korzyści:**
- Separacja odpowiedzialności
- Testowalność
- Możliwość ponownego użycia (informacja o wersji może być udostępniona przez endpoint REST)
- Czystsza metoda main

**Przykład:**
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

**Dotknięte Pliki:**
- `ProtonMailExportApplication.java` (uproszczona)
- Nowy plik: `ApplicationVersionService.java`

---

## 4. Wydzielenie Logiki Rozwiązywania URL do Klasy Narzędziowej w OpenApiConfig

**Obecny Problem:**
`OpenApiConfig.resolveOpenApiDocumentUrl()` zawiera logikę biznesową, która mogłaby być ponownie użyta i testowana niezależnie.

**Propozycja:**
Utworzenie klasy narzędziowej `UrlUtils` lub `OpenApiUrlResolver` dla logiki manipulacji URL.

**Korzyści:**
- Zasada pojedynczej odpowiedzialności
- Łatwiejsze testy jednostkowe
- Możliwość ponownego użycia w różnych konfiguracjach

**Przykład:**
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

**Dotknięte Pliki:**
- `OpenApiConfig.java`
- Nowy plik: `OpenApiUrlResolver.java`

---

## 5. Poprawa Obsługi Wyjątków przez Użycie Własnych Wyjątków

**Obecny Problem:**
Generyczne wyjątki jak `MessagingException`, `IOException` i `Exception` są przechwytywane i logowane bez specyficznych strategii obsługi.

**Propozycja:**
Utworzenie klas wyjątków domenowych, które dostarczają lepszy kontekst i pozwalają na bardziej precyzyjną obsługę błędów.

**Korzyści:**
- Lepsze komunikaty błędów
- Ulepszone debugowanie
- Typowo bezpieczna obsługa błędów
- Czytelna hierarchia wyjątków

**Przykład:**
```java
public class EmailProcessingException extends RuntimeException {
    private final String messageId;
    private final String fileName;
    
    public EmailProcessingException(String message, String messageId, Throwable cause) {
        super(message, cause);
        this.messageId = messageId;
        this.fileName = null;
    }
    
    // Gettery i dodatkowe konstruktory
}

public class ImapConnectionException extends EmailProcessingException {
    // Specyficzny wyjątek dla problemów z IMAP
}

public class EmlFileParsingException extends EmailProcessingException {
    // Specyficzny wyjątek dla problemów z plikami EML
}
```

**Dotknięte Pliki:**
- `EmlEmailLoggingRunner.java`
- `GmailImapFetcher.java`
- `GmailImapClientSupport.java`
- Nowe pliki: Hierarchia klas wyjątków

---

## 6. Wydzielenie Właściwości Połączenia IMAP do Wzorca Builder

**Obecny Problem:**
`GmailImapClientSupport.buildMailProperties()` ręcznie konstruuje Properties z powtarzalnym kodem.

**Propozycja:**
Utworzenie klasy `MailPropertiesBuilder` używającej wzorca Builder do konstruowania właściwości mail w bardziej płynny i łatwiejszy do utrzymania sposób.

**Korzyści:**
- Płynne API
- Bezpieczeństwo typów
- Łatwiejsze dodawanie nowych właściwości
- Lepsza testowalność

**Przykład:**
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
        
        // Więcej płynnych metod...
        
        Properties build() {
            return properties;
        }
    }
}
```

**Dotknięte Pliki:**
- `GmailImapClientSupport.java`
- Nowy plik: `MailPropertiesBuilder.java`

---

## 7. Utworzenie Obiektów Wartości dla Nagłówków Email i Statusu Migracji

**Obecny Problem:**
Identyfikatory wiadomości oparte na String i typy prymitywne są przekazywane bez bezpieczeństwa typów. Interfejs `MigrationStatus` jest dobry, ale mógłby być ulepszony.

**Propozycja:**
Utworzenie właściwych obiektów wartości/klas record dla encji domenowych jak `MessageId`, `EmailAddress`, itp.

**Korzyści:**
- Bezpieczeństwo typów
- Walidacja w momencie konstrukcji
- Niezmienność
- Samo-dokumentujący się kod

**Przykład:**
```java
public record MessageId(String value) {
    public MessageId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Message ID nie może być null lub pusty");
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
            throw new IllegalArgumentException("Adres email nie może być null lub pusty");
        }
        // Dodaj walidację email jeśli potrzeba
        value = value.strip().toLowerCase();
    }
}

// Użycie:
public MigrationEntity createFileMigration(MessageId messageId, OffsetDateTime messageDate) {
    return createMigration(messageId.value(), messageDate, builder -> builder.messageInFile(true));
}
```

**Dotknięte Pliki:**
- `MigrationService.java`
- `ProblemService.java`
- `EmlEmailLoggingRunner.java`
- Nowe pliki: Rekordy obiektów wartości

---

## 8. Implementacja Wzorca Strategy dla Logowania Problemów

**Obecny Problem:**
`ProblemService` ma dwie podobne metody (`logFileProblem` i `logRemoteProblem`), które różnią się tylko parametrami.

**Propozycja:**
Użycie wzorca Strategy lub Factory do bardziej eleganckiej obsługi różnych typów problemów.

**Korzyści:**
- Rozszerzalność (łatwe dodawanie nowych typów problemów)
- Zmniejszona duplikacja kodu
- Lepsza separacja odpowiedzialności

**Przykład:**
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

// W ProblemService:
public ProblemEntity logProblem(ProblemType problemType, ...) {
    ProblemEntity saved = problemRepository.save(problemType.buildProblem(...));
    log.debug("Zapisano wpis problemu z id={}", saved.getId());
    return saved;
}
```

**Dotknięte Pliki:**
- `ProblemService.java`
- Nowe pliki: Strategie typów problemów

---

## 9. Wydzielenie Logiki Przetwarzania Wsadowego do Osobnego Serwisu

**Obecny Problem:**
`EmlEmailLoggingRunner` zawiera logikę przetwarzania wsadowego zmieszaną z odpowiedzialnościami runnera wiersza poleceń.

**Propozycja:**
Wydzielenie przetwarzania wsadowego do dedykowanego serwisu `EmlBatchProcessor`.

**Korzyści:**
- Zasada pojedynczej odpowiedzialności
- Lepsza testowalność
- Możliwość ponownego użycia
- Jaśniejsza struktura kodu

**Przykład:**
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
            log.error("Nie udało się odczytać plików EML z katalogu: {}", directory, exception);
            throw new EmailProcessingException("Przetwarzanie katalogu nie powiodło się", exception);
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

**Dotknięte Pliki:**
- `EmlEmailLoggingRunner.java` (uproszczona)
- Nowy plik: `EmlBatchProcessor.java`
- Nowy plik: `BatchAccumulator.java` (klasa narzędziowa)

---

## 10. Dodanie Adnotacji Walidacyjnych i Sanityzacji Wejścia

**Obecny Problem:**
Walidacja wejścia jest wykonywana ręcznie za pomocą instrukcji if (np. `StringUtils.hasText()`). Nie używa się Bean Validation (JSR-380).

**Propozycja:**
Użycie adnotacji Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Valid`) i utworzenie własnych walidatorów dla złożonej logiki walidacji.

**Korzyści:**
- Deklaratywna walidacja
- Integracja z frameworkiem
- Spójna walidacja w całej aplikacji
- Lepsze komunikaty błędów

**Przykład:**
```java
@ConfigurationProperties(prefix = "gmail.imap")
@Validated
public record GmailImapProperties(
        @NotBlank(message = "Host IMAP nie może być pusty")
        String host,
        
        @Min(1)
        @Max(65535)
        int port,
        
        boolean sslEnabled,
        
        @NotBlank(message = "Nazwa użytkownika nie może być pusta")
        String username,
        
        @NotBlank(message = "Hasło nie może być puste")
        String password,
        
        @NotBlank(message = "Nazwa folderu nie może być pusta")
        String folder,
        
        @Min(1)
        @Max(10000)
        int messageLimit
) {}

// W serwisach:
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

**Dotknięte Pliki:**
- Klasy konfiguracyjne (`GmailImapProperties.java`, `EmlReaderProperties.java`)
- Klasy serwisowe (`MigrationService.java`, `ProblemService.java`)
- Dodanie zależności: `spring-boot-starter-validation` (jeśli nie jest obecna)

---

## Podsumowanie

Te propozycje refaktoringu koncentrują się na:

1. **Organizacji kodu**: Wydzielanie stałych, narzędzi i logiki biznesowej do odpowiednich klas
2. **Bezpieczeństwie typów**: Używanie obiektów wartości i rekordów zamiast typów prymitywnych
3. **Wzorcach projektowych**: Stosowanie wzorców Strategy, Builder i Factory tam, gdzie to odpowiednie
4. **Najlepszych praktykach**: Używanie nowoczesnych funkcji Javy, właściwej obsługi wyjątków i walidacji
5. **Łatwości utrzymania**: Poprawa testowalności, redukcja duplikacji i wyjaśnienie intencji

## Priorytet Implementacji

Zalecana kolejność implementacji (od najwyższego do najniższego wpływu):

1. **Wysoki Priorytet**: #5 (Własne Wyjątki), #7 (Obiekty Wartości), #10 (Walidacja)
2. **Średni Priorytet**: #9 (Przetwarzanie Wsadowe), #3 (Serwis Wersji), #1 (Stałe)
3. **Niski Priorytet**: #2 (StringBuilder), #4 (Resolver URL), #6 (Builder Właściwości), #8 (Wzorzec Strategy)

## Uwagi

- Wszystkie refaktoringi powinny zachować kompatybilność wsteczną gdzie to możliwe
- Każdy refaktoring powinien być implementowany przyrostowo z właściwymi testami
- Pokrycie kodu nie powinno się zmniejszyć po refaktoringu
- Rozważ aktualizację wersji w `pom.xml` po implementacji refaktoringów (obecna: 0.0.27-SNAPSHOT)
