# 🏆 Desafio 13 — Authentication, Authorization & SSO (JWT / OAuth2 / Keycloak)

> **Nível:** ⭐⭐⭐⭐ Expert
> **Tipo:** Security · JWT · OAuth2 · OIDC · SSO · Keycloak · RBAC · MFA
> **Estimativa:** 12–16 horas por stack

---

## 📋 Descrição

Implementar **autenticação e autorização completa** na API Person usando **Keycloak** como Identity Provider central com **SSO (Single Sign-On)**, **OAuth2/OIDC**, **RBAC**, **MFA** e **Social Login**. Cobrir cenários enterprise como federação de identidade, multi-tenancy, token introspection, e session management.

---

## 🎯 Objetivos de Aprendizado

- [ ] **Keycloak** — Realms, Clients, Roles, Users, Groups, Identity Providers
- [ ] **SSO (Single Sign-On)** — Login único entre múltiplas aplicações
- [ ] **OAuth2** — Authorization Code, Client Credentials, PKCE
- [ ] **OIDC (OpenID Connect)** — ID Token, UserInfo, Discovery
- [ ] **JWT** — Estrutura, assinatura (RS256), validação, claims customizadas
- [ ] **RBAC** com roles (Realm + Client) e fine-grained permissions
- [ ] **Social Login** — Google, GitHub como Identity Providers
- [ ] **MFA** (Multi-Factor Authentication) — TOTP (Google Authenticator)
- [ ] **Token Introspection** e **Token Exchange**
- [ ] **Session Management** — Logout, SSO Logout (backchannel/frontchannel)
- [ ] **Refresh Token** rotation com detecção de reuso
- [ ] **CORS** — configuração segura
- [ ] **API Key** — autenticação simplificada para integrações M2M

---

## 📐 Especificação

### Arquitetura SSO com Keycloak

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Frontend A  │    │  Frontend B  │    │  Mobile App  │
│  (SPA React) │    │  (Admin UI)  │    │  (Flutter)   │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       │    ┌──────────────▼───────────────┐   │
       └───▶│        KEYCLOAK SSO          │◀──┘
            │   Realm: person-api          │
            │                              │
            │  ┌─────────┐ ┌─────────────┐ │
            │  │ Users   │ │ Social IdPs │ │
            │  │ Groups  │ │ Google      │ │
            │  │ Roles   │ │ GitHub      │ │
            │  └─────────┘ └─────────────┘ │
            │  ┌─────────────────────────┐ │
            │  │ Clients:               │ │
            │  │  person-web (SPA)      │ │
            │  │  person-admin (web)    │ │
            │  │  person-api (backend)  │ │
            │  │  person-m2m (service)  │ │
            │  └─────────────────────────┘ │
            └──────────────┬───────────────┘
                           │ JWT (access_token)
            ┌──────────────▼───────────────┐
            │     API Person (Resource     │
            │        Server)               │
            │   - Validate JWT             │
            │   - Check roles              │
            │   - Extract claims           │
            └──────────────────────────────┘
```

> **SSO em ação:** Usuário faz login no Frontend A → acessa Frontend B sem pedir credenciais novamente → token do Keycloak é compartilhado entre todas as aplicações do mesmo Realm.

---

### Keycloak — Configuração do Realm

#### Realm: `person-api`

| Configuração | Valor |
|---|---|
| Realm name | `person-api` |
| Login Theme | `keycloak` |
| Email as username | `true` |
| Registration allowed | `true` |
| Remember me | `true` |
| Login with email | `true` |
| MFA Policy | Optional (TOTP) |
| Brute force detection | Enabled (5 failures → 30min lock) |
| Password policy | 8+ chars, 1 upper, 1 digit, 1 special |
| SSO Session Idle | 30 min |
| SSO Session Max | 10 hours |
| Access Token Lifespan | 5 min |
| Refresh Token Lifespan | 30 min |

#### Clients

| Client ID | Type | Flow | Redirect URIs |
|---|---|---|---|
| `person-web` | Public (SPA) | Authorization Code + PKCE | `http://localhost:3000/*` |
| `person-admin` | Confidential (Web) | Authorization Code | `http://localhost:3001/*` |
| `person-api` | Bearer Only | — (Resource Server) | — |
| `person-m2m` | Confidential (Service) | Client Credentials | — |

