## Programming Guidelines for Java

- Use **Java 21** syntax
- Within `modules/export-to-gmail`, prefer using `final var` for local variable declarations whenever possible.
- Use **Lombok**
- Prefer leveraging Lombok annotations (e.g., `@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Builder`) to remove boilerplate whenever
  feasible in Java code. When writing static utility classes, annotate them with `@UtilityClass` instead of hand-writing private constructors
  or manual static wrappers.
- Use **Spring-boot**
- For logging, use the Lombok annotation `@Slf4j(topic = "TOPIC_NAME")` instead of `LoggerFactory.getLogger`
- Use English when writing descriptions or documentation for the project
- When completing Codex-assigned tasks, bump the version number defined in `pom.xml`
- To run tests, change to `modules/export-to-gmail` and execute `mvn clean package`
- Persist information in the preferred **H2** database, keeping the database files on disk, and integrate with it through Spring Boot.
- When creating `@GetMapping` or `@PostMapping` methods, annotate them with relevant OpenAPI metadata that clearly explains for Custom GPT in ChatGPT what the endpoint does and when it should be invoked.

- Whenever `@GetMapping` or `@PostMapping` endpoints are added, removed, or modified, update the `clearSecurity(...)` calls in `OpenApiSecurityCustomizer` so they reference the current paths.
- Keep the `environment` section for the `proton-mail-export-to-gmail` service in `modules/export-to-gmail-docker/docker-compose.yml` aligned with the environment variables referenced in `modules/export-to-gmail/src/main/resources/application.yml`.
- Whenever you modify repository content, review and, if appropriate, refresh `CUSTOM_GPT.md` so it reflects the current behavior and wskazówki.
