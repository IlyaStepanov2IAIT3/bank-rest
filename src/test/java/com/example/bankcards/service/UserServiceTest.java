package com.example.bankcards.service;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;
    
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("Иван");
        user.setFullName("Иван Иванов");
        user.setEmail("ivan@example.com");
    }

    @Test
    void getAllUsers_shouldReturnMappedDtos() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDto> users = userService.getAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("Иван");
    }

    @Test
    void getUserById_existingId_shouldReturnDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto dto = userService.getUser(1L);

        assertThat(dto.getUsername()).isEqualTo("Иван");
    }

    @Test
    void getUserById_nonExistingId_shouldThrowException() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getUser(2L));
    }

    @Test
    void getUserByUsername_existingUsername_shouldReturnDto() {
        when(userRepository.findByUsername("Иван")).thenReturn(Optional.of(user));

        UserDto dto = userService.getUser("Иван");

        assertThat(dto.getUsername()).isEqualTo("Иван");
    }

    @Test
    void getUserByUsername_nonExistingUsername_shouldThrowException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getUser("unknown"));
    }

    @Test
    void deleteUserById_shouldCallRepository() {
        userService.deleteUser(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUserByUsername_shouldCallRepository() {
        userService.deleteUser("Иван");
        verify(userRepository, times(1)).deleteByUsername("Иван");
    }
}