CREATE TABLE attachments (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    defect_id UUID REFERENCES defects(id) ON DELETE CASCADE,
    test_run_item_id UUID REFERENCES test_run_items(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_attachment_content_type CHECK (
        content_type IN ('image/png', 'image/jpeg', 'image/webp', 'application/pdf', 'text/plain')
    ),
    CONSTRAINT chk_attachment_link CHECK (
        (defect_id IS NOT NULL AND test_run_item_id IS NULL)
        OR (defect_id IS NULL AND test_run_item_id IS NOT NULL)
    )
);

CREATE INDEX idx_attachments_project ON attachments(project_id);
CREATE INDEX idx_attachments_defect ON attachments(defect_id);
CREATE INDEX idx_attachments_test_run_item ON attachments(test_run_item_id);
