package com.qs.service;

import com.qs.entity.User;
import com.qs.entity.UserArchivePerm;
import com.qs.entity.UserMenuPerm;
import com.qs.enums.MenuCode;
import com.qs.repository.ArchiveRepository;
import com.qs.repository.UserArchivePermRepository;
import com.qs.repository.UserMenuPermRepository;
import com.qs.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private final UserMenuPermRepository menuPermRepository;
    private final UserArchivePermRepository archivePermRepository;
    private final UserRepository userRepository;
    private final ArchiveRepository archiveRepository;

    public PermissionService(UserMenuPermRepository menuPermRepository,
                             UserArchivePermRepository archivePermRepository,
                             UserRepository userRepository,
                             ArchiveRepository archiveRepository) {
        this.menuPermRepository = menuPermRepository;
        this.archivePermRepository = archivePermRepository;
        this.userRepository = userRepository;
        this.archiveRepository = archiveRepository;
    }

    public Set<String> getMenuCodes(String userId) {
        return menuPermRepository.findByUserId(userId).stream()
                .map(UserMenuPerm::getMenuCode)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public Set<String> getMenuCodesByUsername(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return Set.of();
        }
        return getMenuCodes(user.getId());
    }

    public boolean hasMenu(String username, MenuCode menu) {
        if (username == null || menu == null) {
            return false;
        }
        User user = userRepository.findByUsername(username).orElse(null);
        return user != null && menuPermRepository.existsByUserIdAndMenuCode(user.getId(), menu.getCode());
    }

    public boolean hasMenuByUserId(String userId, MenuCode menu) {
        return userId != null && menu != null
                && menuPermRepository.existsByUserIdAndMenuCode(userId, menu.getCode());
    }

    /** 授权的医院/项目档案 ID；账号管理权限用户视为全部档案 */
    public Set<String> getAllowedArchiveIds(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return Set.of();
        }
        if (menuPermRepository.existsByUserIdAndMenuCode(user.getId(), MenuCode.USERS.getCode())) {
            return archiveRepository.findAll().stream()
                    .map(a -> a.getId())
                    .collect(Collectors.toCollection(HashSet::new));
        }
        return archivePermRepository.findByUserId(user.getId()).stream()
                .map(UserArchivePerm::getArchiveId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public Set<String> getAssignedArchiveIds(String userId) {
        return archivePermRepository.findByUserId(userId).stream()
                .map(UserArchivePerm::getArchiveId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Transactional
    public void grantArchiveToUser(String userId, String archiveId) {
        grantArchiveIfAbsent(userId, archiveId);
    }

    public boolean canAccessArchive(String username, String archiveId) {
        if (archiveId == null || archiveId.isBlank()) {
            return false;
        }
        return getAllowedArchiveIds(username).contains(archiveId);
    }

    @Transactional
    public void grantDefaultMenus(String userId) {
        for (MenuCode menu : MenuCode.defaultMenusForNewUser()) {
            grantMenuIfAbsent(userId, menu);
        }
    }

    @Transactional
    public void grantAllMenus(String userId) {
        for (MenuCode menu : MenuCode.allMenus()) {
            grantMenuIfAbsent(userId, menu);
        }
    }

    @Transactional
    public void grantAllArchives(String userId) {
        archiveRepository.findAll().forEach(archive -> grantArchiveIfAbsent(userId, archive.getId()));
    }

    @Transactional
    public void savePermissions(String userId, List<String> menuCodes, List<String> archiveIds) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在"));

        menuPermRepository.deleteByUserId(userId);
        archivePermRepository.deleteByUserId(userId);

        Set<String> menus = menuCodes == null ? Set.of() : new HashSet<>(menuCodes);
        for (String code : menus) {
            if (MenuCode.fromCode(code) != null) {
                UserMenuPerm perm = new UserMenuPerm();
                perm.setUserId(userId);
                perm.setMenuCode(code);
                menuPermRepository.save(perm);
            }
        }

        Set<String> archives = archiveIds == null ? Set.of() : new HashSet<>(archiveIds);
        for (String archiveId : archives) {
            if (archiveId != null && !archiveId.isBlank()
                    && archiveRepository.existsById(archiveId)) {
                UserArchivePerm perm = new UserArchivePerm();
                perm.setUserId(userId);
                perm.setArchiveId(archiveId);
                archivePermRepository.save(perm);
            }
        }
    }

    @Transactional
    public void ensureBootstrapPermissions() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (menuPermRepository.countByUserId(user.getId()) == 0) {
                if ("王威".equals(user.getUsername())) {
                    grantAllMenus(user.getId());
                    grantAllArchives(user.getId());
                } else {
                    grantDefaultMenus(user.getId());
                }
            }
        }
    }

    private void grantMenuIfAbsent(String userId, MenuCode menu) {
        if (!menuPermRepository.existsByUserIdAndMenuCode(userId, menu.getCode())) {
            UserMenuPerm perm = new UserMenuPerm();
            perm.setUserId(userId);
            perm.setMenuCode(menu.getCode());
            menuPermRepository.save(perm);
        }
    }

    private void grantArchiveIfAbsent(String userId, String archiveId) {
        if (!archivePermRepository.existsByUserIdAndArchiveId(userId, archiveId)) {
            UserArchivePerm perm = new UserArchivePerm();
            perm.setUserId(userId);
            perm.setArchiveId(archiveId);
            archivePermRepository.save(perm);
        }
    }
}
