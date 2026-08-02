package com.digibank.controller;

import com.digibank.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller @RequestMapping("/admin")
public class AdminController {
	private final AuditLogRepository auditLogs;
	public AdminController(AuditLogRepository auditLogs){this.auditLogs=auditLogs;}
	@GetMapping("/dashboard") public String dashboard(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="") String q,Model model){
		int safePage=Math.max(0,page);var pageable=PageRequest.of(safePage,20,Sort.by(Sort.Direction.DESC,"occurredAt"));
		var logs=q.isBlank()?auditLogs.findAll(pageable):auditLogs.findByActorUsernameContainingIgnoreCaseOrActionContainingIgnoreCaseOrTargetIdentifierContainingIgnoreCase(q,q,q,pageable);
		model.addAttribute("logs",logs);model.addAttribute("query",q);model.addAttribute("auditCount",auditLogs.count());return "admin/dashboard";}
}
