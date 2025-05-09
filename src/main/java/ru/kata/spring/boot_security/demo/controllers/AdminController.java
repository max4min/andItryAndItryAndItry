package ru.kata.spring.boot_security.demo.controllers;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.kata.spring.boot_security.demo.models.Role;
import ru.kata.spring.boot_security.demo.models.User;
import ru.kata.spring.boot_security.demo.models.UserDto;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {


    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);


    public AdminController(UserService userService, RoleService roleService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping()
    public String getAdminPanel(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("allRoles", roleService.findAll());
        model.addAttribute("newUser", new User());
        System.out.println("Роли из БД: " + roleService.findAll()); // отладка 167 строки
        return "admin-panel";
    }

    @GetMapping("/new")
    public String getNewUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleService.findAll());
        return "new-user";
    }

    @GetMapping("/user")
    public String getPage(){
        return "user";
    }

    @PostMapping
    public String createUser(@ModelAttribute("newUser") @Valid User user,
                             BindingResult bindingResult,
                             @RequestParam(value = "selectedRoles", required = false) List<String> roleNames,
                             Model model) {
        logger.info("Attempting to create user: {}", user);
        if (bindingResult.hasErrors()) {
            logger.error("Validation errors: {}", bindingResult.getAllErrors());
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("allRoles", roleService.findAll());
            return "admin-panel";
        }
        try {
            if (roleNames == null || roleNames.isEmpty()) {
                throw new IllegalArgumentException("At least one role must be selected");
            }
            Set<Role> roles = roleNames.stream()
                    .map(roleService::findByName)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            if (roles.isEmpty()) {
                throw new IllegalArgumentException("No valid roles selected");
            }
            user.setRoles(roles);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            logger.info("Saving user with roles: {}", roles);
            userService.saveUser(user);
            return "redirect:/admin";
        } catch (Exception e) {
            logger.error("Error creating user", e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("allRoles", roleService.findAll());
            return "admin-panel";
        }
    }

    @PostMapping("/update")
    public String updateUser(@ModelAttribute("user") @Valid User user,
                             BindingResult bindingResult,
                             @RequestParam("selectedRoles") List<String> roleNames) {
        if (bindingResult.hasErrors()) {
            return "admin-panel";
        }
        Set<Role> roles = new HashSet<>(roleService.findRolesByNameIn(roleNames));
        user.setRoles(roles);
        userService.updateUser(user);
        return "redirect:/admin";
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return "redirect:/admin";
    }



// отладка ниже. Настраиваем JSON
@PostMapping(value = "/users/api", consumes = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<?> createUserApi(@RequestBody @Valid UserDto userDto,
                                       BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
        return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
    }

    try {
        // Получаем роли по ID
        Set<Role> roles = roleService.findRolesByIds(userDto.getRoleIds());
        if (roles.isEmpty()) {
            return ResponseEntity.badRequest().body("At least one valid role ID must be provided");
        }

        // Создаём пользователя
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRoles(roles);

        userService.saveUser(user);
        return ResponseEntity.ok("User created successfully!");
    } catch (Exception e) {
        return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
    }
}

    @GetMapping(value = "/users/api", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<User>> getAllUsersApi() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @DeleteMapping("/users/api/{id}")
    public ResponseEntity<String> deleteUserApi(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted");
    }
    @PutMapping("/users/api/{id}")
    public ResponseEntity<?> updateUserApi(
            @PathVariable Long id,
            @RequestBody @Valid UserDto userDto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        try {
            User user = userService.findById(id).orElseThrow();

            // Обновляем данные
            user.setUsername(userDto.getUsername());
            user.setFirstName(userDto.getFirstName());
            user.setLastName(userDto.getLastName());
            user.setEmail(userDto.getEmail());

            // Обновляем пароль (если он был изменен)
            if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            }

            // Обновляем роли
            Set<Role> roles = roleService.findRolesByIds(userDto.getRoleIds());
            user.setRoles(roles);

            userService.updateUser(user);
            return ResponseEntity.ok("User updated successfully!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/users/api/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(user);
    }
}