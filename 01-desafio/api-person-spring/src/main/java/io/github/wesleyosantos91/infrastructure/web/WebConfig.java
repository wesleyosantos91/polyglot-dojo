package io.github.wesleyosantos91.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Configura a serialização de Page<T> via PagedModel (DTO estável).
 * Elimina o warning:
 *   "For a stable JSON structure, please use Spring Data's PagedModel
 *    (globally via @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO))"
 *
 * Com VIA_DTO, a estrutura JSON é:
 * { "content": [...], "page": { "size": 10, "number": 0, "totalElements": N, "totalPages": M } }
 */
@Configuration(proxyBeanMethods = false)
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class WebConfig {
}