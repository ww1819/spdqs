package com.qs.controller;

import com.qs.dto.ArchiveBriefDto;
import com.qs.dto.ArchiveOptionDto;
import com.qs.service.ArchiveService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/archives")
public class ArchiveApiController {

    private final ArchiveService archiveService;

    public ArchiveApiController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @GetMapping("/options")
    public List<ArchiveOptionDto> options() {
        return archiveService.listOptions();
    }

    @GetMapping("/{id}/brief")
    public ArchiveBriefDto brief(@PathVariable String id) {
        return archiveService.getBrief(id);
    }
}
