# Configuration Documentation

### Module Configuration

The modules `fhir`, `loinc`, and `snomed` use microprofile-config for configuration management. This
allows for flexible
configuration through
various sources such as system properties, environment variables, and configuration files.
As implementation smallrye-config is used.

SmallRye Config follows this priority order (highest to lowest):

1. System properties (-Dfhirterms.start=true)
2. Environment variables (FHIRTERMS_START=true)
3. application.yaml (in src/main/resources)
4. microprofile-config.properties (in META-INF/)

## Core Configuration

The core module provides centralized configuration for the terminology server, including server
capabilities, security
settings, paging, and operation controls.
It is configured using spring boot configuration properties, which can be set through
application.properties,
application.yaml, or environment variables.

### Basic Core Configuration

| Property                     | Type   | Default    | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|------------------------------|--------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `core.baseWorkingDir`        | string | *required* | Base directory for indexes and temporary files (must exist and be writable)                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| `core.systemLanguage`        | string | `de-DE`    | BCP 47 language code for server-wide default language                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `core.resourceRetrievalMode` | Enum   | `STRIPPED` | Resource retrieval mode for search operations. When set to 'STRIPPED', the server will return only the resource metadata (without the actual content) in search results, which can improve performance and reduce bandwidth usage. Currently, CodeSystem.concept, ValueSet.compose/ValueSet.expansion and ConceptMap.group is stripped. Clients can then retrieve the full resource content using a separate read operation if needed. Set to 'FULL' to include the full resource content in search results. |   

### Security Configuration

#### CORS Settings

| Property                              | Type    | Default              | Description                                                  |
|---------------------------------------|---------|----------------------|--------------------------------------------------------------|
| `core.security.cors.allowedOrigins`   | string  | ``                   | List of allowed CORS origins (URLs), comma-separated         |
| `core.security.cors.allowedMethods`   | string  | `GET, POST, OPTIONS` | List of allowed HTTP methods  , comma-separated              |
| `core.security.cors.allowedHeaders`   | string  | `*`                  | List of allowed request headers , comma-separated            |
| `core.security.cors.allowCredentials` | boolean | `true`               | Whether credentials are allowed in CORS requests             |
| `core.security.cors.exposedHeaders`   | string  | ``                   | List of headers to expose in CORS responses, comma-separated |

#### Rate Limiting

| Property                                    | Type    | Default | Description                            |
|---------------------------------------------|---------|---------|----------------------------------------|
| `core.security.rateLimit.enabled`           | boolean | `false` | Enable/disable rate limiting           |
| `core.security.rateLimit.requestsPerMinute` | integer | `100`   | Maximum requests per minute per client |

#### Request Security

| Property                                    | Type    | Default    | Description                                                                                     |
|---------------------------------------------|---------|------------|-------------------------------------------------------------------------------------------------|
| `core.security.request.maxParameterLength`  | integer | `1000`     | Maximum length for individual query parameters. Set to 0 to disable parameter length validation |
| `core.security.request.maxRequestSizeBytes` | integer | `10485760` | Maximum request body size in bytes (10MB). Set to 0 to disable request size validation          |

### Paging Configuration

| Property                         | Type    | Default | Description                                     |
|----------------------------------|---------|---------|-------------------------------------------------|
| `core.paging.pagingProviderSize` | integer | `1000`  | Size of internal paging provider buffer         |
| `core.paging.defaultPageSize`    | integer | `20`    | Default page size when not specified in request |
| `core.paging.maximumPageSize`    | integer | `100`   | Maximum allowed page size in a single request   |

### Capabilities Configuration

These properties control the CapabilityStatement metadata exposed by the server.

