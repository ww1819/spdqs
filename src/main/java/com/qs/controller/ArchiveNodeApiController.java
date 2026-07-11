package com.qs.controller;

import com.qs.dto.ArchiveNodeDto;
import com.qs.dto.ArchiveNodeRequest;
import com.qs.service.ArchiveNodeService;
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
@RequestMapping("/api/archives")
public class ArchiveNodeApiController {

    private final ArchiveNodeService archiveNodeService;
    private final UserService userService;

    public ArchiveNodeApiController(ArchiveNodeService archiveNodeService, UserService userService) {
        this.archiveNodeService = archiveNodeService;
        this.userService = userService;
    }

    @GetMapping("/{archiveId}/nodes")
    public List<ArchiveNodeDto> list(@PathVariable String archiveId) {
        return archiveNodeService.listByArchiveId(archiveId);
    }

    @PostMapping("/{archiveId}/nodes")
    public List<ArchiveNodeDto> create(@PathVariable String archiveId,
                                       @RequestBody ArchiveNodeRequest request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        return archiveNodeService.create(archiveId, request, resolveDisplayName(userDetails));
    }

    @PutMapping("/{archiveId}/nodes/{nodeId}")
    public List<ArchiveNodeDto> update(@PathVariable String archiveId,
                                       @PathVariable String nodeId,
                                       @RequestBody ArchiveNodeRequest request) {
        return archiveNodeService.update(archiveId, nodeId, request);
    }

    @DeleteMapping("/{archiveId}/nodes/{nodeId}")
    public List<ArchiveNodeDto> delete(@PathVariable String archiveId,
                                       @PathVariable String nodeId) {
        return archiveNodeService.delete(archiveId, nodeId);
    }

    private String resolveDisplayName(UserDetails userDetails) {
        if (userDetails == null) {
            return "";
        }
        var user = userService.findByUsername(userDetails.getUsername());
        return user != null ? user.getDisplayName() : userDetails.getUsername();
    }
}
