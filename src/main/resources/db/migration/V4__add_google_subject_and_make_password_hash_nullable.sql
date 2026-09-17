-- Allow students registered via Google to have no initial local password
ALTER TABLE students
    ALTER COLUMN password_hash DROP NOT NULL;

-- Add Google Subject identifier column
ALTER TABLE students
    ADD COLUMN IF NOT EXISTS google_subject VARCHAR(255);

-- Ensure Google subject is unique across all accounts
ALTER TABLE students
    ADD CONSTRAINT uk_students_google_subject UNIQUE (google_subject);

-- Index for fast lookup during OAuth2 login callback
CREATE INDEX IF NOT EXISTS idx_students_google_subject ON students (google_subject);
