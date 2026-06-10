package com.qs.service;

import com.qs.entity.User;
import com.qs.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .roles("USER")
                .build();
    }

    public List<User> listAll() {
        return userRepository.findAllByOrderByCreateTimeDesc();
    }

    public User getById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在"));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Transactional
    public void register(String username, String displayName, String rawPassword) {
        if (userRepository.countByUsername(username) > 0) {
            throw new IllegalArgumentException("账户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void createUserIfAbsent(String username, String displayName, String rawPassword) {
        if (userRepository.countByUsername(username) == 0) {
            User user = new User();
            user.setUsername(username);
            user.setDisplayName(displayName);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setEnabled(true);
            userRepository.save(user);
        }
    }

    @Transactional
    public void updateUser(String id, String displayName, boolean enabled, String newPassword,
                           String operatorUsername) {
        User user = getById(id);
        if (operatorUsername != null && operatorUsername.equals(user.getUsername()) && !enabled) {
            throw new IllegalArgumentException("不能停用自己的账号");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("显示名不能为空");
        }
        user.setDisplayName(displayName.trim());
        user.setEnabled(enabled);
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 4) {
                throw new IllegalArgumentException("密码至少4位");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        userRepository.save(user);
    }

    @Transactional
    public void setEnabled(String id, boolean enabled, String operatorUsername) {
        updateUser(id, getById(id).getDisplayName(), enabled, null, operatorUsername);
    }

    @Transactional
    public void updateProfile(String username, String displayName, String newPassword, String confirmPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("显示名不能为空");
        }
        user.setDisplayName(displayName.trim());
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 4) {
                throw new IllegalArgumentException("密码至少4位");
            }
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("两次输入的密码不一致");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        userRepository.save(user);
    }
}
