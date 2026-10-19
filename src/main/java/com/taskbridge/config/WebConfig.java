package com.taskbridge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;

/**
 * URLs "limpas" do front-end (/projects/12) apontando para páginas estáticas. Cada página lê
 * o id da própria URL e busca os dados na API — a página em si não contém dado nenhum.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Map<String, String> PAGINAS = Map.ofEntries(
            Map.entry("/login", "login"),
            Map.entry("/register", "register"),
            Map.entry("/register/complete", "register-social"),
            Map.entry("/dashboard", "dashboard"),
            Map.entry("/projects", "projects"),
            Map.entry("/projects/new", "project-form"),
            Map.entry("/projects/{id}", "project"),
            Map.entry("/projects/{id}/edit", "project-form"),
            Map.entry("/services", "services"),
            Map.entry("/services/new", "service-form"),
            Map.entry("/services/{id}/edit", "service-form"),
            Map.entry("/proposals", "proposals"),
            Map.entry("/contracts", "contracts"),
            Map.entry("/contracts/{id}", "contract"),
            Map.entry("/payment/{id}", "payment"),
            Map.entry("/profile", "profile"),
            Map.entry("/u/{nomeUsuario}", "profile"),
            Map.entry("/settings", "settings"),
            Map.entry("/security", "security"),
            Map.entry("/notifications", "notifications"),
            Map.entry("/privacy", "privacy"),
            Map.entry("/terms", "terms"));

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        PAGINAS.forEach((rota, pagina) -> registry.addViewController(rota).setViewName("forward:/view/" + pagina + ".html"));
    }
}
