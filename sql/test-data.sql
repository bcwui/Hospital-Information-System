-- ============================================================
-- HIS 测试数据
-- 先执行 hospital.sql 建表，再执行本文件（hospital.sql 已含 DROP TABLE IF EXISTS）
-- 所有账号密码：123456
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE bill_item;
TRUNCATE TABLE bill;
TRUNCATE TABLE prescription_detail;
TRUNCATE TABLE prescription;
TRUNCATE TABLE diagnosis;
TRUNCATE TABLE appointment;
TRUNCATE TABLE schedule;
TRUNCATE TABLE patient;
TRUNCATE TABLE doctor;
TRUNCATE TABLE clinic;
TRUNCATE TABLE department;
TRUNCATE TABLE user;

-- ============================================================
-- 1. 用户
-- ============================================================
INSERT INTO `user` (`id`, `username`, `password`, `phone`, `email`, `role`) VALUES
(1,  'admin',    '3cc64eb037d2f883f0a9895a9c1b018a', '13800000001', 'admin@test.com',   2),
(2,  'zhangsan', '3cc64eb037d2f883f0a9895a9c1b018a', '13800000002', 'zhangsan@test.com', 1),
(3,  'lisi',     '3cc64eb037d2f883f0a9895a9c1b018a', '13800000003', 'lisi@test.com',     1),
(4,  'wangwu',   '3cc64eb037d2f883f0a9895a9c1b018a', '13800000004', 'wangwu@test.com',   1),
(5,  'liuwei',   '3cc64eb037d2f883f0a9895a9c1b018a', '13800000005', 'liuwei@test.com',   0),
(6,  'zhaomin',  '3cc64eb037d2f883f0a9895a9c1b018a', '13800000006', 'zhaomin@test.com',  0),
(7,  'sunqiang', '3cc64eb037d2f883f0a9895a9c1b018a', '13800000007', 'sunqiang@test.com', 0);

-- ============================================================
-- 2. 科室
-- ============================================================
INSERT INTO `department` (`dept_id`, `dept_name`) VALUES
(1, '心血管内科'), (2, '呼吸内科'), (3, '消化内科'), (4, '骨科'),
(5, '儿科'),       (6, '神经内科'), (7, '皮肤科'),   (8, '急诊科');

-- ============================================================
-- 3. 门诊
-- ============================================================
INSERT INTO `clinic` (`clinic_id`, `dept_id`, `clinic_name`) VALUES
(1, 1, '高血压专病门诊'), (2, 1, '冠心病专病门诊'),
(3, 2, '哮喘专病门诊'),   (4, 2, '慢阻肺专病门诊'),
(5, 3, '胃肠病门诊'),     (6, 4, '关节外科门诊'),
(7, 4, '脊柱外科门诊'),   (8, 5, '儿科普通门诊'),
(9, 6, '头痛门诊'),       (10, 8, '急诊综合门诊');

-- ============================================================
-- 4. 医生
-- ============================================================
INSERT INTO `doctor` (`doctor_id`, `user_id`, `name`, `clinic_id`, `title`, `introduction`) VALUES
(1, 2, '张医生', 1,  '主任医师', '心血管内科专家，20年临床经验。'),
(2, 3, '李医生', 5,  '副主任医师', '消化内科专家，擅长胃肠镜诊疗。'),
(3, 4, '王医生', 8,  '主治医师', '儿科医生，擅长儿童常见病诊治。');

-- ============================================================
-- 5. 患者
-- ============================================================
INSERT INTO `patient` (`patient_id`, `user_id`, `name`, `id_card`, `gender`, `age`, `region`, `address`) VALUES
(1, 5, '刘伟', '320102199001010011', 1, 35, '江苏省南京市', '鼓楼区中山路100号'),
(2, 6, '赵敏', '320102199205150022', 2, 28, '江苏省南京市', '玄武区北京东路200号'),
(3, 7, '孙强', '320102198808080033', 1, 48, '江苏省南京市', '建邺区江东中路300号');

