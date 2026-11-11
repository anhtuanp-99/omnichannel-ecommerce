package com.omni.notification.email;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class TemplateService {

    private final TemplateEngine engine;

    public TemplateService(TemplateEngine engine) {
        this.engine = engine;
    }

    public String render(String name, Map<String, Object> model) {
        Context ctx = new Context();
        ctx.setVariables(model);
        return engine.process("email/" + name, ctx);
    }
}