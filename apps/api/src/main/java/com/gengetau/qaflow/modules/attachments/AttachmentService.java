package com.gengetau.qaflow.modules.attachments;

import com.gengetau.qaflow.modules.activity.ActivityLog;
import com.gengetau.qaflow.modules.activity.ActivityLogRepository;
import com.gengetau.qaflow.modules.attachments.dto.AttachmentResponse;
import com.gengetau.qaflow.modules.defects.Defect;
import com.gengetau.qaflow.modules.defects.DefectRepository;
import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.projects.ProjectService;
import com.gengetau.qaflow.modules.test_runs.TestRunItem;
import com.gengetau.qaflow.modules.test_runs.TestRunItemRepository;
import com.gengetau.qaflow.modules.users.User;
import com.gengetau.qaflow.modules.users.UserRepository;
import com.gengetau.qaflow.security.CurrentUser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttachmentService {

  private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/png", "image/jpeg", "image/webp", "application/pdf", "text/plain");

  private final AttachmentRepository attachmentRepository;
  private final ProjectService projectService;
  private final DefectRepository defectRepository;
  private final TestRunItemRepository testRunItemRepository;
  private final UserRepository userRepository;
  private final ActivityLogRepository activityLogRepository;
  private final Path uploadRoot;

  public AttachmentService(
      AttachmentRepository attachmentRepository,
      ProjectService projectService,
      DefectRepository defectRepository,
      TestRunItemRepository testRunItemRepository,
      UserRepository userRepository,
      ActivityLogRepository activityLogRepository,
      @Value("${qaflow.upload-root}") String uploadRoot) {
    this.attachmentRepository = attachmentRepository;
    this.projectService = projectService;
    this.defectRepository = defectRepository;
    this.testRunItemRepository = testRunItemRepository;
    this.userRepository = userRepository;
    this.activityLogRepository = activityLogRepository;
    this.uploadRoot = Path.of(uploadRoot);
  }

  @Transactional
  public AttachmentResponse upload(
      CurrentUser currentUser, UUID projectId, UUID defectId, UUID testRunItemId, MultipartFile file) {
    Project project = projectService.requireProjectForTestArtifactWrite(projectId, currentUser);
    User actor = requireUser(currentUser.id());
    validateFile(file);
    validateTarget(defectId, testRunItemId);
    Defect defect = defectId == null ? null : requireDefectInProject(defectId, projectId);
    TestRunItem testRunItem = testRunItemId == null ? null : requireRunItemInProject(testRunItemId, projectId);

    String fileName = cleanFileName(file.getOriginalFilename());
    String contentType = file.getContentType().toLowerCase(Locale.ROOT);
    String targetFolder = defect != null ? "defects" : testRunItem != null ? "test-run-items" : "project";
    String storedName = UUID.randomUUID() + "-" + fileName;
    Path relativePath =
        Path.of(
            "workspace-" + project.getWorkspace().getId(),
            "project-" + project.getId(),
            targetFolder,
            storedName);

    writeFile(file, relativePath);

    Attachment attachment =
        attachmentRepository.save(
            new Attachment(
                project,
                defect,
                testRunItem,
                actor,
                fileName,
                contentType,
                storagePath(relativePath),
                file.getSize()));
    activityLogRepository.save(
        new ActivityLog(
            project.getWorkspace(), project, actor, "ATTACHMENT", attachment.getId(), "ATTACHMENT_UPLOADED"));
    return toResponse(attachment);
  }

  @Transactional(readOnly = true)
  public List<AttachmentResponse> listByDefect(CurrentUser currentUser, UUID defectId) {
    Defect defect = requireDefect(defectId);
    projectService.requireProjectForRead(defect.getProject().getId(), currentUser);
    return attachmentRepository.findByDefect_IdOrderByCreatedAtAsc(defectId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<AttachmentResponse> listByTestRunItem(CurrentUser currentUser, UUID itemId) {
    TestRunItem item = requireRunItem(itemId);
    projectService.requireProjectForRead(item.getTestRun().getProject().getId(), currentUser);
    return attachmentRepository.findByTestRunItem_IdOrderByCreatedAtAsc(itemId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public AttachmentDownload download(CurrentUser currentUser, UUID attachmentId) {
    Attachment attachment =
        attachmentRepository
            .findDetailedById(attachmentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment was not found"));
    projectService.requireProjectForRead(attachment.getProject().getId(), currentUser);

    Path filePath = uploadRootPath().resolve(attachment.getStoragePath()).normalize();
    if (!filePath.startsWith(uploadRootPath()) || !Files.isRegularFile(filePath)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment file was not found");
    }
    return new AttachmentDownload(toResponse(attachment), new FileSystemResource(filePath));
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 10 MB");
    }
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File type is not allowed");
    }
  }

  private void validateTarget(UUID defectId, UUID testRunItemId) {
    boolean hasDefect = defectId != null;
    boolean hasRunItem = testRunItemId != null;
    if (hasDefect == hasRunItem) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Attachment must target exactly one defect or test run item");
    }
  }

  private Defect requireDefectInProject(UUID defectId, UUID projectId) {
    Defect defect = requireDefect(defectId);
    if (!defect.getProject().getId().equals(projectId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Defect does not belong to the project");
    }
    return defect;
  }

  private Defect requireDefect(UUID defectId) {
    return defectRepository
        .findDetailedById(defectId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Defect was not found"));
  }

  private TestRunItem requireRunItemInProject(UUID itemId, UUID projectId) {
    TestRunItem item = requireRunItem(itemId);
    if (!item.getTestRun().getProject().getId().equals(projectId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Run item does not belong to the project");
    }
    return item;
  }

  private TestRunItem requireRunItem(UUID itemId) {
    return testRunItemRepository
        .findDetailedById(itemId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test run item was not found"));
  }

  private User requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(User::isActive)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not active"));
  }

  private void writeFile(MultipartFile file, Path relativePath) {
    Path destination = uploadRootPath().resolve(relativePath).normalize();
    if (!destination.startsWith(uploadRootPath())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path");
    }

    try {
      Files.createDirectories(destination.getParent());
      try (InputStream inputStream = file.getInputStream()) {
        Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Attachment could not be stored", exception);
    }
  }

  private Path uploadRootPath() {
    return uploadRoot.toAbsolutePath().normalize();
  }

  private String cleanFileName(String originalFileName) {
    String original = originalFileName == null || originalFileName.isBlank() ? "evidence" : originalFileName;
    String normalized = original.replace("\\", "/");
    String leaf = normalized.substring(normalized.lastIndexOf('/') + 1);
    String cleaned = leaf.replaceAll("[^A-Za-z0-9._ -]", "_").replace("..", ".").trim();
    if (cleaned.isBlank()) {
      cleaned = "evidence";
    }
    return cleaned.length() <= 255 ? cleaned : cleaned.substring(cleaned.length() - 255);
  }

  private String storagePath(Path relativePath) {
    return relativePath.toString().replace('\\', '/');
  }

  private AttachmentResponse toResponse(Attachment attachment) {
    return new AttachmentResponse(
        attachment.getId(),
        attachment.getProject().getId(),
        attachment.getDefect() == null ? null : attachment.getDefect().getId(),
        attachment.getTestRunItem() == null ? null : attachment.getTestRunItem().getId(),
        attachment.getUploadedBy().getId(),
        attachment.getFileName(),
        attachment.getContentType(),
        attachment.getFileSize(),
        attachment.getCreatedAt());
  }

  public record AttachmentDownload(AttachmentResponse attachment, Resource resource) {}
}
