package com.smartlearning.system.user.repository;

import com.smartlearning.system.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
