package com.sheemab.shardedsagawallet.controller;

import com.sheemab.shardedsagawallet.dtos.UserRequestDto;
import com.sheemab.shardedsagawallet.entities.User;
import com.sheemab.shardedsagawallet.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

//    @PostMapping(path = "/create")
//    public ResponseEntity<User> createUser(@RequestBody UserRequestDto userDto) {
//        User newUser = userService.createUser(userDto);
//        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
//    }

    @GetMapping(path = "/id/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/name")
    public ResponseEntity<List<User>> searchUsersByName(@RequestParam String name) {
        List<User> users = userService.searchUsersByName(name);
        return ResponseEntity.ok(users);
    }
}