| Property                                    | Type    | Default                                                | Description                                                  |
|---------------------------------------------|---------|--------------------------------------------------------|--------------------------------------------------------------|
| `core.capabilities.name`                    | string  | `TERMzillaTerminologyServices`                         | Server name in CapabilityStatement                           |
| `core.capabilities.title`                   | string  | `TERMzilla Terminology Services`                       | Human-readable server title                                  |
| `core.capabilities.description`             | string  | `Terminology Service for FHIR Terminologies`           | Server description                                           |
| `core.capabilities.publisher`               | string  | `Fraunhofer FOKUS`                                     | Publisher name                                               |
| `core.capabilities.status`                  | enum    | `active`                                               | Publication status (`active`, `draft`, `retired`, `unknown`) |
| `core.capabilities.experimental`            | boolean | `true`                                                 | Whether this is an experimental server instance              |
| `core.capabilities.softwareName`            | string  | `TERMzilla`                                            | Name of implementing software                                |
| `core.capabilities.softwareVersion`         | string  | `0.1.0`                                                | Software version                                             |
| `core.capabilities.softwareReleaseDate`     | string  | `2023-10-01`                                           | Release date (ISO 8601 format: YYYY-MM-DD)                   |
| `core.capabilities.restSecurityDescription` | string  | `No security mechanisms are enabled for this instance` | Security description                                         |

### Operation Control

Control which operations are enabled for each resource type. Empty parameter lists mean no
restrictions (all parameters
allowed).

#### System-Level Operations

| Property                                         | Type    | Default | Description                                                                                                                                                             |
|--------------------------------------------------|---------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `core.operations.systemLevel.batch.enabled`      | boolean | `true`  | Enables registration of the native system-level batch transaction provider (`@Transaction`) used for batch processing.                                                  |
| `core.operations.systemLevel.batch.maxBatchSize` | integer | `1000`  | Maximum number of entries allowed in an incoming batch bundle (`Bundle.entry.size`). Requests above this limit are rejected. Must be > 0 if batch processing is enabled |

#### CodeSystem Operations

| Property                                                | Type    | Default | Description                                                               |
|---------------------------------------------------------|---------|---------|---------------------------------------------------------------------------|
| `core.operations.codesystem.read.enabled`               | boolean | `true`  | Enable READ operation                                                     |
| `core.operations.codesystem.search.enabled`             | boolean | `true`  | Enable SEARCH operation                                                   |
| `core.operations.codesystem.lookup.enabled`             | boolean | `true`  | Enable $lookup operation                                                  |
| `core.operations.codesystem.validateCode.enabled`       | boolean | `true`  | Enable $validate-code operation                                           |
| `core.operations.codesystem.findMatches.enabled`        | boolean | `true`  | Enable $find-matches operation                                            |
| `core.operations.codesystem.subsumes.enabled`           | boolean | `true`  | Enable $subsumes operation                                                |
| `core.operations.codesystem.search.allowedParams`       | string  | ``      | Allowed search parameters, comma-separated (empty = all allowed)          |
| `core.operations.codesystem.lookup.allowedParams`       | string  | ``      | Allowed $lookup parameters, comma-separated  (empty = all allowed)        |
| `core.operations.codesystem.validateCode.allowedParams` | string  | ``      | Allowed $validate-code parameters, comma-separated  (empty = all allowed) |
| `core.operations.codesystem.findMatches.allowedParams`  | string  | ``      | Allowed $find-matches parameters, comma-separated  (empty = all allowed)  |
| `core.operations.codesystem.subsumes.allowedParams`     | string  | ``      | Allowed $subsumes parameters, comma-separated  (empty = all allowed)      |

#### ValueSet Operations

| Property                                              | Type    | Default | Description                                                               |
|-------------------------------------------------------|---------|---------|---------------------------------------------------------------------------|
| `core.operations.valueset.read.enabled`               | boolean | `true`  | Enable READ operation                                                     |
| `core.operations.valueset.search.enabled`             | boolean | `true`  | Enable SEARCH operation                                                   |
| `core.operations.valueset.expand.enabled`             | boolean | `true`  | Enable $expand operation                                                  |
| `core.operations.valueset.validateCode.enabled`       | boolean | `true`  | Enable $validate-code operation                                           |
| `core.operations.valueset.search.allowedParams`       | string  | ``      | Allowed search parameters, comma-separated  (empty = all allowed)         |
| `core.operations.valueset.expand.allowedParams`       | string  | ``      | Allowed $expand parameters, comma-separated  (empty = all allowed)        |
| `core.operations.valueset.validateCode.allowedParams` | string  | ``      | Allowed $validate-code parameters, comma-separated  (empty = all allowed) |

#### ConceptMap Operations

