package ru.kata.spring.boot_security.demo.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.kata.spring.boot_security.demo.models.Role;
import ru.kata.spring.boot_security.demo.models.User;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;

import java.util.*;
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
        return "admin-panel";
    }

    @GetMapping("/new")
    public String getNewUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleService.findAll());
        return "new-user";
    }

    @GetMapping("/user")
    public String getPage() {
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


    @PostMapping("/add")
    public String addUser(@Valid @ModelAttribute("newUser") User user,
                          @RequestParam(required = false) List<Long> roleIds,  // Принимаем ID ролей
                          BindingResult bindingResult,
                          Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userService.getAllUsers());
            return "admin-panel";
        }

        if (roleIds != null) {
            Set<Role> roles = roleIds.stream()
                    .map(roleService::findById)  // Находим Role по ID
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        try {
            userService.saveUser(user);
        } catch (DataIntegrityViolationException e) {
            bindingResult.rejectValue("username", "user.exists", "Username уже существует");
            model.addAttribute("users", userService.getAllUsers());
            return "admin-panel";
        }

        return "redirect:/admin?success=user_added";
    }

    @PostMapping("/update")
    public String updateUser(

            @RequestParam Long id,
            @RequestParam String username,
            @RequestParam(required = false) String password,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam int age,
            @RequestParam String email,
            @RequestParam List<Long> roleIds,
            RedirectAttributes redirectAttributes) {

        try {
            logger.debug("Starting user update for ID: {}", id);

            User user = userService.findById(id);

            // Логирование полученных данных
            logger.debug("Updating user: {}", user);
            logger.debug("Roles IDs received: {}", roleIds);

            // Обновление данных
            user.setUsername(username);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setAge(age);
            user.setEmail(email);

            if (password != null && !password.isEmpty()) {
                user.setPassword(passwordEncoder.encode(password));
            }

            Set<Role> roles = roleService.findAllByIds(roleIds);
            user.setRoles(roles);

            userService.updateUser(user);

            logger.info("User updated successfully: {}", user.getId());
            redirectAttributes.addFlashAttribute("success", "User updated successfully");

        } catch (Exception e) {
            logger.error("Error updating user", e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());

            // Для отладки - сохраняем ввод пользователя
            redirectAttributes.addFlashAttribute("userInput", Map.of(
                    "id", id,
                    "username", username,
                    "email", email,
                    "roleIds", roleIds
            ));
        }

        return "redirect:/admin";
    }
}
