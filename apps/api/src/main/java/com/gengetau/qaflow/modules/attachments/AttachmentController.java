package com.gengetau.qaflow.modules.attachments;

import com.gengetau.qaflow.modules.attachments.AttachmentService.AttachmentDownload;
import com.gengetau.qaflow.modules.attachments.dto.AttachmentResponse;
import com.gengetau.qaflow.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class AttachmentController {

  private final AttachmentService attachmentService;

  public AttachmentController(AttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  AttachmentResponse upload(
      @AuthenticationPrincipal CurrentUser currentUser,
      @RequestParam UUID projectId,
      @RequestParam(required = false) UUID defectId,
      @RequestParam(required = false) UUID testRunItemId,
      @RequestPart MultipartFile file) {
    return attachmentService.upload(currentUser, projectId, defectId, testRunItemId, file);
  }

  @GetMapping("/defects/{defectId}/attachments")
  List<AttachmentResponse> listByDefect(
      @AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID defectId) {
    return attachmentService.listByDefect(currentUser, defectId);
  }

  @GetMapping("/test-run-items/{itemId}/attachments")
  List<AttachmentResponse> listByTestRunItem(
      @AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID itemId) {
    return attachmentService.listByTestRunItem(currentUser, itemId);
  }

  @GetMapping("/attachments/{attachmentId}/download")
  ResponseEntity<Resource> download(
      @AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID attachmentId) {
    AttachmentDownload download = attachmentService.download(currentUser, attachmentId);
    AttachmentResponse attachment = download.attachment();
    ContentDisposition disposition = ContentDisposition.attachment().filename(attachment.fileName()).build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(attachment.contentType()))
        .contentLength(attachment.fileSize())
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(download.resource());
  }
}
