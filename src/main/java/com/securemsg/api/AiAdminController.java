package com.securemsg.api;

import com.securemsg.service.AiAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/admin")
public class AiAdminController {

    private final AiAdminService aiAdminService;

    public AiAdminController(AiAdminService aiAdminService) {
        this.aiAdminService = aiAdminService;
    }

    @GetMapping("/analysis")
    public String getSystemAnalysis() {
        return aiAdminService.analyzeSystem();
    }

    @PostMapping("/ask")
    public AnswerResponse askQuestion(@RequestBody QuestionRequest request) {
        String answer = aiAdminService.askQuestion(request.question());
        return new AnswerResponse(answer);
    }

    public record QuestionRequest(String question) {}
    public record AnswerResponse(String answer) {}
}
