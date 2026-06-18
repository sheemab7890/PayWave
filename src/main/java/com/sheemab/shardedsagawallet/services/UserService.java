package com.sheemab.shardedsagawallet.services;

import com.sheemab.shardedsagawallet.dtos.UserRequestDto;
import com.sheemab.shardedsagawallet.entities.User;
import com.sheemab.shardedsagawallet.repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@AllArgsConstructor
@Slf4j
@Service
public class UserService {

    private final UserRepository repo;

    public User getUserById(Long id){
        return repo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> searchUsersByName(String name) {
        return repo.findByNameContainingIgnoreCase(name);
    }

}
