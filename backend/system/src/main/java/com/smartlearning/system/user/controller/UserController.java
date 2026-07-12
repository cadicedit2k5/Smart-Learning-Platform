package com.smartlearning.system.user.controller;

import com.smartlearning.system.user.entity.User;
import com.smartlearning.system.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getUsers() {
        List<User> users = this.userService.handleGetUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> addUser(
            @ModelAttribute User user,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        if (avatar != null && !avatar.isEmpty()) {
            user.setAvatar(avatar.getOriginalFilename());
        }

        User createdUser = this.userService.handleAddUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

}
