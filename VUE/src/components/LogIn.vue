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
  padding: 2rem 2.25rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.panel-head {
  text-align: center;

  h3 {
    margin: 0;
    font-size: 1.6rem;
    font-weight: 700;
    background: linear-gradient(135deg, #0ea5e9, #6366f1);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  p {
    margin: 0.35rem 0 0;
    color: #64748b;
    font-size: 0.9rem;
  }
}

.auth-form {
  :deep(.el-form-item) {
    margin-bottom: 1.1rem;
  }

  :deep(.el-input__wrapper) {
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
    transition: all 0.25s ease;

    &:hover {
      box-shadow: 0 2px 8px rgba(14, 165, 233, 0.12);
    }

    &.is-focus {
      box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.15), 0 2px 8px rgba(14, 165, 233, 0.1);
    }
  }

  :deep(.el-input__inner) {
    font-size: 0.95rem;
  }
}

.actions {
  display: flex;
  justify-content: flex-end;

  .wide {
    width: 100%;
    height: 44px;
    border-radius: 12px;
    font-size: 1rem;
    font-weight: 600;
    background: linear-gradient(135deg, #0ea5e9, #6366f1);
    border: none;
    transition: all 0.3s ease;
    letter-spacing: 0.02em;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 8px 20px rgba(14, 165, 233, 0.35);
    }

    &:active:not(:disabled) {
      transform: translateY(0);
    }
  }
}

.panel-foot {
  display: flex;
  justify-content: center;
  gap: 0.4rem;
  font-size: 0.9rem;
  color: #64748b;
  padding-top: 0.25rem;
}
</style>
