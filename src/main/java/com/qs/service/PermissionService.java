package com.qs.service;

import com.qs.entity.Delivery;
import com.qs.entity.PartnerDeliveryPerm;
import com.qs.entity.User;
import com.qs.entity.UserDeliveryPerm;
import com.qs.entity.UserMenuPerm;
import com.qs.enums.MenuCode;
import com.qs.repository.DeliveryRepository;
import com.qs.repository.PartnerDeliveryPermRepository;
import com.qs.repository.UserDeliveryPermRepository;
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
    private final UserDeliveryPermRepository deliveryPermRepository;
    private final PartnerDeliveryPermRepository partnerDeliveryPermRepository;
    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;

    public PermissionService(UserMenuPermRepository menuPermRepository,
                             UserDeliveryPermRepository deliveryPermRepository,
                             PartnerDeliveryPermRepository partnerDeliveryPermRepository,
                             UserRepository userRepository,
                             DeliveryRepository deliveryRepository) {
        this.menuPermRepository = menuPermRepository;
        this.deliveryPermRepository = deliveryPermRepository;
        this.partnerDeliveryPermRepository = partnerDeliveryPermRepository;
        this.userRepository = userRepository;
        this.deliveryRepository = deliveryRepository;
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

    /** 授权的产品交付 ID；账号管理权限用户视为全部交付；服务商账号叠加服务商授权 */
    public Set<String> getAllowedDeliveryIds(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return Set.of();
        }
        if (menuPermRepository.existsByUserIdAndMenuCode(user.getId(), MenuCode.USERS.getCode())) {
            return deliveryRepository.findAll().stream()
                    .map(Delivery::getId)
                    .collect(Collectors.toCollection(HashSet::new));
        }
        Set<String> ids = deliveryPermRepository.findByUserId(user.getId()).stream()
                .map(UserDeliveryPerm::getDeliveryId)
                .collect(Collectors.toCollection(HashSet::new));
        if (user.getPartnerId() != null && !user.getPartnerId().isBlank()) {
            partnerDeliveryPermRepository.findByPartnerId(user.getPartnerId()).stream()
                    .map(PartnerDeliveryPerm::getDeliveryId)
                    .forEach(ids::add);
        }
        return ids;
    }

    public Set<String> getAssignedDeliveryIds(String userId) {
        return deliveryPermRepository.findByUserId(userId).stream()
                .map(UserDeliveryPerm::getDeliveryId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Transactional
    public void grantDeliveryToUser(String userId, String deliveryId) {
        grantDeliveryIfAbsent(userId, deliveryId);
    }

    public boolean canAccessDelivery(String username, String deliveryId) {
        if (deliveryId == null || deliveryId.isBlank()) {
            return false;
        }
        return getAllowedDeliveryIds(username).contains(deliveryId);
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
    public void grantAllDeliveries(String userId) {
        deliveryRepository.findAll().forEach(d -> grantDeliveryIfAbsent(userId, d.getId()));
    }

    @Transactional
    public void savePermissions(String userId, List<String> menuCodes, List<String> deliveryIds) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在"));

        menuPermRepository.deleteByUserId(userId);
        deliveryPermRepository.deleteByUserId(userId);

        Set<String> menus = menuCodes == null ? Set.of() : new HashSet<>(menuCodes);
        for (String code : menus) {
            if (MenuCode.fromCode(code) != null) {
                UserMenuPerm perm = new UserMenuPerm();
                perm.setUserId(userId);
                perm.setMenuCode(code);
                menuPermRepository.save(perm);
            }
        }

        Set<String> deliveries = deliveryIds == null ? Set.of() : new HashSet<>(deliveryIds);
        for (String deliveryId : deliveries) {
            if (deliveryId != null && !deliveryId.isBlank()
                    && deliveryRepository.existsById(deliveryId)) {
                UserDeliveryPerm perm = new UserDeliveryPerm();
                perm.setUserId(userId);
                perm.setDeliveryId(deliveryId);
                deliveryPermRepository.save(perm);
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
                    grantAllDeliveries(user.getId());
                } else {
                    grantDefaultMenus(user.getId());
                }
            } else {
                // 存量账号：有档案权限则补使用单位；有账号管理则补服务商
                if (menuPermRepository.existsByUserIdAndMenuCode(user.getId(), MenuCode.ARCHIVES.getCode())) {
                    grantMenuIfAbsent(user.getId(), MenuCode.CUSTOMERS);
                }
                if (menuPermRepository.existsByUserIdAndMenuCode(user.getId(), MenuCode.USERS.getCode())) {
                    grantMenuIfAbsent(user.getId(), MenuCode.CUSTOMERS);
                    grantMenuIfAbsent(user.getId(), MenuCode.PARTNERS);
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

    private void grantDeliveryIfAbsent(String userId, String deliveryId) {
        if (!deliveryPermRepository.existsByUserIdAndDeliveryId(userId, deliveryId)) {
            UserDeliveryPerm perm = new UserDeliveryPerm();
            perm.setUserId(userId);
            perm.setDeliveryId(deliveryId);
            deliveryPermRepository.save(perm);
        }
    }
}
