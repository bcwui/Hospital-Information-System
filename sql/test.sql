USE `hospital`;

-- 1. 用户表测试数据
INSERT INTO `user` (`username`, `password`, `phone`, `email`, `role`, `avatar`) VALUES
('patient_001', 'e10adc3949ba59abbe56e057f20f883e', '13800138001', 'patient001@test.com', 0, 'https://avatar.example.com/p1.jpg'),
('doctor_001', 'e10adc3949ba59abbe56e057f20f883e', '13900139001', 'doctor001@test.com', 1, 'https://avatar.example.com/d1.jpg'),
('admin_001', 'e10adc3949ba59abbe56e057f20f883e', '13700137001', 'admin001@test.com', 2, 'https://avatar.example.com/a1.jpg'),
('patient_002', 'e10adc3949ba59abbe56e057f20f883e', '13800138002', 'patient002@test.com', 0, 'https://avatar.example.com/p2.jpg'),
('doctor_002', 'e10adc3949ba59abbe56e057f20f883e', '13900139002', 'doctor002@test.com', 1, 'https://avatar.example.com/d2.jpg');

-- 2. 科室表测试数据
INSERT INTO `department` (`dept_name`, `is_active`) VALUES
('内科', 1),
('外科', 1),
('妇产科', 1),
('儿科', 1),
('急诊科', 1);

-- 3. 门诊表测试数据
INSERT INTO `clinic` (`dept_id`, `clinic_name`, `is_active`) VALUES
(1, '心血管内科门诊', 1),
(1, '呼吸内科门诊', 1),
(2, '骨科门诊', 1),
(2, '普外科门诊', 1),
(3, '妇科门诊', 1),
(4, '小儿呼吸门诊', 1),
(5, '急诊内科门诊', 1);

-- 4. 医生表测试数据
INSERT INTO `doctor` (`user_id`, `name`, `clinic_id`, `title`, `introduction`) VALUES
(2, '张医生', 1, '主任医师', '从事心血管内科临床工作20年，擅长冠心病、高血压等疾病的诊治。'),
(5, '李医生', 3, '副主任医师', '骨科专家，擅长骨折、关节置换等手术治疗，临床经验丰富。'),
(6, '王医生', 5, '主治医师', '专注妇科常见疾病诊疗，对月经不调、盆腔炎等疾病有深入研究。'),
(7, '赵医生', 6, '副主任医师', '小儿呼吸科专家，擅长儿童哮喘、肺炎等疾病的诊治。'),
(8, '孙医生', 7, '主治医师', '急诊科工作10年，擅长各类急症的急救处理。');

-- 修正：补充缺失的user_id（上面doctor表引用的user_id需要存在，补充user表数据）
INSERT INTO `user` (`username`, `password`, `phone`, `email`, `role`, `avatar`) VALUES
('doctor_003', 'e10adc3949ba59abbe56e057f20f883e', '13900139003', 'doctor003@test.com', 1, 'https://avatar.example.com/d3.jpg'),
('doctor_004', 'e10adc3949ba59abbe56e057f20f883e', '13900139004', 'doctor004@test.com', 1, 'https://avatar.example.com/d4.jpg'),
('doctor_005', 'e10adc3949ba59abbe56e057f20f883e', '13900139005', 'doctor005@test.com', 1, 'https://avatar.example.com/d5.jpg');

-- 5. 患者表测试数据
INSERT INTO `patient` (`user_id`, `name`, `id_card`, `gender`, `age`, `region`, `address`) VALUES
(1, '张三', '110101199001011234', 1, 34, '北京市朝阳区', '北京市朝阳区建国路88号'),
(4, '李四', '310101198505056789', 2, 39, '上海市浦东新区', '上海市浦东新区张江路100号'),
(9, '王五', '440101201008081234', 1, 14, '广东省广州市天河区', '广州市天河区天河路500号'),
(10, '赵六', '510101197810106789', 2, 46, '四川省成都市武侯区', '成都市武侯区武侯祠大街10号'),
(11, '钱七', '330101200503031234', 0, 19, '浙江省杭州市西湖区', '杭州市西湖区西湖路88号');

