package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.CourseMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseChaperRepository extends JpaRepository<CourseMember, UUID> {

}
