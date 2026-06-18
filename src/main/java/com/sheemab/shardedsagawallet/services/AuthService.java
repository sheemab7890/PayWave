package com.sheemab.shardedsagawallet.services;

import com.sheemab.shardedsagawallet.dtos.LoginRequestDto;
import com.sheemab.shardedsagawallet.dtos.LoginResponseDto;
import com.sheemab.shardedsagawallet.dtos.UserRequestDto;
import com.sheemab.shardedsagawallet.dtos.UserResponseDto;
import com.sheemab.shardedsagawallet.entities.User;
import com.sheemab.shardedsagawallet.entities.Wallet;
import com.sheemab.shardedsagawallet.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final WalletService walletService;

    @Transactional
    public UserResponseDto createUser(UserRequestDto userDto){

        // Email duplicate check
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered: " + userDto.getEmail());
        }

        log.info("Creating user: {}", userDto.getEmail());

        User newUser = User.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .build();

        User savedUser = userRepository.save(newUser);

       Wallet userWallet = walletService.createWallet(savedUser.getId()); // Create wallet for the saved user


        log.info("User created with ID {} in database shardwallet {} ",
                savedUser.getId(),
                (Math.abs(savedUser.getId().hashCode()) % 2 + 1));

        log.info("Wallet created for user {} with balance ₹0", savedUser.getId());

         return UserResponseDto.builder()
                 .id(savedUser.getId())
                 .name(savedUser.getName())
                 .email(savedUser.getEmail())
                 .balance(userWallet.getBalance())
                 .build();

    }


    /**
     * LOGIN:
     * 1. AuthenticationManager se verify karo (email + password)
     *    → Internally CustomUserDetailsService + BCrypt use hoga
     *    → Wrong credentials → exception throw hogi automatically
     * 2. Token generate karo
     * 3. Return karo
     */
    public LoginResponseDto login(LoginRequestDto request) {

        // Yeh ek line email + password dono verify karti hai
        // Agar galat hai toh BadCredentialsException throw hogi
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        log.info("[Auth] User logged in: {}", request.getEmail());

        String token = jwtService.generateToken(request.getEmail());

        LoginResponseDto response = new LoginResponseDto();
        response.setAccessToken(token);
        return response;
    }


}
