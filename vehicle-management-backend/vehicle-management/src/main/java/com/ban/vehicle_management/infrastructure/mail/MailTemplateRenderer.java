package com.ban.vehicle_management.infrastructure.mail;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Component
public class MailTemplateRenderer {

    private final TemplateEngine templateEngine;
    private final VehicleMailProperties mailProperties;

    public MailTemplateRenderer(TemplateEngine templateEngine, VehicleMailProperties mailProperties) {
        this.templateEngine = templateEngine;
        this.mailProperties = mailProperties;
    }

    public String render(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariable("brandName", "CoParking");
        context.setVariable("portalUrl", mailProperties.getPortalUrl());
        variables.forEach(context::setVariable);
        return templateEngine.process(templateName, context);
    }
}
