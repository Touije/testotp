package com.exemple.testotp.mapper;


import com.exemple.testotp.dto.UserRegistrationDto;
import com.exemple.testotp.entity.User;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    @Autowired
    private ModelMapper modelMapper;



    public User toEntity(UserRegistrationDto dto) {
        return modelMapper.map(dto, User.class);
    }

    public UserRegistrationDto toDto(User user) {
        return modelMapper.map(user, UserRegistrationDto.class);
    }
}
