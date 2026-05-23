package com.axiomai.service;

import com.axiomai.api.response.MathResponse;
import com.axiomai.security.SensitiveLogSanitizer;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SymPyService {

    private final RestTemplate restTemplate =
            new RestTemplate();

    public MathResponse solve(String problem) {

        try {

            String url =
                    "http://localhost:5000/solve";

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            String json =
                    "{\"problem\":\"" + problem + "\"}";

            HttpEntity<String> request =
                    new HttpEntity<>(json, headers);

            ResponseEntity<MathResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            request,
                            MathResponse.class
                    );

            return response.getBody();

        } catch (Exception e) {

            System.out.println(
                    "[SYMPY SERVICE] Request failed: "
                            + SensitiveLogSanitizer.redact(
                            e.getMessage()
                    )
            );

            MathResponse error =
                    new MathResponse();

            error.setResult(
                    "SymPy engine connection failed."
            );

            return error;
        }
    }
}
