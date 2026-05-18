<template>
  <div id="card" class="light-shadow cardHover">
    <h2>患者信息</h2>
    <div id="main-msg">
      <div class="left">
        <p><span class="label">当前序号:</span><span class="detail">{{ num }}</span></p>
        <p><span class="label">姓名:</span><span class="detail">{{ name }}</span></p>
        <p><span class="label">性别:</span><span class="detail">{{ gender }}</span></p>
        <p><span class="label">年龄:</span><span class="detail">{{ age }}</span></p>
        <p><span class="label">病历号:</span><span class="detail">{{ id }}</span></p>
      </div>
      <div class="right">
        <p><span class="label">挂号时间:</span><span class="detail">{{ registerTime }}</span></p>
        <p><span class="label">科室名:</span><span class="detail">{{ department }}</span></p>
        <p><span class="label">诊室名:</span><span class="detail">{{ clinicRoom }}</span></p>
        <p><span class="label">当前状态:</span><el-tag class="detail" type="warning">{{ state }}</el-tag></p>
        <p><span class="label">备注:</span><span class="detail">{{ note }}</span></p>
      </div>
    </div>
    <p><span class="label">症状:</span><span class="detail">{{ description }}</span></p>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue';
import { ElTag } from 'element-plus';
defineProps({
  num: {
    type: Number,
    required: true,
    default: 1,
  },
  name: {
    type: String,
    required: true,
    default: '默认患者',
  },
  gender: {
    type: String,
    required: true,
    default: '男',
  },
  age: {
    type: Number,
    required: true,
    default: 18,
  },
  id: {
    type: String,
    required: true,
    default: '001',
  },
  description: {
    type: String,
    required: true,
    default: '请阐释症状,例:患者头部发热',
  },
  registerTime: {
    type: String,
    required: true,
    default: '2025-05-01',
  },
  department: {
    type: String,
    required: true,
    default: '内科',
  },
  clinicRoom: {
    type: String,
    required: true,
    default: '门诊1',
  },
  state: {
    type: String,
    required: true,
    default: "待执行",
  },
  note: {
    type: String,
    required: true,
    default: "无",
  },
});
</script>

<style lang="scss" scoped>
#card {
  float: left;
  min-height: 200px;
  width: 42%;
  margin: 10px 2%;
  min-width: 400px;
  padding: 20px;
  background: vars.$card-bg-depart;
  border-radius: 10px;
  color: #303133;

  h2 {
    border-bottom: 1px solid black;
    padding-bottom: 10px;
    margin-bottom: 10px;
  }

  #main-msg {
    display: flex;

    .right {
      margin-left: 30%;
    }
  }

  p {
    display: flex;
    margin: 1px 0;

    .label {
      width: 70px;
      text-align: left;
    }

    .detail {
      font-weight: bold;
      margin-left: 20px;
      color: #606266;
    }
  }
}
</style>
