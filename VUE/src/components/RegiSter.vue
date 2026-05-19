<script setup lang="ts">

import { DoAxios, DoAxiosWithErro } from '@/api';
import { reactive, ref, defineEmits } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus';
//   import { FormInstance, FormRules } from 'element-plus' // 不需要类型导入

type ValidatorCallback = (error?: Error) => void;

const ruleFormRef = ref<FormInstance | null>(null)
const isfetching = ref(false);
const isdetailed = ref(true);

const sendButton = reactive({
  value: "发送请求",
  refresh: 0,
  disable: false
})

const ruleForm = reactive({
  pass: '',
  checkPass: '',
  username: '',
  email: '',
  checkcode: '',
  phone: '',
  idCard: '',
  name: '',
  address: '',
  gender: '',
  region: '',
  age: 0,
})
const emit = defineEmits(["turnLoR"])

const handleturn = () => {
  emit('turnLoR', 'login');
}

const handleSend = async () => {
  sendButton.disable = true;
  try {
    await DoAxios(`/auth/email`, 'get', {
      email: ruleForm.email
    }, false, false, 30000);
    ElMessage({
      message: '发送成功',
      type: 'success'
    })
    sendButton.disable = true;
    sendButton.refresh = 60;
    sendButton.value = '重发'
    const timer = setInterval(() => {
      sendButton.refresh = sendButton.refresh - 1;
      if (sendButton.refresh === 0) {
        clearInterval(timer);
        sendButton.value = '重新发送'
        sendButton.disable = false;
      }
    }, 1000);
  } catch (e) {
    sendButton.disable = false;
    ElMessage({
      message: e instanceof Error ? e.message : String(e),
      type: 'error'
    })
  }
}

const checkUserName = (_rule: unknown, value: string, callback: ValidatorCallback) => {
  if (!value) {
    return callback(new Error("请输入昵称"))
  }
  DoAxios('/auth/IsExists', 'get', {
    username: value
  }, false).then((res) => {
    if (!res) {
      callback();
    } else {
      callback(new Error("该昵称已被注册"))
    }
  }).catch(rject => {
    callback(rject);
  })
}

const checkEmail = (_rule: unknown, value: string, callback: ValidatorCallback) => {
  const reg = /^[0-9A-Za-z._%]+@[a-zA-Z0-9._-]+\.[a-zA-Z]{2,}$/;

  // 如果没有输入值，提示"请输入绑定邮箱"
  if (!value) {
    return callback(new Error("请输入绑定邮箱"));
  }

  // 如果邮箱格式不正确，提示"请输入正确的邮箱地址"
  if (!reg.test(value)) {
    return callback(new Error("请输入正确的邮箱地址"));
  }

  // 异步请求，检查邮箱是否已注册
  DoAxios('/auth/IsExists', 'get', {
    email: value,
  })
    .then((res) => {
      if (!res) {
        callback(); // 邮箱不存在，验证通过
      } else {
        callback(new Error("该邮箱已被注册"));
      }
    })
    .catch(err => {
      callback(err);
    })
};





const checkCode = (_rule: unknown, value: string, callback: ValidatorCallback) => {
  if (value === '') {
    return callback(new Error("请输入验证码"))
  }
  callback();
}

const checkPhone = (_rule: unknown, value: string, callback: ValidatorCallback) => {
  const reg = /^1[23456789]\d{9}$/;
  if (value === '') {
    return callback(new Error("请输入手机号"))
  }
  if (!reg.test(value)) {
    return callback(new Error("请输入正确的手机号"))
  }
  callback();
}

const checkIDcard = (_rule: unknown, value: string, callback: ValidatorCallback) => {
  const reg = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/;
  if (value === '') {
    return callback(new Error("请输入身份证号"))
  }
  if (!reg.test(value)) {
    return callback(new Error("请输入正确的身份证号"))
  }
  callback();
}

const checkName = (_rule: unknown, value: string, callback: ValidatorCallback) => {
  if (value === '') {
    return callback(new Error("请输入姓名"))
  }
  callback();
}

const checkAddress = (_rule: unknown, value: string, callback: ValidatorCallback) => {
  if (value === '') {
    return callback(new Error("请输入地址"))
  }
  callback();
}

const checkGender = (_rule: unknown, value: string | number, callback: ValidatorCallback) => {
  if (value === '') {
    return callback(new Error("请选择性别"))
  }
  callback();
}

const checkRegion = (_rule: unknown, value: string, callback: ValidatorCallback) => {
  if (value === '') {
    return callback(new Error("请选择地区"))
  }
  callback();
}

