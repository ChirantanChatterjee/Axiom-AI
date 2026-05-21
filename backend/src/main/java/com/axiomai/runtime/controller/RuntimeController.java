package com.axiomai.runtime.controller;

import com.axiomai.runtime.engine.RuntimeFlowExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/runtime")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RuntimeController {

    private final RuntimeFlowExecutor runtimeFlowExecutor;

    @PostMapping("/execute/{flowId}")
    public String executeFlow(

            @PathVariable
            Long flowId

    ) {

        runtimeFlowExecutor.executeFlow(flowId);

        return "Flow execution started";

    }

}