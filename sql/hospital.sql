/*
 Navicat Premium Dump SQL

 Source Server         : mysql-base
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : hospital

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 20/07/2026 17:02:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_consult_record
-- ----------------------------
DROP TABLE IF EXISTS `ai_consult_record`;
CREATE TABLE `ai_consult_record`  (
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `patient_id` bigint NOT NULL COMMENT '患者ID',
  `conversation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI 对话内容(JSON)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`session_id`) USING BTREE,
  INDEX `idx_ai_consult_patient_id`(`patient_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 问诊记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for appointment
-- ----------------------------
DROP TABLE IF EXISTS `appointment`;
CREATE TABLE `appointment`  (
  `appointment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `patient_id` bigint NOT NULL COMMENT '患者ID',
  `doctor_id` bigint NOT NULL COMMENT '医生ID',
  `schedule_id` bigint NOT NULL COMMENT '排班ID',
  `appointment_date` date NOT NULL COMMENT '预约日期',
  `time_slot` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '预约时间段',
  `is_revisit` tinyint NOT NULL DEFAULT 0 COMMENT '是否为复诊(0-初诊,1-复诊)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '预约状态(0-待就诊,1-已就诊,2-已取消等)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`appointment_id`) USING BTREE,
  INDEX `idx_appointment_patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `idx_appointment_doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `idx_appointment_schedule_id`(`schedule_id` ASC) USING BTREE,
  INDEX `idx_appointment_date`(`appointment_date` ASC) USING BTREE,
  INDEX `idx_appointment_status`(`status` ASC) USING BTREE,
  INDEX `idx_appointment_is_revisit`(`is_revisit` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '预约挂号表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bill
-- ----------------------------
DROP TABLE IF EXISTS `bill`;
CREATE TABLE `bill`  (
  `bill_id` bigint NOT NULL AUTO_INCREMENT COMMENT '账单ID',
  `appointment_id` bigint NULL DEFAULT NULL COMMENT '关联预约ID',
  `patient_id` bigint NOT NULL COMMENT '患者ID',
  `total_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
  `paid_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '已付金额',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态(0-未支付,1-部分支付,2-已支付)',
  `payment_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付方式(现金/微信/支付宝/医保卡)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`bill_id`) USING BTREE,
  UNIQUE INDEX `uniq_bill_appointment_id`(`appointment_id` ASC) USING BTREE,
  INDEX `idx_bill_patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `idx_bill_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bill_item
-- ----------------------------
DROP TABLE IF EXISTS `bill_item`;
CREATE TABLE `bill_item`  (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '账单明细ID',
  `bill_id` bigint NOT NULL COMMENT '账单ID',
  `item_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '费用类型(registration/consultation/examination/drug)',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目名称',
  `unit_price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '单价',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '小计金额',
  `related_id` bigint NULL DEFAULT NULL COMMENT '关联业务ID(如drug_id, prescription_id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`item_id`) USING BTREE,
  INDEX `idx_bi_bill_id`(`bill_id` ASC) USING BTREE,
  INDEX `idx_bi_item_type`(`item_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账单明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for clinic
-- ----------------------------
DROP TABLE IF EXISTS `clinic`;
CREATE TABLE `clinic`  (
  `clinic_id` bigint NOT NULL AUTO_INCREMENT COMMENT '门诊ID',
  `dept_id` bigint NOT NULL COMMENT '所属科室ID',
  `clinic_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '门诊名称',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '是否有效(0-无效,1-有效)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`clinic_id`) USING BTREE,
  UNIQUE INDEX `uniq_clinic_name_dept_id`(`clinic_name` ASC, `dept_id` ASC) USING BTREE,
  INDEX `idx_clinic_dept_id`(`dept_id` ASC) USING BTREE,
  INDEX `idx_clinic_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门诊表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for department
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department`  (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '科室ID',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '科室名称',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '是否有效(0-无效,1-有效)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`dept_id`) USING BTREE,
  UNIQUE INDEX `uniq_department_dept_name`(`dept_name` ASC) USING BTREE,
  INDEX `idx_department_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '医院科室表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for diagnosis
-- ----------------------------
DROP TABLE IF EXISTS `diagnosis`;
CREATE TABLE `diagnosis`  (
  `diag_id` bigint NOT NULL AUTO_INCREMENT COMMENT '诊断记录ID',
  `appointment_id` bigint NOT NULL COMMENT '预约ID',
  `doctor_id` bigint NOT NULL COMMENT '医生ID',
  `patient_id` bigint NOT NULL COMMENT '患者ID',
  `diagnosis_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '诊断结果',
  `examination` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '检查记录',
  `prescription` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '处方信息(药品、数量、用法等)',
  `advice` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '医嘱',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`diag_id`) USING BTREE,
  UNIQUE INDEX `uniq_diagnosis_appointment_id`(`appointment_id` ASC) USING BTREE,
  INDEX `idx_diagnosis_doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `idx_diagnosis_patient_id`(`patient_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '医生诊断记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for doctor
-- ----------------------------
DROP TABLE IF EXISTS `doctor`;
CREATE TABLE `doctor`  (
  `doctor_id` bigint NOT NULL AUTO_INCREMENT COMMENT '医生ID',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '医生姓名',
  `clinic_id` bigint NOT NULL COMMENT '所属门诊ID',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职称(主任医师,副主任医师等)',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '医生简介',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`doctor_id`) USING BTREE,
  INDEX `idx_doctor_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_doctor_name`(`name` ASC) USING BTREE,
  INDEX `idx_doctor_clinic_id`(`clinic_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '医生信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for drug
-- ----------------------------
DROP TABLE IF EXISTS `drug`;
CREATE TABLE `drug`  (
  `drug_id` bigint NOT NULL AUTO_INCREMENT COMMENT '药品ID',
  `drug_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '药品名称',
  `specification` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格(如0.25g*12片)',
  `manufacturer` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生产厂家',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '盒' COMMENT '单位(盒/瓶/支)',
  `unit_price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '单价',
  `stock_quantity` int NOT NULL DEFAULT 0 COMMENT '库存数量',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '药品描述/说明',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '是否有效(0-无效,1-有效)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`drug_id`) USING BTREE,
  INDEX `idx_drug_name`(`drug_name` ASC) USING BTREE,
  INDEX `idx_drug_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品库存表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `notification_id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收用户ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知内容',
  `type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知类型(appointment_reminder/status_change/system)',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读(0-未读,1-已读)',
  `related_id` bigint NULL DEFAULT NULL COMMENT '关联业务ID(如appointment_id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`notification_id`) USING BTREE,
  INDEX `idx_notif_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_notif_is_read`(`is_read` ASC) USING BTREE,
  INDEX `idx_notif_type`(`type` ASC) USING BTREE,
  INDEX `idx_notif_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for patient
-- ----------------------------
DROP TABLE IF EXISTS `patient`;
CREATE TABLE `patient`  (
  `patient_id` bigint NOT NULL AUTO_INCREMENT COMMENT '患者ID',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '患者姓名',
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  `gender` tinyint NOT NULL DEFAULT 0 COMMENT '性别(0-未知,1-男,2-女)',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `region` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地区(省市区)',
  `address` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '详细住址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`patient_id`) USING BTREE,
  INDEX `idx_patient_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_patient_name`(`name` ASC) USING BTREE,
  INDEX `idx_patient_id_card`(`id_card` ASC) USING BTREE,
  INDEX `idx_patient_region`(`region` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '患者信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for prescription
-- ----------------------------
DROP TABLE IF EXISTS `prescription`;
CREATE TABLE `prescription`  (
  `prescription_id` bigint NOT NULL AUTO_INCREMENT COMMENT '处方ID',
  `diagnosis_id` bigint NULL DEFAULT NULL COMMENT '诊断记录ID(可为空，支持独立续方)',
  `doctor_id` bigint NOT NULL COMMENT '开方医生ID',
  `patient_id` bigint NOT NULL COMMENT '患者ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '处方状态(0-草稿,1-已提交,2-已发药)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`prescription_id`) USING BTREE,
  INDEX `idx_prescription_diagnosis_id`(`diagnosis_id` ASC) USING BTREE,
  INDEX `idx_prescription_doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `idx_prescription_patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `idx_prescription_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处方表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for prescription_detail
-- ----------------------------
DROP TABLE IF EXISTS `prescription_detail`;
CREATE TABLE `prescription_detail`  (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `prescription_id` bigint NOT NULL COMMENT '处方ID',
  `drug_id` bigint NOT NULL COMMENT '药品ID',
  `unit_price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '开方时药品单价',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `dosage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单次用量(如0.25g/1片)',
  `usage_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用法(口服/外用/静脉注射等)',
  `frequency` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '频率(每日3次/每日1次等)',
  `days` int NULL DEFAULT 1 COMMENT '天数',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`detail_id`) USING BTREE,
  INDEX `idx_pd_prescription_id`(`prescription_id` ASC) USING BTREE,
  INDEX `idx_pd_drug_id`(`drug_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处方明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for schedule
-- ----------------------------
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule`  (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '排班ID',
  `doctor_id` bigint NOT NULL COMMENT '医生ID',
  `clinic_id` bigint NOT NULL COMMENT '门诊ID',
  `schedule_date` date NOT NULL COMMENT '排班日期',
  `time_slot` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '时间段(如 08:00-12:00)',
  `max_patients` int NOT NULL DEFAULT 10 COMMENT '该时段可挂号最大人数',
  `current_patients` int NOT NULL DEFAULT 0 COMMENT '当前已预约人数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '排班状态(0-无效,1-有效)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`schedule_id`) USING BTREE,
  UNIQUE INDEX `uniq_schedule_doctor_date_time`(`doctor_id` ASC, `schedule_date` ASC, `time_slot` ASC) USING BTREE,
  INDEX `idx_schedule_doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `idx_schedule_clinic_id`(`clinic_id` ASC) USING BTREE,
  INDEX `idx_schedule_date`(`schedule_date` ASC) USING BTREE,
  INDEX `idx_schedule_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 103 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '医生排班表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码(加密后)',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `role` tinyint NOT NULL DEFAULT 0 COMMENT '用户角色(0-患者,1-医生,2-管理员)',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像URL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_username`(`username` ASC) USING BTREE,
  INDEX `idx_user_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_user_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户基本信息表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