-- ============================================================
-- 6. 排班（今天+未来6天，每位医生每天2个时段）
-- ============================================================
INSERT INTO `schedule` (`doctor_id`, `clinic_id`, `schedule_date`, `time_slot`, `max_patients`, `current_patients`)
SELECT d.doctor_id, d.clinic_id,
  DATE_ADD(CURDATE(), INTERVAL dd.n DAY),
  ts.slot, 10, 0
FROM (SELECT 1 AS doctor_id, 1 AS clinic_id
      UNION SELECT 2, 5
      UNION SELECT 3, 8) d,
     (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) dd,
     (SELECT '08:00-12:00' AS slot UNION SELECT '14:00-18:00') ts;

-- ============================================================
-- 7. 历史就诊数据（过去30天，用于看板统计）
-- ============================================================
INSERT INTO `appointment` (`patient_id`, `doctor_id`, `schedule_id`, `appointment_date`, `time_slot`, `is_revisit`, `status`)
SELECT p.patient_id, d.doctor_id,
  (SELECT schedule_id FROM `schedule` LIMIT 1),
  DATE_SUB(CURDATE(), INTERVAL dy.n DAY),
  IF(dy.n MOD 2 = 0, '08:00-12:00', '14:00-18:00'),
  0, 1
FROM (SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
      UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
      UNION SELECT 17 UNION SELECT 19 UNION SELECT 21 UNION SELECT 23 UNION SELECT 25
      UNION SELECT 27 UNION SELECT 29) dy,
     (SELECT 1 AS patient_id UNION SELECT 2 UNION SELECT 3) p,
     (SELECT 1 AS doctor_id UNION SELECT 2 UNION SELECT 3) d;

-- ============================================================
-- 8. 药品
-- ============================================================
INSERT INTO `drug` (`drug_name`, `specification`, `manufacturer`, `unit`, `unit_price`, `stock_quantity`, `description`) VALUES
('阿莫西林胶囊',   '0.25g*24粒',   '华北制药',     '盒', 12.50, 200, '青霉素类抗生素。使用前需青霉素皮试。'),
('布洛芬缓释胶囊', '0.3g*20粒',    '中美史克',     '盒', 18.00, 150, '解热镇痛，用于头痛/关节痛/牙痛。'),
('奥美拉唑肠溶片', '20mg*14片',    '阿斯利康',     '盒', 35.00, 100, '质子泵抑制剂，用于胃/十二指肠溃疡。'),
('氨溴索口服液',   '100ml:0.6g',   '勃林格殷格翰', '瓶', 28.00, 80,  '祛痰药，用于支气管炎痰液粘稠。'),
('氯雷他定片',     '10mg*6片',     '先灵葆雅',     '盒', 15.50, 120, '抗组胺药，过敏性鼻炎/荨麻疹。'),
('硝苯地平控释片', '30mg*7片',     '拜耳医药',     '盒', 45.00, 60,  '钙通道阻滞剂，高血压/心绞痛。'),
('阿托伐他汀钙片', '20mg*7片',     '辉瑞制药',     '盒', 55.00, 50,  '他汀类降脂药，高胆固醇血症。'),
('蒙脱石散',       '3g*10袋',      '益普生',       '盒', 22.00, 90,  '急慢性腹泻。'),
('二甲双胍片',     '0.5g*20片',    '中美施贵宝',   '盒', 16.00, 70,  '口服降糖药，2型糖尿病。'),
('左氧氟沙星片',   '0.5g*4片',     '第一三共',     '盒', 32.00, 45,  '喹诺酮类抗生素，18岁以下禁用。');

