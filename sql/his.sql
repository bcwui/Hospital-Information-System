-- 0. 创建数据库及使用
CREATE DATABASE IF NOT EXISTS `hospital`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `hospital`;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
  `password` VARCHAR(100) DEFAULT NULL COMMENT '密码(加密后)',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(50) DEFAULT NULL COMMENT '邮箱',
  `role` TINYINT NOT NULL DEFAULT 0 COMMENT '用户角色(0-患者,1-医生,2-管理员)',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '用户头像URL',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  -- 索引
  INDEX `idx_user_username` (`username`),
  INDEX `idx_user_phone` (`phone`),
  INDEX `idx_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基本信息表';

-- 2. 科室表
CREATE TABLE IF NOT EXISTS `department` (
  `dept_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '科室ID',
  `dept_name` VARCHAR(50) NOT NULL COMMENT '科室名称',
  `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否有效(0-无效,1-有效)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  -- 索引
  UNIQUE INDEX `uniq_department_dept_name` (`dept_name`),
  INDEX `idx_department_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院科室表';

-- 3. 门诊表
CREATE TABLE IF NOT EXISTS `clinic` (
  `clinic_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '门诊ID',
  `dept_id` BIGINT NOT NULL COMMENT '所属科室ID',
  `clinic_name` VARCHAR(50) NOT NULL COMMENT '门诊名称',
  `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否有效(0-无效,1-有效)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  -- 索引
  UNIQUE INDEX `uniq_clinic_name_dept_id` (`clinic_name`, `dept_id`),
  INDEX `idx_clinic_dept_id` (`dept_id`),
  INDEX `idx_clinic_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门诊表';

-- 4. 医生表
CREATE TABLE IF NOT EXISTS `doctor` (
  `doctor_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '医生ID',
  `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '医生姓名',
  `clinic_id` BIGINT NOT NULL COMMENT '所属门诊ID',
  `title` VARCHAR(50) DEFAULT NULL COMMENT '职称(主任医师,副主任医师等)',
  `introduction` TEXT DEFAULT NULL COMMENT '医生简介',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  -- 索引
  INDEX `idx_doctor_user_id` (`user_id`),
  INDEX `idx_doctor_name` (`name`),
  INDEX `idx_doctor_clinic_id` (`clinic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生信息表';

-- 5. 患者表
CREATE TABLE IF NOT EXISTS `patient` (
  `patient_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '患者ID',
  `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '患者姓名',
  `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
  `gender` TINYINT NOT NULL DEFAULT 0 COMMENT '性别(0-未知,1-男,2-女)',
  `age` INT DEFAULT NULL COMMENT '年龄',
  `region` VARCHAR(50) DEFAULT NULL COMMENT '地区(省市区)',
  `address` VARCHAR(100) DEFAULT NULL COMMENT '详细住址',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  -- 索引
  INDEX `idx_patient_user_id` (`user_id`),
  INDEX `idx_patient_name` (`name`),
  INDEX `idx_patient_id_card` (`id_card`),
  INDEX `idx_patient_region` (`region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者信息表';

-- 6. 医生排班表
CREATE TABLE IF NOT EXISTS `schedule` (
  `schedule_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '排班ID',
  `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
  `clinic_id` BIGINT NOT NULL COMMENT '门诊ID',
  `schedule_date` DATE NOT NULL COMMENT '排班日期',
  `time_slot` VARCHAR(20) NOT NULL COMMENT '时间段(如 08:00-12:00)',
  `max_patients` INT NOT NULL DEFAULT 10 COMMENT '该时段可挂号最大人数',
  `current_patients` INT NOT NULL DEFAULT 0 COMMENT '当前已预约人数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '排班状态(0-无效,1-有效)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  -- 索引
  UNIQUE INDEX `uniq_schedule_doctor_date_time` (`doctor_id`, `schedule_date`, `time_slot`),
  INDEX `idx_schedule_doctor_id` (`doctor_id`),
  INDEX `idx_schedule_clinic_id` (`clinic_id`),
  INDEX `idx_schedule_date` (`schedule_date`),
  INDEX `idx_schedule_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生排班表';

-- 7. 预约挂号表
CREATE TABLE IF NOT EXISTS `appointment` (
  `appointment_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预约ID',
  `patient_id` BIGINT NOT NULL COMMENT '患者ID',
  `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
  `schedule_id` BIGINT NOT NULL COMMENT '排班ID',
  `appointment_date` DATE NOT NULL COMMENT '预约日期',
  `time_slot` VARCHAR(20) NOT NULL COMMENT '预约时间段',
  `is_revisit` TINYINT NOT NULL DEFAULT 0 COMMENT '是否为复诊(0-初诊,1-复诊)',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '预约状态(0-待就诊,1-已就诊,2-已取消等)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  -- 索引
  INDEX `idx_appointment_patient_id` (`patient_id`),
  INDEX `idx_appointment_doctor_id` (`doctor_id`),
  INDEX `idx_appointment_schedule_id` (`schedule_id`),
  INDEX `idx_appointment_date` (`appointment_date`),
  INDEX `idx_appointment_status` (`status`),
  INDEX `idx_appointment_is_revisit` (`is_revisit`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约挂号表';

-- 8. AI 问诊记录表 (使用session_id作为主键，与挂号解耦)
CREATE TABLE IF NOT EXISTS `ai_consult_record` (
  `session_id` VARCHAR(64) PRIMARY KEY COMMENT '会话ID',
  `patient_id` BIGINT NOT NULL COMMENT '患者ID',
  `conversation` TEXT COMMENT 'AI 对话内容(JSON)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  -- 索引
  INDEX `idx_ai_consult_patient_id` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 问诊记录表';

-- 9. 医生诊断记录表
CREATE TABLE IF NOT EXISTS `diagnosis` (
  `diag_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '诊断记录ID',
  `appointment_id` BIGINT NOT NULL COMMENT '预约ID',
  `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
  `patient_id` BIGINT NOT NULL COMMENT '患者ID',
  `diagnosis_result` TEXT DEFAULT NULL COMMENT '诊断结果',
  `examination` TEXT DEFAULT NULL COMMENT '检查记录',
  `prescription` TEXT DEFAULT NULL COMMENT '处方信息(药品、数量、用法等)',
  `advice` TEXT DEFAULT NULL COMMENT '医嘱',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  -- 索引
  UNIQUE INDEX `uniq_diagnosis_appointment_id` (`appointment_id`),
  INDEX `idx_diagnosis_doctor_id` (`doctor_id`),
  INDEX `idx_diagnosis_patient_id` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生诊断记录表';
