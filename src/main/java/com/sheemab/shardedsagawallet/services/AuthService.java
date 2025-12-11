package com.sheemab.shardedsagawallet.services;

import com.sheemab.shardedsagawallet.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;


}