| Property                                             | Type    | Default | Description                                                           |
|------------------------------------------------------|---------|---------|-----------------------------------------------------------------------|
| `core.operations.conceptmap.read.enabled`            | boolean | `true`  | Enable READ operation                                                 |
| `core.operations.conceptmap.search.enabled`          | boolean | `true`  | Enable SEARCH operation                                               |
| `core.operations.conceptmap.translate.enabled`       | boolean | `true`  | Enable $translate operation                                           |
| `core.operations.conceptmap.closure.enabled`         | boolean | `true`  | Enable $closure operation                                             |
| `core.operations.conceptmap.search.allowedParams`    | string  | ``      | Allowed search parameters, comma-separated  (empty = all allowed)     |
| `core.operations.conceptmap.translate.allowedParams` | string  | ``      | Allowed $translate parameters, comma-separated  (empty = all allowed) |
| `core.operations.conceptmap.closure.allowedParams`   | string  | ``      | Allowed $closure parameters, comma-separated  (empty = all allowed)   |

### Core Configuration Examples

#### Configuration example (application.yaml)

```yaml
# ========================================
# Server Configuration
# ========================================

server:
  port: 8080
  servlet:
    context-path: /

# ========================================
# Core Configuration
# ========================================

core:
  baseWorkingDir: /Users/bha/Projects/gematik-FHIR/repo/termzilla/TMP
  systemLanguage: de-DE

  # ========================================
  # Security Configuration (TODO)
  # ========================================
  security:
    cors:
      allowedOrigins: "http://localhost:3000,https://fragebogen.apps.osc.fokus.fraunhofer.de"
      allowedMethods: "GET,POST,OPTIONS"
      allowedHeaders: "*"
      allowCredentials: true
      exposedHeaders: "Location,Content-Location"
    rateLimit:
      enabled: true
      requestsPerMinute: 100
    request:
      maxParameterLength: 1000
      maxRequestSizeBytes: 10485760

  paging:
    pagingProviderSize: 1000
    defaultPageSize: 20
    maximumPageSize: 100

  # ========================================
  # Capabilities Configuration
  # ========================================
  capabilities:
    name: "TERMzillaTerminologyServices"
    title: "TERMzilla Terminology Services"
    description: "Terminology Service for FHIR Terminologies"
    publisher: "Fraunhofer FOKUS"
    status: "active"
    experimental: true
    softwareName: "TERMzilla"
    softwareVersion: "0.1.0"
    softwareReleaseDate: "2023-10-01"
    restSecurityDescription: "No security mechanisms are enabled for this instance"
  # ========================================
  # Operations Configuration
  # ========================================
  operations:
    systemLevel:
      batch:
        enabled: true
        maxBatchSize: 1000

    codesystem:
      read:
        enabled: true
      search:
        enabled: true
        allowedParams: "_content,_id,_lastUpdated,_profile,_security,_source,url,version,name,title,status,publisher,description,code,system,content-mode"
      lookup:
        enabled: true
        allowedParams: "code,system,version,coding,date,displayLanguage,property"
      validateCode:
        enabled: true
        allowedParams: "url,codeSystem,code,version,display,coding,codeableConcept,date,abstract,displayLanguage"
      findMatches:
        enabled: true
        allowedParams: "system,version,property,exact,compositional"
      subsumes:
        enabled: false
        allowedParams: "codeA,codeB,system,version,codingA,codingB"

    valueset:
      read:
        enabled: true
      search:
        enabled: true
        allowedParams: "_content,_id,_lastUpdated,_profile,_security,_source,url,version,name,title,status,publisher,description,reference,expansion"
      expand:
        enabled: true
        allowedParams: "url,valueSet,valueSetVersion,context,contextDirection,filter,date,offset,count,includeDesignations,designation,includeDefinition,activeOnly,excludeNested,excludeNotForUI,excludePostCoordinated,displayLanguage,exclude-system,system-version,check-system-version,force-system-version,useSupplement"
      validateCode:
        enabled: true
        allowedParams: "url,context,valueSet,valueSetVersion,code,system,systemVersion,display,coding,codeableConcept,date,abstract,displayLanguage"

    conceptmap:
      read:
        enabled: true
      search:
        enabled: true
        allowedParams: "_content,_id,_lastUpdated,_profile,_security,_source,url,version,name,title,status,publisher,description,source,target,source-uri,target-uri,source-system,target-system"
      translate:
        enabled: false
        allowedParams: "url,conceptMap,conceptMapVersion,code,system,version,source,coding,codeableConcept,target,targetsystem,dependency,reverse"
      closure:
        enabled: false
        allowedParams: "name,concept,version"

# ========================================
# Management & Actuator Configuration
# ========================================

management:
  server:
    port: 9090
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,readiness,liveness
  endpoint:
    health:
      probes:
        enabled: true
      show-details: always
      group:
        readiness:
          include: readinessState,terminologyServer
        liveness:
          include: livenessState
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true

```

