package com.taskbridge.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Swagger UI em /swagger-ui.html — desligado no perfil prod. */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "TaskBridge API",
                version = "1.0",
                description = """
                        API REST do TaskBridge. Autenticação por sessão: faça POST /api/auth/login e o navegador
                        passa a enviar o cookie de sessão. Requisições POST/PUT/DELETE exigem o header X-XSRF-TOKEN
                        com o token obtido em GET /api/auth/csrf.
                        Erros seguem sempre o formato {status, message, timestamp, path}."""),
        security = @SecurityRequirement(name = "sessao"))
@SecurityScheme(name = "sessao", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE, paramName = "TBSESSION")
public class OpenApiConfig {
}
