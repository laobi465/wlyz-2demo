package com.jicek.license.enduser.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 终端用户新建/更新 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 新建时：id 为空，password 必填（BCrypt 哈希后存储）
 * 更新时：id 必填，password 为空表示不修改密码
 *
 * tenantId 由 Service 从 AuthContext 获取，前端禁传（防越权）
 */
@Data
public class EndUserSaveDTO {

    /** 主键（更新时必填，新建时为空） */
    private Long id;

    /** 关联软件ID */
    @NotNull(message = "软件ID不能为空")
    private Long softwareId;

    /** 登录用户名（同软件内唯一） */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度 3-64 字符")
    private String username;

    /** 密码明文（创建必填，更新可空表示不改） */
    @Size(max = 64, message = "密码最长 64 字符")
    private String password;

    /** 昵称 */
    @Size(max = 64, message = "昵称最长 64 字符")
    private String nickname;

    /** 邮箱 */
    @Email(message = "邮箱格式非法")
    @Size(max = 128, message = "邮箱最长 128 字符")
    private String email;

    /** 手机号 */
    @Size(max = 20, message = "手机号最长 20 字符")
    private String phone;

    /** 0封禁 1正常（可选，默认 1） */
    private Integer status;

    /** 备注 */
    @Size(max = 255, message = "备注最长 255 字符")
    private String remark;
}
