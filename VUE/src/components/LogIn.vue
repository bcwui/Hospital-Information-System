<script lang="ts" setup>
import { useUserStore } from '@/stores/user';
import { reactive, defineEmits, ref } from 'vue'
import { useRouter } from 'vue-router';

const Login = ref(null);
const logining = ref(false);
const router = useRouter();
const userstor = useUserStore();

// do not use same name with ref
const form = reactive({
  name: '',
  password: '',
})
const emit = defineEmits(['turnLoR'])

const handelturn = () => {
  emit('turnLoR', 'register')
}

const handleLogin = async () => {
  if (logining.value) return;
  logining.value = true;
  try {
    const userInfo = {
      account: form.name as string,
      password: form.password as string
    };
    await userstor.login(userInfo);

    // 根据角色跳转
    const role = userstor.userInfo?.role;
    if (role === 0) {
      router.push({ path: '/patient' });
    } else if (role === 1) {
      router.push({ path: '/doctor' });
    } else if (role === 2) {
      router.push({ path: '/admin' });
    }
  } catch (error) {
    // 错误已由 DoAxiosWithErro 显示
    console.error('登录失败:', error);
  } finally {
    logining.value = false;
  }
}
</script>
<template>
  <section class="auth-panel">
    <header class="panel-head">
      <h3>登录</h3>
    </header>
    <el-form ref="Login" :model="form" label-width="auto" class="auth-form">
      <el-form-item label="账号">
        <el-input v-model="form.name" placeholder="请输入昵称/用户名" autocomplete="username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>
      <div class="actions">
        <el-button :disabled="logining" @click="handleLogin" type="primary" class="wide">
          {{ logining ? '登录中...' : '登录' }}
        </el-button>
      </div>
    </el-form>
    <footer class="panel-foot">
      <span>还没有账号？</span>
      <el-link @click="handelturn" type="primary">去注册</el-link>
    </footer>
  </section>
</template>


<style lang="scss" scoped>
.auth-panel {
  height: 100%;
  width: 100%;
  padding: 1.5rem 2rem;
  border-radius: 24px;
  background: var(--card-bg, #fff);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.panel-head {
  h3 {
    margin: 0;
    font-size: 1.75rem;
    color: var(--text-strong, #1f2933);
  }

  p {
    margin: 0.35rem 0 0;
    color: var(--text-muted, #5f6b7c);
    font-size: 0.95rem;
  }
}

.auth-form {
  :deep(.el-form-item) {
    margin-bottom: 1rem;
  }

  :deep(.el-input__wrapper) {
    border-radius: 12px;
  }
}

.actions {
  display: flex;
  justify-content: flex-end;

  .wide {
    width: 100%;
  }
}

.panel-foot {
  display: flex;
  justify-content: center;
  gap: 0.4rem;
  font-size: 0.95rem;
  color: var(--text-muted, #5f6b7c);
}
</style>
