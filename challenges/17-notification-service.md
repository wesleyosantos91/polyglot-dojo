# 🏆 Desafio 18 — Email & Notification Service

> **Nível:** ⭐⭐ Intermediário
> **Tipo:** Notifications · Email · SMS · Push · Template Engine
> **Estimativa:** 5–7 horas por stack

---

## 📋 Descrição

Criar um serviço de **notificações** que envia emails, SMS (mock) e push notifications quando eventos de Person ocorrem. Usar templates para os emails e processamento assíncrono via fila.

---

## 🎯 Objetivos de Aprendizado

- [ ] Template engine para emails (Thymeleaf, FreeMarker, Go templates)
- [ ] SMTP integration (MailHog para dev)
- [ ] Processamento assíncrono de notificações
- [ ] Notification preferences (opt-in/opt-out)
- [ ] Retry para falhas de envio
- [ ] Notification history e audit
- [ ] Rate limiting (evitar spam)
- [ ] Multi-channel (email, SMS, push)

---

## 📐 Especificação

### Canais de Notificação

| Canal | Provider | Descrição |
|---|---|---|
| Email | SMTP (MailHog) | Email com template HTML |
| SMS | Mock/Log | Simula envio SMS |
| In-App | Database | Notificação interna |

### Eventos → Notificações

| Evento | Email | SMS | In-App |
|---|---|---|---|
| Person cadastrada | ✅ Welcome email | ❌ | ✅ |
| Email atualizado | ✅ Confirmação | ✅ | ✅ |
| Person deletada | ✅ Goodbye email | ❌ | ✅ |
| Aniversário | ✅ Birthday email | ✅ | ✅ |

### Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/notifications/send` | Envia notificação manual |
| `GET` | `/api/notifications/{personId}` | Lista notificações da person |
| `PUT` | `/api/notifications/{id}/read` | Marca como lida |
| `GET` | `/api/notifications/preferences/{personId}` | Preferências |
| `PUT` | `/api/notifications/preferences/{personId}` | Atualiza preferências |

### Template de Email (Welcome)

```html
<!DOCTYPE html>
<html>
<body>
  <h1>Bem-vindo(a), {{name}}!</h1>
  <p>Seu cadastro foi realizado com sucesso.</p>
  <p>Email: {{email}}</p>
  <p>Data de cadastro: {{created_at}}</p>
  <footer>© 2026 Person API Workshop</footer>
</body>
</html>
```

### Tabela: notifications

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID | PK |
| `person_id` | Long | FK para person |
| `channel` | Enum | `EMAIL`, `SMS`, `IN_APP` |
| `type` | String | `WELCOME`, `BIRTHDAY`, etc. |
| `subject` | String | Assunto (email) |
| `body` | Text | Conteúdo renderizado |
| `status` | Enum | `PENDING`, `SENT`, `FAILED`, `READ` |
| `sent_at` | Timestamp | Quando enviou |
| `read_at` | Timestamp | Quando leu |
| `retry_count` | Int | Tentativas |
| `error_message` | String | Erro (se falhou) |

---

## ✅ Critérios de Aceite

- [ ] Email enviado com template HTML renderizado
- [ ] MailHog capturando emails em dev
- [ ] Notificação in-app salva no banco
- [ ] Fila assíncrona para processamento
- [ ] Retry (3x) para falhas de envio
- [ ] Preferências de notificação por person
- [ ] Histórico consultável via API
- [ ] Rate limit: max 10 emails/hora por person
- [ ] Job de aniversário rodando diariamente

---

## 🐳 Docker Compose (MailHog)

```yaml
services:
  mailhog:
    image: mailhog/mailhog:latest
    ports:
      - "1025:1025"  # SMTP
      - "8025:8025"  # Web UI
```

> Web UI: http://localhost:8025 — visualize todos os emails enviados.

---

## 🛠️ Implementar em

| Stack | Email | Templates |
|---|---|---|
| **Spring Boot** | `JavaMailSender` | Thymeleaf |
| **Micronaut** | `micronaut-email-javamail` | Micronaut Views |
| **Quarkus** | `quarkus-mailer` | Qute Templates |
| **Go** | `net/smtp` + `gomail` | `html/template` |

---

## 💡 Dicas

### Spring Boot
```java
@Service
public class EmailService {
    @Autowired private JavaMailSender mailSender;
    @Autowired private TemplateEngine templateEngine;
    
    public void sendWelcome(Person person) {
        Context ctx = new Context();
        ctx.setVariable("name", person.getName());
        String html = templateEngine.process("welcome", ctx);
        
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(person.getEmail());
        helper.setSubject("Bem-vindo!");
        helper.setText(html, true);
        mailSender.send(msg);
    }
}
```

### Go
```go
func sendEmail(to, subject, body string) error {
    m := gomail.NewMessage()
    m.SetHeader("From", "noreply@workshop.com")
    m.SetHeader("To", to)
    m.SetHeader("Subject", subject)
    m.SetBody("text/html", body)
    
    d := gomail.NewDialer("localhost", 1025, "", "")
    return d.DialAndSend(m)
}
```

---

## 📦 Dependências Extras

| Stack | Dependência |
|---|---|
| Spring | `spring-boot-starter-mail` + `spring-boot-starter-thymeleaf` |
| Micronaut | `micronaut-email-javamail` |
| Quarkus | `quarkus-mailer` |
| Go | `github.com/wneessen/go-mail` |

---

## 🔗 Referências

- [MailHog](https://github.com/mailhog/MailHog)
- [Spring Email Guide](https://spring.io/guides/gs/sending-email/)
- [Quarkus Mailer](https://quarkus.io/guides/mailer)
- [Thymeleaf](https://www.thymeleaf.org/)