const checkAge = (_rule: unknown, value: number | '', callback: ValidatorCallback) => {
  if (value === '') {
    return callback(new Error("请输入年龄"))
  }
  if (value > 200 || value < 0) {
    return callback(new Error("请输入正确的年龄"))
  }
  callback();
}

const validatePass = (_rule: unknown, value: string, callback: ValidatorCallback) => {
  const reg = /^(?=.*[a-zA-Z])(?=.*[\d])[a-zA-Z\d]{6,20}$/;
  if (value === '') {
    return callback(new Error('请输入密码'));
  }
  if (!reg.test(value)) {
    return callback(new Error("密码需包含字母和数字，长度6-20位"));
  }
  if (ruleForm.checkPass !== '') {
    ruleFormRef.value?.validateField("checkPass");
  }
  callback();
};


const validatePass2 = (_rule: unknown, value: string, callback: ValidatorCallback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== ruleForm.pass) {
    callback(new Error("两次输入密码不一致!"))
  } else {
    callback()
  }
}



const rules = reactive({
  pass: [{ validator: validatePass, trigger: 'change' }],
  checkPass: [{ validator: validatePass2, trigger: 'change' }],
  username: [{ validator: checkUserName, trigger: "blur" }],
  email: [{ validator: checkEmail, trigger: "blur" }],
  checkcode: [{ validator: checkCode, trigger: 'change' }],
  phone: [{ validator: checkPhone, trigger: 'change' }],
  idCard: [{ validator: checkIDcard, trigger: 'change' }],
  name: [{ validator: checkName, trigger: 'change' }],
  address: [{ validator: checkAddress, trigger: 'change' }],
  gender: [{ validator: checkGender, trigger: 'change' }],
  region: [{ validator: checkRegion, trigger: 'change' }],
  age: [{ validator: checkAge, trigger: 'change' }]
})

const submitForm = () => {
  const formEl = ruleFormRef.value;
  if (!formEl) {
    console.log("undefind is err");
    return;
  }
  formEl.validate(async (valid: boolean) => {
    if (valid) {
      const formdata = {
        username: ruleForm.username,
        password: ruleForm.pass,
        email: ruleForm.email,
        verifyCode: ruleForm.checkcode,
        phone: ruleForm.phone,
        idCard: ruleForm.idCard,
        name: ruleForm.name,
        address: ruleForm.address,
        gender: ruleForm.gender,
        region: ruleForm.region,
        age: ruleForm.age
      }
      isfetching.value = true
      console.log(formdata);
      await DoAxiosWithErro('/auth/register', 'post', formdata, false).finally(() => {
        isfetching.value = false
      });
      ElMessage({
        message: '注册成功！',
        type: 'success'
      })
      handleturn();
    }
  })
}

const resetForm = (formEl: FormInstance | null) => {
  if (!formEl) return
  isdetailed.value = true;
  formEl.resetFields()
}

const sendMessage = async () => {
  const formEl = ruleFormRef.value;
  if (!formEl) {
    ElMessage.warning('表单尚未初始化');
    return;
  }
  const validateList = ['email', 'name'];

  const validationPromises = validateList.map(item => {
    return new Promise((resolve, reject) => {
      formEl.validateField(item, (isValid: boolean) => {
        if (isValid) {
          resolve(true);
        } else {
          reject(false);
        }
      });
    });
  });

  Promise.all(validationPromises)
    .then(() => {
      handleSend(); // 验证通过后发送消息
    })
    .catch(() => {
      ElMessage.warning('请先完善必填信息');
    });
};


</script>