## Logging

The application uses **Logback** as its logging framework (via Spring Boot's default configuration).
You can configure
logging levels and output formats through application properties, YAML files, or environment
variables.

### Configuration via application.properties

  ```properties
  # Root logging level
logging.level.root=INFO
# Package-specific logging levels
logging.level.de.gematik.zts.termzilla=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.springframework.boot=WARN
logging.level.ca.uhn.fhir=INFO
```

### Configuration via application.yml

```yaml
logging:
  level:
    root: INFO
    de.gematik.zts.termzilla: DEBUG
    org.springframework.web: INFO
    org.springframework.boot: WARN
    ca.uhn.fhir: INFO
```

### Configuration via Environment Variables

Spring Boot automatically converts environment variables to properties. Use uppercase with
underscores:

```bash
# Root logging level
export LOGGING_LEVEL_ROOT=INFO

# Package-specific logging levels
export LOGGING_LEVEL_DE_GEMATIK_ZTS_TX=DEBUG
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=INFO
export LOGGING_LEVEL_CA_UHN_FHIR=WARN

```

### Common Logging Levels

| Level   | Description                        | Use Case                        |
|---------|------------------------------------|---------------------------------|
| `TRACE` | Very detailed information          | Deep debugging                  |
| `DEBUG` | Detailed information for debugging | Development and troubleshooting |
| `INFO`  | General informational messages     | Production default              |
| `WARN`  | Warning messages                   | Potential issues                |
| `ERROR` | Error messages                     | Failures and exceptions         |

## Actuator Endpoints

Spring Boot Actuator provides production-ready features for monitoring and managing the application.
The application
can expose health and info endpoints on a separate management port if desired.

### Configuration via application.properties

```properties
# Management server configuration
management.server.port=9090
management.endpoints.web.base-path=/actuator
management.endpoints.web.exposure.include=health,info
# Health endpoint configuration
management.endpoint.health.probes.enabled=true
management.endpoint.health.show-details=always
# Liveness and Readiness probes
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true
# Custom health indicator groups
management.endpoint.health.group.readiness.include=readinessState,terminologyServer
management.endpoint.health.group.liveness.include=livenessState
```

### Configuration via application.yml

```yaml
management:
  server:
    port: 9090

  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info

  endpoint:
    health:
      probes:
        enabled: true
      show-details: always

  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true

  endpoint:
    health:
      group:
        readiness:
          include: readinessState,terminologyServer
        liveness:
          include: livenessState
```

### Configuration via Environment Variables

```bash
# Management server port
export MANAGEMENT_SERVER_PORT=9090

# Endpoint exposure
export MANAGEMENT_ENDPOINTS_WEB_BASE_PATH=/actuator
export MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info

# Health endpoint configuration
export MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true
export MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=always

# Probe configuration
export MANAGEMENT_HEALTH_LIVENESSSTATE_ENABLED=true
export MANAGEMENT_HEALTH_READINESSSTATE_ENABLED=true

# Health groups
export MANAGEMENT_ENDPOINT_HEALTH_GROUP_READINESS_INCLUDE=readinessState,terminologyServer
export MANAGEMENT_ENDPOINT_HEALTH_GROUP_LIVENESS_INCLUDE=livenessState
```

### Available Endpoints

| Endpoint      | URL                                               | Description                                             |
|---------------|---------------------------------------------------|---------------------------------------------------------|
| **Health**    | `http://localhost:9090/actuator/health`           | Overall application health status                       |
| **Liveness**  | `http://localhost:9090/actuator/health/liveness`  | Indicates if the application is running                 |
| **Readiness** | `http://localhost:9090/actuator/health/readiness` | Indicates if the application is ready to accept traffic |
| **Info**      | `http://localhost:9090/actuator/health/info`      | Application information                                 |

### Health Check Responses

**Liveness Probe** (Application is running):

```json
{
  "status": "UP",
  "groups": [
    "liveness"
  ]
}
```

**Readiness Probe** (Before initialization):

```json
{
  "status": "DOWN",
  "groups": [
    "readiness"
  ],
  "components": {
    "terminologyServer": {
      "status": "DOWN",
      "details": {
        "status": "initializing"
      }
    }
  }
}
```

**Readiness Probe** (After initialization):

```json
{
  "status": "UP",
  "groups": [
    "readiness"
  ],
  "components": {
    "terminologyServer": {
      "status": "UP",
      "details": {
        "status": "initialized"
      }
    }
  }
}
```

### Testing Actuator Endpoints

```bash
# Check overall health
curl http://localhost:9090/actuator/health

# Check liveness
curl http://localhost:9090/actuator/health/liveness

# Check readiness
curl http://localhost:9090/actuator/health/readiness

# Pretty print with jq
curl -s http://localhost:9090/actuator/health | jq
```

### Custom Health Indicator

The application includes a custom `TerminologyServerHealthIndicator` that monitors the
initialization status of the
terminology server. This indicator is part of the readiness probe group, ensuring that the
application only receives
traffic after the server is fully initialized.

# Module Configuration

The modules `fhir`, `snomed`, and `loinc` use **MicroProfile Config** with **SmallRye Config** for
flexible
configuration management. Configuration can be provided through YAML files, properties files,
environment variables, or
system properties.

## Configuration Sources and Priority

SmallRye Config follows this priority order (highest to lowest):

| Priority | Source                           | Example                  | Description                     |
|----------|----------------------------------|--------------------------|---------------------------------|
| 400      | System properties                | `-Dfhirterms.start=true` | JVM system properties           |
| 300      | Environment variables            | `FHIRTERMS_START=true`   | OS environment variables        |
| **250**  | **Custom YAML files**            | `application.yaml`       | **Custom ConfigSourceProvider** |
| 100      | `application.properties`         | `src/main/resources`     | Standard properties files       |
| 100      | `microprofile-config.properties` | `META-INF/`              | MicroProfile standard           |

**Note:** The modules use a custom YAML `ConfigSourceProvider` with priority **250**, placing YAML
configuration between
environment variables and standard properties files.

**Each module can have its own `application.yaml` file** for module-specific configuration.

## Configuration Format

### YAML Configuration

Each module can have its own `application.yaml` file in its classpath or specify a custom location:

```yaml
fhirterms:
  start: true
  packageFolderLocation: /path/to/fhir/packages
  additionalResourcesFolderLocation: /path/to/additional/resources
  enrichmentFolderLocation: /path/to/enrichment
  loadDependencies: false


  provisionMode: predefined
  provisionSettingsPath: /path/to/provision-settings.yaml

loinc:
  start: false
  defaultLanguage: de-DE
  languages:
    - de-DE
    - de-AT
    - es-ES
  rfCollectionLocation: /path/to/loinc/input
  provisionMode: supported

snomed:
  start: false
  releasefile: /path/to/snomed/release.zip
  provisionMode: supported
  contextExtensionInDesignations: false
  expressionProcessingEnabled: true
  restrictedMode: true
```

### Properties File Configuration

Alternatively, use `microprofile-config.properties` or `application.properties`:

```properties
# FHIR Configuration
fhirterms.start=true
fhirterms.packageFolderLocation=/path/to/fhir/packages
fhirterms.loadDependencies=false
fhirterms.provisionMode=predefined
fhirterms.provisionSettingsPath=/path/to/provision-settings.yaml
# LOINC Configuration
loinc.start=false
loinc.defaultLanguage=de-DE
loinc.languages=de-DE,de-AT,es-ES
loinc.rfCollectionLocation=/path/to/loinc/input
loinc.provisionMode=supported
# SNOMED Configuration
snomed.start=false
snomed.releasefile=/path/to/snomed/release.zip
snomed.provisionMode=supported
snomed.contextExtensionInDesignations=false
snomed.expressionProcessingEnabled=true
snomed.restrictedMode=true
```

### Environment Variables

Environment variables use uppercase with underscores:

```bash
# FHIR Configuration
export FHIRTERMS_START=true
export FHIRTERMS_PACKAGEFOLDERLOCATION=/path/to/fhir/packages
export FHIRTERMS_LOADDEPENDENCIES=false
export FHIRTERMS_PROVISIONMODE=predefined

# LOINC Configuration
export LOINC_START=false
export LOINC_DEFAULTLANGUAGE=de-DE
export LOINC_LANGUAGES=de-DE,de-AT,es-ES
export LOINC_RFCOLLECTIONLOCATION=/path/to/loinc/input
export LOINC_PROVISIONMODE=supported

# SNOMED Configuration
export SNOMED_START=false
export SNOMED_RELEASEFILE=/path/to/snomed/release.zip
export SNOMED_PROVISIONMODE=supported
export SNOMED_CONTEXTEXTENSIONINDESIGNATIONS=false
export SNOMED_EXPRESSIONPROCESSINGENABLED=true
export SNOMED_RESTRICTEDMODE=true
```

## SNOMED Module Configuration

### Basic Configuration Properties

| Property                                | Type    | Default     | Description                                                                                             |
|-----------------------------------------|---------|-------------|---------------------------------------------------------------------------------------------------------|
| `snomed.start`                          | boolean | `false`     | Enable/disable the SNOMED module                                                                        |
| `snomed.releasefile`                    | string  | *required*  | Path to the SNOMED CT release archive                                                                   |
| `snomed.provisionMode`                  | enum    | `SUPPORTED` | Provision mode for SNOMED resources (`SUPPORTED` or `PREDEFINED`)                                       |
| `snomed.contextExtensionInDesignations` | boolean | `false`     | Include SNOMED context extension metadata in returned designation components                            |
| `snomed.expressionProcessingEnabled`    | boolean | `true`      | Enable SCG expression processing (validation and term rendering). Parsing remains available either way. |
| `snomed.restrictedMode`                 | boolean | `true`      | Restricted mode: only concepts, descriptions, language refsets and refset membership are exposed; concept relationships (hierarchy, attributes, MRCM) and the definition status are not. Forces expression processing off. |

## FHIR Module Configuration

### Basic Configuration Properties

| Property                                      | Type    | Default      | Description                                                       |
|-----------------------------------------------|---------|--------------|-------------------------------------------------------------------|
| `fhirterms.start`                             | boolean | `false`      | Enable/disable the FHIR module                                    |
| `fhirterms.packageFolderLocation`             | string  | *required*   | Path to FHIR package directory                                    |
| `fhirterms.additionalResourcesFolderLocation` | string  | *(optional)* | Path to additional FHIR resources                                 |
| `fhirterms.enrichmentFolderLocation`          | string  | *(optional)* | Path to enrichment resources                                      |
| `fhirterms.loadDependencies`                  | boolean | `false`      | Load package dependencies                                         |
| `fhirterms.packageAutoDownloadEnabled`        | boolean | `false`      | Enable automatic package download from configured package servers |
| `fhirterms.provisionMode`                     | enum    | `PREDEFINED` | Default provision mode: `PREDEFINED` or `SUPPORTED`               |
| `fhirterms.provisionSettingsPath`             | string  | *(optional)* | Path to provision settings YAML file                              |

### Package Server Configuration (FHIR)

Use `fhirterms.packageServerConfig.servers` to configure one or more package servers, including
per-server authentication and package lists.

#### YAML Example

```yaml
fhirterms:
  packageAutoDownloadEnabled: true
  packageServerConfig:
    servers:
      - name: "ZTS BfArM"
        url: "https://terminologien.bfarm.de"
        auth:
          type: BFARM_AUTO_TOKEN
        skipDeprecatedUnlistedPackageVersions: false
        endpoints:
          packagePathPrefix: "/packages"
          catalogPath: "/packages/catalog"
        packages:
          - "bfarm.terminologien.icd10gm@2024.0.0"
          - "bfarm.terminologien.ops@2024.0.0"

      - name: "FHIR Packages"
        url: "https://packages.fhir.org"
        auth:
          type: NONE
        skipDeprecatedUnlistedPackageVersions: true
        endpoints:
          packagePathPrefix: ""
          catalogPath: "/catalog"
        packages:
          - "hl7.fhir.r4.core@4.0.1"

      - name: "FHIR Simplifier"
        url: "https://simplifier.net"
        auth:
          type: BASIC
          user: "user"
          password: "password"
        packages:
          - "my.package.core@1.2.3"
```

#### Endpoint Path Settings

Each server can optionally define endpoint paths:

| Property                      | Default             | Description                                                            |
|-------------------------------|---------------------|------------------------------------------------------------------------|
| `endpoints.packagePathPrefix` | `/packages`         | Path prefix used for package metadata and package download requests    |
| `endpoints.catalogPath`       | `/packages/catalog` | Path used for catalog requests when `*` package entries are configured |

Examples:

- **BfArM-style layout:**
    - metadata: `https://terminologien.bfarm.de/packages/{packageName}`
    - catalog: `https://terminologien.bfarm.de/packages/catalog`
- **Root layout (e.g. packages.fhir.org):**
    - set `packagePathPrefix: ""` and `catalogPath: "/catalog"`
    - metadata: `https://packages.fhir.org/{packageName}`
    - catalog: `https://packages.fhir.org/catalog`

#### Deprecated/Unlisted Version Filtering

Use `skipDeprecatedUnlistedPackageVersions` per server to control wildcard catalog filtering.

| Property                                | Default | Description                                                                                                                      |
|-----------------------------------------|---------|----------------------------------------------------------------------------------------------------------------------------------|
| `skipDeprecatedUnlistedPackageVersions` | `false` | When `true`, package entries marked as `deprecated` and/or `unlisted` in package metadata are skipped during package resolution. |

Behavior notes:

- Applies to explicit package entries (for example `package.name@x.y.z`) and wildcard-expanded
  entries.
- `deprecated` and `unlisted` are treated equally.
- Skipped versions are logged as warnings including `package@version` and the metadata message.

Properties example:

```properties
fhirterms.packageServerConfig.servers[0].skipDeprecatedUnlistedPackageVersions=false
fhirterms.packageServerConfig.servers[1].skipDeprecatedUnlistedPackageVersions=true
```

#### Authentication Types

| Value              | Description                                                                          |
|--------------------|--------------------------------------------------------------------------------------|
| `NONE`             | No credentials are sent                                                              |
| `BFARM_AUTO_TOKEN` | Token is generated via the configured server token endpoint and used as Bearer token |
| `BASIC`            | HTTP Basic authentication (`user` + `password`)                                      |
| `BEARER_TOKEN`     | Static bearer token authentication (`token`)                                         |
| `API_KEY`          | API key authentication using request header (`apiKey`, optional `apiKeyHeader`)      |

**Authentication Properties by Type:**

- `BASIC`: `auth.user`, `auth.password`
- `BEARER_TOKEN`: `auth.token`
- `API_KEY`: `auth.apiKey`, optional `auth.apiKeyHeader` (default: `X-API-Key`)
- `BFARM_AUTO_TOKEN`: no static credential fields required

**YAML Authentication Examples:**

```yaml
# BASIC
auth:
  type: BASIC
  user: "user"
  password: "password"

# BEARER_TOKEN
auth:
  type: BEARER_TOKEN
  token: "<bearer-token>"

# API_KEY
auth:
  type: API_KEY
  apiKey: "<api-key>"
  apiKeyHeader: "X-API-Key"
```

**Compliance Notice for `BFARM_AUTO_TOKEN`:**
When `BFARM_AUTO_TOKEN` is configured, the server automatically requests tokens and downloads
protected packages from the BfArM registry. By enabling this mode, the operator explicitly
confirms acceptance of the applicable BfArM download conditions:
`https://terminologien.bfarm.de/download-conditions.html`.

#### Environment Variable Example (indexed properties)

For nested list structures, configure indexed keys.
For package server list entries in this module, use a double underscore (`__`) after each numeric
index (for example `SERVERS_0__NAME`). This keeps indexed path segments unambiguous for the
configuration mapping in use.

```bash
export FHIRTERMS_PACKAGEAUTODOWNLOADENABLED=true

export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__NAME="ZTS BfArM"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__URL="https://terminologien.bfarm.de"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__AUTH_TYPE="BFARM_AUTO_TOKEN"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__SKIPDEPRECATEDUNLISTEDPACKAGEVERSIONS="false"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__ENDPOINTS_PACKAGEPATHPREFIX="/packages"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__ENDPOINTS_CATALOGPATH="/packages/catalog"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__PACKAGES_0="bfarm.terminologien.icd10gm@2024.0.0"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__PACKAGES_1="bfarm.terminologien.ops@2024.0.0"

export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__NAME="FHIR Packages"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__URL="https://packages.fhir.org"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__AUTH_TYPE="NONE"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__SKIPDEPRECATEDUNLISTEDPACKAGEVERSIONS="true"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__ENDPOINTS_PACKAGEPATHPREFIX=""
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__ENDPOINTS_CATALOGPATH="/catalog"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__PACKAGES_0="hl7.fhir.r4.core@4.0.1"

export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_2__NAME="FHIR Simplifier"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_2__URL="https://simplifier.net"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_2__AUTH_TYPE="BASIC"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_2__AUTH_USER="user"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_2__AUTH_PASSWORD="password"
export FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_2__PACKAGES_0="my.package.core@1.2.3"
```

### Provision Modes

The FHIR module supports two provision modes that apply to terminology resources (CodeSystem,
ValueSet, ConceptMap):

- **`SUPPORTED`**: Resources are available for terminology operations:
    - CodeSystem: `$expand`, `$validate-code`, `$lookup`
    - ValueSet: `$expand`, `$validate-code`
    - ConceptMap: `$translate`

- **`PREDEFINED`**: Resources are exposed through their respective REST endpoints:
    - `/CodeSystem` - Browse and retrieve CodeSystem resources
    - `/ValueSet` - Browse and retrieve ValueSet resources
    - `/ConceptMap` - Browse and retrieve ConceptMap resources

**Note:** `PREDEFINED` mode includes `SUPPORTED` functionality, making resources available both
through endpoints and
operations.

### Advanced Provision Settings

For fine-grained control, use a provision settings file to configure provision modes per package,
version, or individual
resource:

```yaml
provision-settings:
  packages:
    # All versions of a package - all resources supported
    - id: bfarm.terminologien.orphacodes
      version: "*"
      mode: supported

    # Specific package without version constraint
    - id: bfarm.terminologien.loinc
      mode: supported

    # Predefined mode with specific supported resources
    - id: bfarm.terminologien.ops
      version: "^2022.0.0"
      mode: predefined
      supported:
        - "http://fhir.de/CodeSystem/bfarm/ops"

    # Different mode for newer versions
    - id: bfarm.terminologien.ops
      version: "^2023.0.0"
      mode: supported

    # Supported mode with exceptions for specific resources as predefined
    - id: bfarm.terminologien.icd10gm
      version: "*"
      mode: supported
      predefined:
        - "https://terminologien.bfarm.de/fhir/ValueSet/*"
```

**Provision Settings Properties:**

| Property     | Type   | Description                                                                         |
|--------------|--------|-------------------------------------------------------------------------------------|
| `id`         | string | FHIR package ID                                                                     |
| `version`    | string | Version constraint (`*` = all versions, `^X.Y.Z` = semver range)                    |
| `mode`       | enum   | Base provision mode: `PREDEFINED` or `SUPPORTED`                                    |
| `supported`  | array  | List of resource URL patterns to treat as `SUPPORTED` (overrides `PREDEFINED` mode) |
| `predefined` | array  | List of resource URL patterns to treat as `PREDEFINED` (overrides `SUPPORTED` mode) |

**Pattern Matching:**

- Supports wildcards using `*` (e.g., `https://example.com/fhir/ValueSet/*`)
- Patterns match against the resource's canonical URL
- More specific patterns take precedence over the base `mode`

**Examples:**

1. **Base mode `PREDEFINED` with exceptions:**
   ```yaml
   - id: bfarm.terminologien.ops
     mode: predefined
     supported:
       - "http://fhir.de/CodeSystem/bfarm/ops"
   ```
   All resources are `PREDEFINED` except the specified CodeSystem, which is `SUPPORTED` only.

2. **Base mode `SUPPORTED` with exceptions:**
   ```yaml
   - id: bfarm.terminologien.icd10gm
     mode: supported
     predefined:
       - "https://terminologien.bfarm.de/fhir/ValueSet/*"
   ```
   All resources are `SUPPORTED` except ValueSets matching the pattern, which are `PREDEFINED`.
