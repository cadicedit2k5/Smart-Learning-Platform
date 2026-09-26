package com.smartlearning.core.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class CourseFeatureConfig {

    @Column(name = "feature_announcements_enabled", nullable = false)
    private boolean announcements = true;

    @Column(name = "feature_content_enabled", nullable = false)
    private boolean content = true;

    @Column(name = "feature_assignments_enabled", nullable = false)
    private boolean assignments = true;

    @Column(name = "feature_documents_enabled", nullable = false)
    private boolean documents = true;

    @Column(name = "feature_discussion_enabled", nullable = false)
    private boolean discussion = true;

    @Column(name = "feature_ai_tutor_enabled", nullable = false)
    private boolean aiTutor = true;
}