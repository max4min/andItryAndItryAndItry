package ru.kata.spring.boot_security.demo.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.models.Role;
import ru.kata.spring.boot_security.demo.models.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> getAllUsers();

    User getUserById(Long id);

    User getUserByUsername(String username);

    @Transactional(readOnly = true)
    User getUserByEmail(String email);

    @Transactional
    User saveUser(User user);

    void deleteUser(Long id);

    void updateUser(User user);//

    Optional<User> findByUsername(String username);

    @Transactional(readOnly = true)
    Optional<User> findByEmail(String email);

    User findById(Long id);

    Collection<? extends GrantedAuthority> mapRolesToAuthorities(Collection<Role> roles);

}
