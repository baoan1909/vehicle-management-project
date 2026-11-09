package com.ban.vehicle_management.entrypoint.controller.dev;

import com.ban.vehicle_management.application.dev.mailpreview.port.in.MailPreviewPortIn;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/mail-preview")
public class MailPreviewController {

    private static final String TEXT_HTML_UTF8 = "text/html;charset=UTF-8";

    private final MailPreviewPortIn mailPreviewPortIn;

    public MailPreviewController(MailPreviewPortIn mailPreviewPortIn) {
        this.mailPreviewPortIn = mailPreviewPortIn;
    }

    @GetMapping(produces = TEXT_HTML_UTF8)
    public String index() {
        return mailPreviewPortIn.index();
    }

    @GetMapping(value = "/{template}", produces = TEXT_HTML_UTF8)
    public String preview(@PathVariable String template) {
        return mailPreviewPortIn.preview(template);
    }
}