#### Realm Roles

| Role | Descrição | Herança |
|---|---|---|
| `viewer` | Read-only access | — |
| `editor` | CRUD operations | inherits `viewer` |
| `admin` | Full access + user management | inherits `editor` |
| `super-admin` | Cross-tenant management | inherits `admin` |

#### Groups

| Group | Roles Atribuídas | Descrição |
|---|---|---|
| `/viewers` | `viewer` | Usuários somente leitura |
| `/editors` | `editor` | Usuários com permissão de escrita |
| `/admins` | `admin` | Administradores |
| `/service-accounts` | custom per service | Contas de serviço M2M |

#### Custom Claims (Protocol Mapper)

| Claim | Source | Token |
|---|---|---|
| `tenant_id` | User Attribute | Access + ID |
| `department` | User Attribute | Access + ID |
| `full_name` | User Property | ID Token |
| `roles` | Realm Roles | Access Token |

---

### Roles & Permissions (RBAC)

| Role | `GET /persons` | `GET /persons/{id}` | `POST /persons` | `PUT /persons/{id}` | `DELETE /persons/{id}` | `GET /admin/*` |
|---|---|---|---|---|---|---|
| `viewer` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| `editor` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| `admin` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Anônimo | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

### Endpoints

| Método | Rota | Auth | Role Mínima | Descrição |
|---|---|---|---|---|
| `POST` | `/api/auth/login` | Público | — | Login (redirect para Keycloak) |
| `POST` | `/api/auth/logout` | Autenticado | — | Logout (revoga token + SSO logout) |
| `POST` | `/api/auth/refresh` | Público (refresh_token) | — | Renova access token |
| `GET` | `/api/auth/userinfo` | Autenticado | — | Dados do usuário logado |
| `GET` | `/api/persons` | JWT | `viewer` | Lista persons |
| `GET` | `/api/persons/{id}` | JWT | `viewer` | Busca person por ID |
| `POST` | `/api/persons` | JWT | `editor` | Cria person |
| `PUT` | `/api/persons/{id}` | JWT | `editor` | Atualiza person |
| `DELETE` | `/api/persons/{id}` | JWT | `admin` | Remove person |
| `GET` | `/api/admin/users` | JWT | `admin` | Lista usuários do Keycloak |
| `POST` | `/api/admin/users/{id}/roles` | JWT | `admin` | Atribui role a usuário |
| `GET` | `/api/admin/sessions` | JWT | `admin` | Sessões ativas |
| `POST` | `/api/auth/token` | API Key | — | Gera token via API Key (M2M) |

---

### OAuth2 Flows

#### 1. Authorization Code + PKCE (SPA / Mobile)

```
1. Frontend gera code_verifier + code_challenge (SHA256)
2. Redirect → Keycloak /authorize?
     response_type=code&
     client_id=person-web&
     redirect_uri=http://localhost:3000/callback&
     scope=openid profile email&
     code_challenge={hash}&
     code_challenge_method=S256
3. Usuário faz login (ou SSO automático se já logado)
4. Keycloak redirect → callback?code=xxx
5. Frontend POST /token com code + code_verifier
6. Recebe: access_token + refresh_token + id_token
```

#### 2. Client Credentials (Service-to-Service / M2M)

```bash
curl -X POST http://localhost:8180/realms/person-api/protocol/openid-connect/token \
  -d "grant_type=client_credentials" \
  -d "client_id=person-m2m" \
  -d "client_secret=SECRET" \
  -d "scope=openid"
```

#### 3. Refresh Token (com Rotation)