<template>
  <section class="auth-panel register-panel">
    <header class="panel-head">
      <h3>注册</h3>
      <div class="step-indicator">
        <span :class="{ active: isdetailed }">账号信息</span>
        <span :class="{ active: !isdetailed }">个人资料</span>
      </div>
    </header>
    <el-form ref="ruleFormRef" :model="ruleForm" status-icon :rules="rules" label-width="0" class="auth-form">
      <transition name="fade" mode="out-in">
        <div v-if="isdetailed" key="account" class="grid-wrapper">
          <el-form-item label="" prop="username">
            <el-input v-model="ruleForm.username" placeholder="昵称 / 登录账号" autocomplete="off" />
          </el-form-item>
          <el-form-item label="" prop="gender">
            <el-radio-group v-model="ruleForm.gender">
              <el-radio-button :label="1">男</el-radio-button>
              <el-radio-button :label="2">女</el-radio-button>
              <el-radio-button :label="0">未知</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="" prop="pass">
            <el-input v-model="ruleForm.pass" type="password" placeholder="设置登录密码" show-password />
          </el-form-item>
          <el-form-item label="" prop="checkPass">
            <el-input v-model="ruleForm.checkPass" type="password" placeholder="再次输入密码" show-password />
          </el-form-item>
          <el-form-item label="" prop="phone">
            <el-input v-model="ruleForm.phone" placeholder="常用手机号" />
          </el-form-item>
          <el-form-item class="full-width">
            <el-button class="cta" type="primary" @click="isdetailed = !isdetailed">下一步</el-button>
          </el-form-item>
        </div>
        <div v-else key="profile" class="grid-wrapper">
          <el-form-item label="" prop="name">
            <el-input v-model="ruleForm.name" placeholder="真实姓名" />
          </el-form-item>
          <el-form-item label="" prop="age">
            <el-input-number v-model="ruleForm.age" :min="0" :max="150" placeholder="年龄" />
          </el-form-item>
          <el-form-item label="" prop="idCard">
            <el-input v-model="ruleForm.idCard" placeholder="身份证号" />
          </el-form-item>
          <el-form-item label="" prop="region">
            <el-input v-model="ruleForm.region" placeholder="所在地区" />
          </el-form-item>
          <el-form-item label="" prop="address">
            <el-input v-model="ruleForm.address" placeholder="详细地址" />
          </el-form-item>
          <el-form-item label="" prop="email">
            <el-input v-model="ruleForm.email" placeholder="绑定邮箱" />
          </el-form-item>
          <el-form-item class="full-width checkcode" label="" prop="checkcode">
            <el-input class="code" v-model="ruleForm.checkcode" placeholder="邮箱验证码" />
            <el-button class="but" @click="sendMessage" :disabled="sendButton.disable">
              <span v-show="sendButton.disable && sendButton.refresh">{{ sendButton.refresh }}秒后</span>
              {{ sendButton.value }}
            </el-button>
          </el-form-item>
          <el-form-item class="full-width">
            <el-button type="primary" class="cta" @click="submitForm" :disabled="isfetching">
              {{ isfetching ? '提交中...' : '完成注册' }}
            </el-button>
          </el-form-item>
          <el-form-item class="full-width secondary">
            <el-button text @click="isdetailed = true">返回上一步</el-button>
            <el-button text type="danger" @click="resetForm(ruleFormRef)">重置</el-button>
          </el-form-item>
        </div>
      </transition>
    </el-form>
    <footer class="panel-foot">
      <span>已有账号？</span>
      <el-link @click="handleturn" type="primary">直接登录</el-link>
    </footer>
  </section>
</template>

<style lang="scss" scoped>
.auth-panel {
  padding: 1.75rem 2rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.panel-head {
  text-align: center;

  h3 {
    margin: 0;
    font-size: 1.5rem;
    font-weight: 700;
    background: linear-gradient(135deg, #0ea5e9, #6366f1);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  p {
    margin: 0.35rem 0 0;
    color: #64748b;
  }
}

.step-indicator {
  display: inline-flex;
  gap: 0.25rem;
  margin-top: 0.75rem;
  padding: 0.2rem 0.3rem;
  border-radius: 999px;
  background: #f1f5f9;

  span {
    padding: 0.35rem 1rem;
    border-radius: 999px;
    font-size: 0.82rem;
    color: #64748b;
    font-weight: 500;
    transition: all 0.3s ease;

    &.active {
      background: linear-gradient(135deg, #0ea5e9, #6366f1);
      color: #fff;
      box-shadow: 0 2px 8px rgba(14, 165, 233, 0.3);
    }
  }
}

.auth-form {
  :deep(.el-form-item) {
    margin-bottom: 0.9rem;
  }

  .grid-wrapper {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.75rem 1.25rem;
  }

  .full-width {
    grid-column: span 2;
  }

  :deep(.el-input__wrapper) {
    border-radius: 10px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
    transition: all 0.25s ease;

    &:hover {
      box-shadow: 0 2px 8px rgba(14, 165, 233, 0.1);
    }

    &.is-focus {
      box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.12);
    }
  }

  :deep(.el-input-number) {
    width: 100%;
  }
}

.checkcode {
  display: flex;
  gap: 0.75rem;
  align-items: center;

  .code {
    flex: 1;
    margin-right: 20px;
  }
}

.cta {
  width: 100%;
  height: 42px;
  border-radius: 10px;
  font-weight: 600;
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  border: none;
  transition: all 0.3s ease;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 6px 18px rgba(14, 165, 233, 0.3);
  }
}

.secondary {
  display: flex;
  justify-content: space-between;
}

.panel-foot {
  display: flex;
  justify-content: center;
  gap: 0.4rem;
  color: #64748b;
  font-size: 0.9rem;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
</style>
