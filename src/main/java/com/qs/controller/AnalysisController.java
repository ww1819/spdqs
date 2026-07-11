package com.qs.controller;

import com.qs.entity.AnalysisProject;
import com.qs.service.AnalysisService;
import com.qs.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final UserService userService;

    public AnalysisController(AnalysisService analysisService, UserService userService) {
        this.analysisService = analysisService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        populateCommon(model, userDetails);
        List<AnalysisProject> projects = analysisService.listProjects();
        model.addAttribute("projects", projects);
        return "analysis/list";
    }

    @GetMapping("/new")
    public String createForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        populateCommon(model, userDetails);
        model.addAttribute("project", new AnalysisProject());
        model.addAttribute("rootTitle", "登录");
        return "analysis/form";
    }

    @PostMapping("/new")
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false, defaultValue = "登录") String rootTitle,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            AnalysisProject project = analysisService.createProject(
                    name, description, rootTitle, resolveDisplayName(userDetails));
            redirectAttributes.addFlashAttribute("success", "项目创建成功");
            return "redirect:/analysis/" + project.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/analysis/new";
        }
    }

    @GetMapping("/{id}")
    public String tree(@PathVariable String id,
                       Model model,
                       @AuthenticationPrincipal UserDetails userDetails) {
        populateCommon(model, userDetails);
        AnalysisProject project = analysisService.getProject(id);
        model.addAttribute("project", project);
        return "analysis/tree";
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable String id) {
        AnalysisProject project = analysisService.getProject(id);
        String text = analysisService.exportProjectText(id);
        String filename = project.getName().replaceAll("[\\\\/:*?\"<>|]", "_") + "-流程说明.txt";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(text.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            analysisService.deleteProject(id);
            redirectAttributes.addFlashAttribute("success", "项目已删除");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/analysis";
    }

    private void populateCommon(Model model, UserDetails userDetails) {
        model.addAttribute("currentUser", resolveDisplayName(userDetails));
        model.addAttribute("activeTab", "analysis");
    }

    private String resolveDisplayName(UserDetails userDetails) {
        if (userDetails == null) {
            return "";
        }
        var user = userService.findByUsername(userDetails.getUsername());
        return user != null ? user.getDisplayName() : userDetails.getUsername();
    }
}