```bash
curl -X POST http://localhost:8180/realms/person-api/protocol/openid-connect/token \
  -d "grant_type=refresh_token" \
  -d "client_id=person-web" \
  -d "refresh_token=CURRENT_REFRESH_TOKEN"

# Retorna novo access_token + NOVO refresh_token (rotation)
# O refresh_token antigo é invalidado
# Se alguém tentar reusar o antigo → toda a sessão é revogada (replay detection)
```

---

### JWT Token (Access Token)

```json
{
  "exp": 1709550600,
  "iat": 1709547000,
  "jti": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "iss": "http://localhost:8180/realms/person-api",
  "aud": "person-api",
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "typ": "Bearer",
  "azp": "person-web",
  "session_state": "d59a6b00-c78c-4aa4-9d3e-b52c14f1c7d5",
  "scope": "openid profile email",
  "sid": "d59a6b00-c78c-4aa4-9d3e-b52c14f1c7d5",
  "preferred_username": "wesley@example.com",
  "email": "wesley@example.com",
  "email_verified": true,
  "name": "Wesley Silva",
  "given_name": "Wesley",
  "family_name": "Silva",
  "realm_access": {
    "roles": ["admin", "editor", "viewer"]
  },
  "resource_access": {
    "person-api": {
      "roles": ["manage-persons"]
    }
  },
  "tenant_id": "acme-corp",
  "department": "engineering"
}
```

### ID Token (OIDC)

```json
{
  "iss": "http://localhost:8180/realms/person-api",
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "aud": "person-web",
  "exp": 1709550600,
  "iat": 1709547000,
  "auth_time": 1709547000,
  "nonce": "abc123",
  "acr": "1",
  "at_hash": "MTIzNDU2Nzg5MA",
  "preferred_username": "wesley@example.com",
  "email": "wesley@example.com",
  "name": "Wesley Silva"
}
```

---

### Social Login (Identity Providers)

#### Google

| Configuração | Valor |
|---|---|
| Provider | Google |
| Client ID | `google-client-id.apps.googleusercontent.com` |
| Client Secret | `google-secret` |
| Default Scopes | `openid email profile` |
| First Login Flow | `first broker login` |
| Account linking | Automatic (by email) |

#### GitHub

| Configuração | Valor |
|---|---|
| Provider | GitHub |
| Client ID | `github-client-id` |
| Client Secret | `github-secret` |
| Default Scopes | `user:email` |
| First Login Flow | `first broker login` |

> No Keycloak: **Identity Providers** → Add provider → Google/GitHub → preencher Client ID/Secret da OAuth App criada em cada plataforma.

---

### MFA — Multi-Factor Authentication (TOTP)

| Configuração | Valor |
|---|---|
| MFA Policy | Optional (pode ser Required) |
| OTP Type | TOTP (Time-based) |
| Algorithm | SHA-1 (Google Authenticator compatível) |
| Digits | 6 |
| Period | 30 seconds |
| Look ahead | 1 |
| Compatible Apps | Google Authenticator, Authy, 1Password |

Fluxo:
1. Usuário faz login com email/senha
2. Keycloak detecta que MFA está habilitado para o usuário
3. Apresenta QR code (primeiro uso) ou solicita código TOTP
4. Validação do código → emite tokens normalmente

---

### SSO Logout

#### Backchannel Logout (recomendado)

```
1. Usuário clica "Logout" no Frontend A
2. Frontend A chama POST /api/auth/logout
3. API revoga token no Keycloak
4. Keycloak envia POST para backchannel_logout_url de TODOS os Clients
5. Frontend B recebe notificação e limpa sessão local
6. Usuário está deslogado de TODAS as aplicações
```

#### Endpoint do Keycloak

```
POST http://localhost:8180/realms/person-api/protocol/openid-connect/logout
Content-Type: application/x-www-form-urlencoded

client_id=person-web&
refresh_token=REFRESH_TOKEN
```

---

### Respostas de Segurança