-- ============================================================
-- 9. 诊断记录（为历史预约生成）
-- ============================================================
INSERT INTO `diagnosis` (`appointment_id`, `doctor_id`, `patient_id`, `diagnosis_result`, `examination`, `advice`)
SELECT a.appointment_id, a.doctor_id, a.patient_id,
  ELT((a.appointment_id MOD 4) + 1, '上呼吸道感染', '高血压I级', '消化不良', '过敏性鼻炎'),
  ELT((a.appointment_id MOD 3) + 1, '血常规、胸片', '心电图', '过敏原检测'),
  ELT((a.appointment_id MOD 3) + 1, '多休息多饮水', '低盐低脂饮食', '避免接触过敏原')
FROM `appointment` a
WHERE a.status = 1
LIMIT 15;

-- ============================================================
-- 10. 处方 + 明细
-- ============================================================
INSERT INTO `prescription` (`diagnosis_id`, `doctor_id`, `patient_id`, `status`)
SELECT diag_id, doctor_id, patient_id, 1 FROM `diagnosis` ORDER BY diag_id LIMIT 8;

INSERT INTO `prescription_detail` (`prescription_id`, `drug_id`, `unit_price`, `quantity`, `dosage`, `usage_method`, `frequency`, `days`)
SELECT p.prescription_id,
  (p.prescription_id MOD 5) + 1,
  (SELECT unit_price FROM drug WHERE drug_id = (p.prescription_id MOD 5) + 1),
  IF(p.prescription_id MOD 3 = 0, 2, 1),
  '1片', '口服', '每日3次', 7
FROM `prescription` p;

-- ============================================================
-- 11. 账单 + 明细
-- ============================================================
INSERT INTO `bill` (`appointment_id`, `patient_id`, `total_amount`, `paid_amount`, `status`, `payment_method`, `pay_time`)
SELECT d.appointment_id, d.patient_id,
  ROUND((@r := RAND()) * 200 + 20, 2),
  0.00,
  IF(@r > 0.3, 2, 0),
  IF(@r > 0.3, ELT(FLOOR(1 + RAND() * 4), '微信', '支付宝', '现金', '医保卡'), NULL),
  IF(@r > 0.3, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 10) DAY), NULL)
FROM `diagnosis` d
LIMIT 12;

-- 已付账单同步 paid_amount
UPDATE `bill` SET `paid_amount` = `total_amount` WHERE `status` = 2;

-- 挂号费明细
INSERT INTO `bill_item` (`bill_id`, `item_type`, `item_name`, `unit_price`, `quantity`, `amount`, `related_id`)
SELECT bill_id, 'registration', '挂号费', 20.00, 1, 20.00, appointment_id FROM `bill`;

-- 药品费明细（偶数bill_id加药费）
INSERT INTO `bill_item` (`bill_id`, `item_type`, `item_name`, `unit_price`, `quantity`, `amount`, `related_id`)
SELECT b.bill_id, 'drug', d.drug_name, d.unit_price, 1, d.unit_price, d.drug_id
FROM `bill` b, `drug` d
WHERE b.bill_id MOD 2 = 0 AND d.drug_id = (b.bill_id MOD 5) + 1;

-- ============================================================
-- 12. 通知
-- ============================================================
INSERT INTO `notification` (`user_id`, `title`, `content`, `type`, `is_read`)
SELECT id, '欢迎使用智慧医疗系统', '感谢您注册智慧医疗门诊系统，祝您身体健康！', 'system', 0
FROM `user` WHERE role = 0;

INSERT INTO `notification` (`user_id`, `title`, `content`, `type`, `is_read`, `related_id`)
SELECT u.id, '预约成功',
  CONCAT('您已成功预约 ', d.name, '医生，日期：', a.appointment_date, '，时段：', a.time_slot, '，请按时就诊。'),
  'appointment_reminder', 0, a.appointment_id
FROM `appointment` a
JOIN `patient` p ON p.patient_id = a.patient_id
JOIN `user` u ON u.id = p.user_id
JOIN `doctor` d ON d.doctor_id = a.doctor_id
WHERE a.status = 1
LIMIT 5;

SET FOREIGN_KEY_CHECKS = 1;
