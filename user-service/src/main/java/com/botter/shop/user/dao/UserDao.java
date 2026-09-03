package com.botter.shop.user.dao;

import com.botter.shop.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-31 12:45
 * @Description 描述信息
 */
public interface UserDao extends JpaRepository<User,Long> {
    User getUserByMobile(String mobile);
}