```json
// 401 Unauthorized — Token inválido ou ausente
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "details": "JWT signature verification failed"
}

// 403 Forbidden — Role insuficiente
{
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions",
  "required_role": "admin",
  "your_roles": ["viewer"]
}

// 401 — MFA Required
{
  "status": 401,
  "error": "MFA Required",
  "message": "Multi-factor authentication is required",
  "mfa_challenge_url": "http://localhost:8180/realms/person-api/login-actions/required-action?execution=CONFIGURE_TOTP"
}
```

---

## ✅ Critérios de Aceite

### Core
- [ ] Keycloak configurado com realm `person-api`, 4 clients, 4 roles, groups
- [ ] **SSO funcionando** — login em uma app automaticamente autentica nas outras
- [ ] Authorization Code + PKCE flow (SPA)
- [ ] Client Credentials flow (M2M / service-to-service)
- [ ] JWT validation (RS256, JWKS endpoint)
- [ ] RBAC com 4 roles (viewer, editor, admin, super-admin) e role hierarchy
- [ ] Custom claims no JWT (tenant_id, department)

### SSO & Identity Federation
- [ ] **Social Login** — Google OU GitHub como Identity Provider
- [ ] **MFA** (TOTP) configurado e funcional
- [ ] **SSO Logout** — backchannel logout entre clients
- [ ] **Refresh token rotation** com detecção de reuso

### Security
- [ ] 401 para token inválido/expirado
- [ ] 403 para role insuficiente (com detalhes da role necessária)
- [ ] CORS configurado por ambiente
- [ ] Rate limiting no endpoint de login

### Admin
- [ ] Endpoint admin para listar usuários do Keycloak (Admin REST API)
- [ ] Endpoint admin para atribuir roles

### Testing
- [ ] Testes de integração com Keycloak (Testcontainers)
- [ ] Testes com tokens mock (JWT gerado localmente para testes unitários)

### Automation
- [ ] **Realm export** (JSON) versionado no repositório
- [ ] Script de inicialização que importa realm automaticamente

---

## 🐳 Docker Compose

```yaml
services:
  # ========================
  # Keycloak (SSO / IdP)
  # ========================
  keycloak:
    image: quay.io/keycloak/keycloak:26.2
    ports:
      - "8180:8080"
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://keycloak-db:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: keycloak
      KC_HEALTH_ENABLED: "true"
      KC_METRICS_ENABLED: "true"
      KC_FEATURES: "token-exchange,admin-fine-grained-authz"
    command: start-dev --import-realm
    volumes:
      - ./keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json
    depends_on:
      - keycloak-db

  keycloak-db:
    image: postgres:17
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: keycloak
    volumes:
      - keycloak-data:/var/lib/postgresql/data

  # ========================
  # API Person
  # ========================
  api-person:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/person-api
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/person-api/protocol/openid-connect/certs
    depends_on:
      - keycloak
      - postgres

  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: persondb
      POSTGRES_USER: person
      POSTGRES_PASSWORD: person123

volumes:
  keycloak-data:
```

---

### Configurar Keycloak (Manual ou Import)

#### Opção 1: Import Automático (recomendado)
```bash
# Exportar realm configurado:
docker exec keycloak /opt/keycloak/bin/kc.sh export --dir /opt/keycloak/data/export --realm person-api

# Copiar para o projeto:
docker cp keycloak:/opt/keycloak/data/export/person-api-realm.json ./keycloak/realm-export.json

# O Docker Compose acima já importa automaticamente na inicialização
```

#### Opção 2: Configuração Manual
1. Acessar http://localhost:8180 (admin/admin)
2. Criar Realm: `person-api`
3. **Realm Settings** → Login → Enable: Registration, Remember Me, Login with Email
4. **Authentication** → Required Actions → Configure TOTP (optional)
5. Criar Clients: `person-web` (public, PKCE), `person-admin` (confidential), `person-api` (bearer-only), `person-m2m` (confidential, service account)
6. Criar Realm Roles: `viewer`, `editor` (composite: viewer), `admin` (composite: editor), `super-admin` (composite: admin)
7. Criar Groups: `/viewers`, `/editors`, `/admins`, `/service-accounts`
8. Criar Users e atribuir a groups
9. **Identity Providers** → Add Google / GitHub
10. **Realm Settings** → Tokens → Access Token Lifespan: 5 min, Refresh Token: 30 min

