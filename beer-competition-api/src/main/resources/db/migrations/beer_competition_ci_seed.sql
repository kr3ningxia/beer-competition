-- CI integration-test prerequisites only. Keep this file free of production data and credentials.
SET NAMES utf8mb4;

INSERT INTO `enterprise_account` (`id`, `account_code`, `name`, `status`)
VALUES (1, 'EA-PLATFORM', '啤酒事务局', 'ACTIVE');

INSERT INTO `organizer` (`id`, `enterprise_account_id`, `name`, `organizer_type`, `status`)
VALUES (1, 1, '啤酒事务局', 'PLATFORM', 'ACTIVE');

-- The random hash keeps this fixture usable for authorization tests without creating a login credential.
INSERT INTO `admin_user` (`id`, `username`, `password`, `name`, `status`, `admin_type`, `must_change_password`)
VALUES (1, 'ci-platform-admin', MD5(UUID()), 'CI 平台管理员', 1, 'PLATFORM_SUPER_ADMIN', 0);

INSERT INTO `style_library`
    (`id`, `code`, `name`, `version`, `language`, `source`, `status`, `organizer_id`, `visibility`)
VALUES
    (1, 'CI_STYLE_LIBRARY', 'CI 测试风格库', '1', '中文', 'CI', 1, 1, 'PUBLIC');

INSERT INTO `style_category` (`id`, `library_id`, `name`, `sort_order`)
VALUES (1, 1, 'CI 测试分类', 1);

INSERT INTO `style_item`
    (`id`, `library_id`, `category_id`, `name`, `style_code`, `description`, `status`, `sort_order`)
VALUES
    (1, 1, 1, 'CI 测试风格', 'CI-1', '仅用于集成测试', 1, 1);
