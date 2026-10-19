package com.taskbridge.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Lista completa de links (substitui a anterior). A validação de cada URL é regra de domínio (UrlPublica). */
public record LinksPerfilRequest(@NotNull @Size(max = 5) List<@Size(max = 300) String> links) {
}
