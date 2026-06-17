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
    private final WalletService walletService;

    public User createUser(UserRequestDto userDto){
        log.info("Creating user: {}", userDto.getEmail());

        User newUser = User.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .password(userDto.getPassword()) // TODO Add password encoder
                .build();

       User savedUser = repo.save(newUser);

       walletService.creatdWallet(savedUser.getId()); // Create wallet for the saved user

        log.info("User created with ID {} in database shardwallet {} ", savedUser.getId(), (savedUser.getId() % 2 + 1));
        log.info("Wallet created for user {} with balance ₹0", savedUser.getId());

        return savedUser;
    }

    public User getUserById(Long id){
        return repo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> searchUsersByName(String name) {
        return repo.findByNameContainingIgnoreCase(name);
    }
}
