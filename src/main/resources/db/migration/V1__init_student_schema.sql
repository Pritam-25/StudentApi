CREATE TABLE IF NOT EXISTS students (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL CONSTRAINT uk_student_email UNIQUE,
    password VARCHAR(255) NOT NULL,
    responsibility VARCHAR(255) NOT NULL CONSTRAINT students_responsibility_check CHECK (responsibility IN ('STUDENT', 'CLASS_REPRESENTATIVE')),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