---

## 🛠️ Implementar em

| Stack | SSO/OIDC | JWT Validation | Admin API |
|---|---|---|---|
| **Spring Boot** | `spring-boot-starter-oauth2-resource-server` + `spring-security-oauth2-client` | JWKS auto-discovery | `keycloak-admin-client` |
| **Micronaut** | `micronaut-security-oauth2` + `micronaut-security-jwt` | JWKS auto-discovery | Keycloak Admin REST (HTTP client) |
| **Quarkus** | `quarkus-oidc` + `quarkus-keycloak-admin-client` | JWKS auto-discovery | `quarkus-keycloak-admin-client` |
| **Go** | `github.com/coreos/go-oidc/v3` + `golang-jwt/jwt/v5` | JWKS manual (com cache) | Keycloak Admin REST (HTTP client) |

---

## 💡 Dicas

### Spring Boot — Resource Server + SSO

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/persons/**").hasRole("viewer")
                .requestMatchers(HttpMethod.POST, "/api/persons").hasRole("editor")
                .requestMatchers(HttpMethod.PUT, "/api/persons/**").hasRole("editor")
                .requestMatchers(HttpMethod.DELETE, "/api/persons/**").hasRole("admin")
                .requestMatchers("/api/admin/**").hasRole("admin")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(Customizer.withDefaults())
            .build();
    }

    // Converter realm_access.roles para Spring Security GrantedAuthorities
    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthorities = new JwtGrantedAuthoritiesConverter();
        // Extrair roles do claim realm_access.roles
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        });
        return converter;
    }
}
```

### Spring Boot — Keycloak Admin Client

```java
@Service
public class KeycloakAdminService {
    private final Keycloak keycloak;
    
    public KeycloakAdminService() {
        this.keycloak = KeycloakBuilder.builder()
            .serverUrl("http://localhost:8180")
            .realm("master")
            .clientId("admin-cli")
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientSecret("admin-secret")
            .build();
    }
    
    public List<UserRepresentation> listUsers() {
        return keycloak.realm("person-api").users().list();
    }
    
    public void assignRole(String userId, String roleName) {
        RoleRepresentation role = keycloak.realm("person-api")
            .roles().get(roleName).toRepresentation();
        keycloak.realm("person-api").users().get(userId)
            .roles().realmLevel().add(List.of(role));
    }
}
```

### Go — OIDC Discovery + JWT Validation

```go
package middleware

import (
    "context"
    "net/http"
    "strings"

    "github.com/coreos/go-oidc/v3/oidc"
    "github.com/gin-gonic/gin"
)

type AuthMiddleware struct {
    verifier *oidc.IDTokenVerifier
}

func NewAuthMiddleware(issuerURL string) (*AuthMiddleware, error) {
    // OIDC Discovery: busca JWKS endpoint automaticamente
    provider, err := oidc.NewProvider(context.Background(), issuerURL)
    if err != nil {
        return nil, err
    }
    verifier := provider.Verifier(&oidc.Config{
        ClientID: "person-api",
    })
    return &AuthMiddleware{verifier: verifier}, nil
}

func (a *AuthMiddleware) Authenticate() gin.HandlerFunc {
    return func(c *gin.Context) {
        header := c.GetHeader("Authorization")
        if header == "" {
            c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "missing token"})
            return
        }
        rawToken := strings.TrimPrefix(header, "Bearer ")
        
        token, err := a.verifier.Verify(c.Request.Context(), rawToken)
        if err != nil {
            c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "invalid token"})
            return
        }
        
        // Extrair claims customizadas
        var claims struct {
            RealmAccess struct {
                Roles []string `json:"roles"`
            } `json:"realm_access"`
            TenantID string `json:"tenant_id"`
            Email    string `json:"email"`
        }
        token.Claims(&claims)
        
        c.Set("user_id", token.Subject)
        c.Set("roles", claims.RealmAccess.Roles)
        c.Set("tenant_id", claims.TenantID)
        c.Set("email", claims.Email)
        c.Next()
    }
}

// Role-based middleware
func RequireRole(role string) gin.HandlerFunc {
    return func(c *gin.Context) {
        roles, exists := c.Get("roles")
        if !exists {
            c.AbortWithStatusJSON(http.StatusForbidden, gin.H{"error": "no roles"})
            return
        }
        for _, r := range roles.([]string) {
            if r == role {
                c.Next()
                return
            }
        }
        c.AbortWithStatusJSON(http.StatusForbidden, gin.H{
            "error":         "insufficient permissions",
            "required_role": role,
            "your_roles":    roles,
        })
    }
}
```

### Quarkus — OIDC + @RolesAllowed

```java
// application.properties
quarkus.oidc.auth-server-url=http://localhost:8180/realms/person-api
quarkus.oidc.client-id=person-api
quarkus.oidc.roles.role-claim-path=realm_access/roles

// Controller
@Path("/api/persons")
@Authenticated
public class PersonResource {

    @GET
    @RolesAllowed("viewer")
    public List<Person> list() { ... }

    @POST
    @RolesAllowed("editor")
    public Response create(@Valid PersonRequest request, @Context SecurityContext ctx) {
        String userId = ctx.getUserPrincipal().getName();
        // ...
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    public Response delete(@PathParam("id") Long id) { ... }
}
```

### Keycloak Testcontainers (Testes de Integração)

```java
// Spring Boot Test
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthIntegrationTest {

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.2")
        .withRealmImportFile("test-realm.json");

    @DynamicPropertySource
    static void configureKeycloak(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
            () -> keycloak.getAuthServerUrl() + "/realms/person-api");
    }

    @Test
    void shouldAllowViewerToListPersons() {
        String token = getAccessToken("viewer-user", "password");
        
        given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/persons")
            .then()
            .statusCode(200);
    }

    @Test
    void shouldDenyViewerFromDeleting() {
        String token = getAccessToken("viewer-user", "password");
        
        given()
            .header("Authorization", "Bearer " + token)
            .when()
            .delete("/api/persons/1")
            .then()
            .statusCode(403);
    }

    private String getAccessToken(String username, String password) {
        return RestAssured.given()
            .contentType("application/x-www-form-urlencoded")
            .formParam("grant_type", "password")
            .formParam("client_id", "person-web")
            .formParam("username", username)
            .formParam("password", password)
            .post(keycloak.getAuthServerUrl() + "/realms/person-api/protocol/openid-connect/token")
            .jsonPath().getString("access_token");
    }
}
```

---

## 📦 Dependências Extras

| Stack | Dependências |
|---|---|
| **Spring** | `spring-boot-starter-oauth2-resource-server` + `spring-boot-starter-oauth2-client` + `keycloak-admin-client` |
| **Micronaut** | `micronaut-security-oauth2` + `micronaut-security-jwt` |
| **Quarkus** | `quarkus-oidc` + `quarkus-keycloak-admin-client` |
| **Go** | `github.com/coreos/go-oidc/v3` + `github.com/golang-jwt/jwt/v5` |
| **Test** | `com.github.dasniko:testcontainers-keycloak` (Java) |

---

## 🔗 Referências

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Keycloak Admin REST API](https://www.keycloak.org/docs-api/26.2/rest-api/index.html)
- [OAuth 2.0 — RFC 6749](https://tools.ietf.org/html/rfc6749)
- [OIDC — OpenID Connect Core](https://openid.net/specs/openid-connect-core-1_0.html)
- [PKCE — RFC 7636](https://tools.ietf.org/html/rfc7636)
- [JWT.io](https://jwt.io/)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/)
- [Quarkus OIDC](https://quarkus.io/guides/security-oidc-bearer-token-authentication)
- [go-oidc](https://github.com/coreos/go-oidc)
- [Keycloak Testcontainers](https://github.com/dasniko/testcontainers-keycloak)
