package com.qs.controller;

import com.qs.dto.ArchiveNodeStageDto;
import com.qs.dto.ArchiveNodeStageRequest;
import com.qs.service.ArchiveNodeStageService;
import com.qs.service.UserService;
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

import java.util.List;

@RestController
@RequestMapping("/api/archive-node-stages")
public class ArchiveNodeStageApiController {

    private final ArchiveNodeStageService stageService;
    private final UserService userService;

    public ArchiveNodeStageApiController(ArchiveNodeStageService stageService, UserService userService) {
        this.stageService = stageService;
        this.userService = userService;
    }

    @GetMapping
    public List<ArchiveNodeStageDto> list() {
        return stageService.listActive();
    }

    @PostMapping
    public List<ArchiveNodeStageDto> create(@RequestBody ArchiveNodeStageRequest request,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        return stageService.create(request, resolveDisplayName(userDetails));
    }

    @PutMapping("/reorder")
    public List<ArchiveNodeStageDto> reorder(@RequestBody List<String> orderedIds) {
        return stageService.reorder(orderedIds);
    }

    @PutMapping("/{id}")
    public List<ArchiveNodeStageDto> update(@PathVariable String id,
                                            @RequestBody ArchiveNodeStageRequest request) {
        return stageService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public List<ArchiveNodeStageDto> delete(@PathVariable String id) {
        return stageService.delete(id);
    }

    private String resolveDisplayName(UserDetails userDetails) {
        if (userDetails == null) {
            return "";
        }
        var user = userService.findByUsername(userDetails.getUsername());
        return user != null ? user.getDisplayName() : userDetails.getUsername();
    }
}
