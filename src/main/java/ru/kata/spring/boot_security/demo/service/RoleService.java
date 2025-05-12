package ru.kata.spring.boot_security.demo.service;

import org.springframework.stereotype.Service;
import ru.kata.spring.boot_security.demo.models.Role;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public interface RoleService {
    List<Role> findAll();

    void save(Role role);

    public Role findById(Long id);

    Optional<Role> findByName(String name);

    List<Role> findRolesByNameIn(List<String> roleNames);

    public Role findRoleById(Long id);

    public Set<Role> findRolesByIds(List<Long> ids);

    Set<Role> findRolesByIdIn(List<Long> ids);

    public Set<Role> findAllByIds (List<Long> ids);
    }