-- 补充患者关联的user表数据
INSERT INTO `user` (`username`, `password`, `phone`, `email`, `role`, `avatar`) VALUES
('patient_003', 'e10adc3949ba59abbe56e057f20f883e', '13800138003', 'patient003@test.com', 0, 'https://avatar.example.com/p3.jpg'),
('patient_004', 'e10adc3949ba59abbe56e057f20f883e', '13800138004', 'patient004@test.com', 0, 'https://avatar.example.com/p4.jpg'),
('patient_005', 'e10adc3949ba59abbe56e057f20f883e', '13800138005', 'patient005@test.com', 0, 'https://avatar.example.com/p5.jpg');

-- 6. 医生排班表测试数据
INSERT INTO `schedule` (`doctor_id`, `clinic_id`, `schedule_date`, `time_slot`, `max_patients`, `current_patients`, `status`) VALUES
(1, 1, '2024-05-20', '08:00-12:00', 10, 3, 1),
(1, 1, '2024-05-20', '14:00-18:00', 10, 5, 1),
(2, 3, '2024-05-21', '08:00-12:00', 8, 2, 1),
(3, 5, '2024-05-22', '08:00-12:00', 10, 7, 1),
(4, 6, '2024-05-20', '08:00-12:00', 12, 4, 1),
(5, 7, '2024-05-20', '24:00-24:00', 20, 10, 1);

-- 7. 预约挂号表测试数据
INSERT INTO `appointment` (`patient_id`, `doctor_id`, `schedule_id`, `appointment_date`, `time_slot`, `is_revisit`, `status`) VALUES
(1, 1, 1, '2024-05-20', '08:00-12:00', 0, 0),
(2, 1, 2, '2024-05-20', '14:00-18:00', 1, 1),
(3, 2, 3, '2024-05-21', '08:00-12:00', 0, 0),
(4, 3, 4, '2024-05-22', '08:00-12:00', 1, 1),
(5, 4, 5, '2024-05-20', '08:00-12:00', 0, 2);

-- 8. AI 问诊记录表测试数据
INSERT INTO `ai_consult_record` (`session_id`, `patient_id`, `conversation`) VALUES
('sess_10001', 1, '{"messages":[{"role":"user","content":"我最近总是心慌，胸闷怎么办？"},{"role":"assistant","content":"根据您的症状，建议先挂心血管内科门诊，做心电图检查..."}]}'),
('sess_10002', 2, '{"messages":[{"role":"user","content":"孩子发烧38.5度，咳嗽，需要吃什么药？"},{"role":"assistant","content":"建议先物理降温，及时到儿科门诊就诊，不要自行用药..."}]}'),
('sess_10003', 3, '{"messages":[{"role":"user","content":"骨折后康复需要注意什么？"},{"role":"assistant","content":"骨折康复期需注意休息，避免负重，按医嘱进行康复训练..."}]}');

-- 9. 医生诊断记录表测试数据
INSERT INTO `diagnosis` (`appointment_id`, `doctor_id`, `patient_id`, `diagnosis_result`, `examination`, `prescription`, `advice`) VALUES
(2, 1, 2, '高血压2级，冠心病', '心电图：ST段轻度改变；血压：160/100mmHg', '硝苯地平缓释片 1片/次，2次/日；阿司匹林肠溶片 1片/次，1次/日', '低盐低脂饮食，避免劳累，定期监测血压，1个月后复诊'),
(4, 3, 4, '盆腔炎，月经不调', 'B超：盆腔少量积液；性激素六项：雌激素略低', '妇科千金片 4片/次，3次/日；黄体酮胶囊 2粒/次，1次/日', '注意个人卫生，避免熬夜，忌辛辣刺激性食物，2周后复诊'),
(1, 1, 1, '待排查冠心病', '心电图：未见明显异常；血常规：正常', '建议进一步做冠脉CT检查，暂未用药', '避免情绪激动，等待检查结果，不适随诊');