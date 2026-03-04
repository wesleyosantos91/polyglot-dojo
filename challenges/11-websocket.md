# 🏆 Desafio 12 — WebSocket Real-Time

> **Nível:** ⭐⭐ Intermediário
> **Tipo:** WebSocket · Real-Time · Push Notifications · Chat
> **Estimativa:** 5–7 horas por stack

---

## 📋 Descrição

Criar um serviço com **WebSocket** que notifica clientes em tempo real sobre mudanças em Person (criação, atualização, deleção). Implementar também um **chat room** simples para demonstrar comunicação bidirecional.

---

## 🎯 Objetivos de Aprendizado

- [ ] WebSocket handshake e lifecycle
- [ ] Broadcast para todos os clientes conectados
- [ ] Mensagens direcionadas (por user/room)
- [ ] Heartbeat/ping-pong
- [ ] Reconexão automática (client-side)
- [ ] Autenticação no WebSocket (token via query param)
- [ ] Gerenciamento de sessões ativas

---

## 📐 Especificação

### Endpoints WebSocket

| Path | Propósito |
|---|---|
| `ws://host/ws/persons` | Notificações de mudanças em Person |
| `ws://host/ws/chat/{room}` | Chat por sala |

### Mensagens WebSocket — Person Events

```json
// Server → Client (notificação)
{
  "type": "PERSON_CREATED",
  "timestamp": "2026-03-04T10:30:00Z",
  "data": {
    "id": 1,
    "name": "Wesley Santos",
    "email": "wesley@example.com"
  }
}
```

### Mensagens WebSocket — Chat

```json
// Client → Server
{
  "type": "MESSAGE",
  "room": "general",
  "content": "Hello everyone!"
}

// Server → Client (broadcast)
{
  "type": "MESSAGE",
  "room": "general",
  "sender": "Wesley",
  "content": "Hello everyone!",
  "timestamp": "2026-03-04T10:30:00Z"
}

// Server → Client (sistema)
{
  "type": "USER_JOINED",
  "room": "general",
  "user": "Wesley",
  "online_count": 5
}
```

### API REST (complementar)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/ws/sessions` | Lista sessões WebSocket ativas |
| `GET` | `/api/ws/rooms` | Lista salas de chat ativas |
| `POST` | `/api/ws/broadcast` | Envia mensagem para todos |

---

## ✅ Critérios de Aceite

- [ ] WebSocket conectando e mantendo sessão
- [ ] Broadcast de person events para todos os subscribers
- [ ] Chat room com múltiplos users
- [ ] Heartbeat/ping a cada 30s
- [ ] Tratamento de desconexão graceful
- [ ] Lista de sessões ativas via REST
- [ ] Página HTML de teste para WebSocket
- [ ] Teste de carga com múltiplas conexões simultâneas

---

## 🛠️ Implementar em

| Stack | Abordagem |
|---|---|
| **Spring Boot** | Spring WebSocket / `@ServerEndpoint` / STOMP |
| **Micronaut** | `@ServerWebSocket` |
| **Quarkus** | `quarkus-websockets-next` / `@WebSocket` |
| **Go** | `gorilla/websocket` ou `nhooyr.io/websocket` |

---

## 💡 Dicas

### Spring Boot (STOMP)
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}

// Notificar
messagingTemplate.convertAndSend("/topic/persons", personEvent);
```

### Go
```go
var upgrader = websocket.Upgrader{CheckOrigin: func(r *http.Request) bool { return true }}

func wsHandler(w http.ResponseWriter, r *http.Request) {
    conn, _ := upgrader.Upgrade(w, r, nil)
    defer conn.Close()
    
    hub.Register(conn)
    defer hub.Unregister(conn)
    
    for {
        _, msg, err := conn.ReadMessage()
        if err != nil { break }
        hub.Broadcast(msg)
    }
}
```

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `spring-boot-starter-websocket` |
| Micronaut | `micronaut-websocket` |
| Quarkus | `quarkus-websockets-next` |
| Go | `github.com/gorilla/websocket` |

---

## 🔗 Referências

- [WebSocket Protocol (RFC 6455)](https://datatracker.ietf.org/doc/html/rfc6455)
- [Spring WebSocket Guide](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [STOMP Protocol](https://stomp.github.io/)
