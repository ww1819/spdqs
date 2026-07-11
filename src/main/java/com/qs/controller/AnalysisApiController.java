package com.qs.controller;

import com.qs.dto.CreateFlowNodeRequest;
import com.qs.dto.FlowNodeTreeDto;
import com.qs.dto.UpdateFlowNodeRequest;
import com.qs.service.AnalysisService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisApiController {

    private final AnalysisService analysisService;
    private final com.qs.service.UserService userService;

    public AnalysisApiController(AnalysisService analysisService,
                                 com.qs.service.UserService userService) {
        this.analysisService = analysisService;
        this.userService = userService;
    }

    @GetMapping("/{projectId}/tree")
    public FlowNodeTreeDto tree(@PathVariable String projectId) {
        return analysisService.getProjectTree(projectId);
    }

    @GetMapping(value = "/{projectId}/text", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public String text(@PathVariable String projectId) {
        return analysisService.exportProjectText(projectId);
    }

    @PostMapping("/nodes")
    public FlowNodeTreeDto createNode(@RequestBody CreateFlowNodeRequest request,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        return analysisService.createChildNode(
                request.getParentId(),
                request.getTitle(),
                resolveDisplayName(userDetails)
        );
    }

    @PutMapping("/nodes/{id}")
    public FlowNodeTreeDto updateNode(@PathVariable String id,
                                      @RequestBody UpdateFlowNodeRequest request) {
        return analysisService.updateNode(id, request.getTitle(), request.getDescription());
    }

    @DeleteMapping("/nodes/{id}")
    public FlowNodeTreeDto deleteNode(@PathVariable String id) {
        return analysisService.deleteNode(id);
    }

    private String resolveDisplayName(UserDetails userDetails) {
        if (userDetails == null) {
            return "";
        }
        var user = userService.findByUsername(userDetails.getUsername());
        return user != null ? user.getDisplayName() : userDetails.getUsername();
    }
}
