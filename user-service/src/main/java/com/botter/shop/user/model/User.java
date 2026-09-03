package com.botter.shop.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@lombok.Getter
@lombok.Setter
@Entity
@Table(name = "user", schema = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 45)
    @NotNull
    @Column(name = "mobile", nullable = false, length = 45)
    private String mobile;

    @Size(max = 45)
    @NotNull
    @Column(name = "nickname", nullable = false, length = 45)
    private String nickname;

    @Size(max = 45)
    @NotNull
    @Column(name = "password", nullable = false, length = 45)
    @JsonIgnore
    private String password;

    @Size(max = 45)
    @Column(name = "salt", length = 45)
    @JsonIgnore
    private String salt;

    @Size(max = 45)
    @Column(name = "avatar", length = 45)
    private String avatar;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "create_time", nullable = false)
    private Instant createTime;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "update_time", nullable = false)
    private Instant updateTime;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "last_login_time", nullable = false)
    private Instant lastLoginTime;

    @ColumnDefault("'0'")
    @Column(name = "login_count", columnDefinition = "int UNSIGNED not null")
    private Long loginCount;

    @ColumnDefault("'0'")
    @Column(name = "point", columnDefinition = "int UNSIGNED not null")
    private Long point;


}
