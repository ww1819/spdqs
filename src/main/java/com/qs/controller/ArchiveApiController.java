package com.qs.controller;

import com.qs.dto.ArchiveBriefDto;
import com.qs.dto.ArchiveOptionDto;
import com.qs.service.ArchiveService;
import com.qs.service.PermissionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/archives")
public class ArchiveApiController {

    private final ArchiveService archiveService;
    private final PermissionService permissionService;

    public ArchiveApiController(ArchiveService archiveService, PermissionService permissionService) {
        this.archiveService = archiveService;
        this.permissionService = permissionService;
    }

    @GetMapping("/options")
    public List<ArchiveOptionDto> options(@AuthenticationPrincipal UserDetails userDetails) {
        Set<String> allowed = userDetails == null
                ? Set.of()
                : permissionService.getAllowedArchiveIds(userDetails.getUsername());
        return archiveService.listOptions(allowed);
    }

    @GetMapping("/{id}/brief")
    public ArchiveBriefDto brief(@PathVariable String id,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null || !permissionService.canAccessArchive(userDetails.getUsername(), id)) {
            throw new IllegalArgumentException("无权访问该医院/项目档案");
        }
        return archiveService.getBrief(id);
    }
}
