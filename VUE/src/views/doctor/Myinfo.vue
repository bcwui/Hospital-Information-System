<template>
  <div class="profile-page" v-loading="loading">
    <header class="page-head">
      <h2>我的信息</h2>
      <el-button type="primary" @click="showEditTip">修改个人资料</el-button>
    </header>

    <section class="profile-card">
      <div class="avatar-panel">
        <input type="file" id="file" @change="handleUP" style="display: none;" />
        <el-avatar :src="myInfo.avatar" :size="150" class="avatar"></el-avatar>
        <label for="file" class="upload-btn">修改头像</label>
        <el-button v-if="newAvatar" type="success" plain size="small" @click="uploadAvatar">保存修改</el-button>
      </div>

      <div class="info-panel">
        <div class="info-col">
          <div class="field"><span class="label">姓名</span><span class="value">{{ myInfo.name }}</span></div>
          <div class="field"><span class="label">科室</span><span class="value">{{ myInfo.deptName }}</span></div>
          <div class="field"><span class="label">职称</span><span class="value">{{ myInfo.title }}</span></div>
          <div class="field"><span class="label">门诊编号</span><span class="value">{{ myInfo.clinicId }}</span></div>
        </div>
        <div class="info-col">
          <div class="field"><span class="label">医生编号</span><span class="value">{{ myInfo.doctorId }}</span></div>
          <div class="field"><span class="label">用户编号</span><span class="value">{{ myInfo.userId }}</span></div>
          <div class="field field--stack">
            <span class="label">简介</span>
            <span class="value value--muted">{{ myInfo.introduction || '暂无简介' }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { updateUserAvatar } from '@/api/patient/myInfo';
import { getCurrentDoctorInfo } from '@/api/doctor/myInfo';
import { ElMessage } from 'element-plus';

defineOptions({
  name: 'DoctorMyInfoView',
});

interface DoctorInfoState {
  doctorId: number;
  userId: number;
  name: string;
  clinicId: number;
  deptName: string;
  title: string;
  introduction: string;
  avatar: string;
}

const loading = ref(true);
const newAvatar = ref(false);

const myInfo = reactive<DoctorInfoState>({
  doctorId: 0,
  userId: 0,
  avatar: '/src/assets/doctor.png',
  name: '默认医生',
  clinicId: 0,
  deptName: '',
  title: '',
  introduction: '',
});

const showEditTip = () => {
  ElMessage.info('医生信息通常由管理员维护；此处暂仅支持修改头像。')
}

const loadDoctorInfo = async () => {
  const info = await getCurrentDoctorInfo();
  loading.value = false;
  if (info) {
    Object.assign(myInfo, info);
  }
}

const handleUP = (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  if (file.type !== 'image/jpeg' && file.type !== 'image/png') {
    ElMessage.error('不支持该文件类型，请选择 JPEG、PNG 格式的图片');
    return;
  }

  const maxSize = 2 * 1024 * 1024;
  if (file.size > maxSize) {
    ElMessage.error('文件大小超过限制，请选择小于 2MB 的图片');
    return;
  }
  myInfo.avatar = URL.createObjectURL(file);
  newAvatar.value = true;
}

const uploadAvatar = async () => {
  const file = (document.getElementById('file') as HTMLInputElement).files?.[0];
  if (!file) return;
  updateUserAvatar(file).then((res) => {
    if (res) {
      newAvatar.value = false;
      ElMessage.success('头像修改成功');
      loadDoctorInfo();
    }
  }).catch((err) => {
    console.error(err);
  });
}

onMounted(async () => {
  await loadDoctorInfo();
})
</script>

<style scoped lang="scss">
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1.5rem 2rem;
  min-height: 100%;
  box-sizing: border-box;
}

.profile-page {
  margin: 0 auto;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #eef2ff, #fdf2f8);
  border-radius: 20px;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.1);

  .eyebrow {
    margin: 0;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: #7c8db5;
    font-size: 0.8rem;
  }

  h2 {
    margin: 0.35rem 0;
    font-size: 2rem;
    color: #1f2933;
  }

  .subtext {
    margin: 0;
    color: #64748b;
  }
}

.profile-card {
  border-radius: 18px;
  box-shadow: 0 15px 35px rgba(15, 23, 42, 0.08);
  background-color: #fff;
  padding: 1.75rem 2rem;
  display: flex;
  gap: 2rem;
  align-items: flex-start;
  box-sizing: border-box;
}

.avatar-panel {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  align-items: center;
  text-align: center;
  min-width: 210px;

  .avatar {
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  }

  .upload-btn {
    cursor: pointer;
    display: inline-block;
    margin-top: 0.5rem;
    padding: 0.4rem 0.9rem;
    background: #409eff;
    color: #fff;
    border-radius: 10px;
    border: 1px solid #fff;
    font-weight: 600;
    transition: background-color 0.2s ease;

    &:hover {
      background-color: #66b1ff;
    }
  }
}

.info-panel {
  flex: 1;
  display: flex;
  gap: 2.5rem;
}

.info-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 2rem 0;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.label {
  width: 88px;
  flex: 0 0 88px;
  color: #64748b;
  font-weight: 600;
}

.value {
  color: #0f172a;
  font-weight: 600;
  word-break: break-word;
}

.field--stack {
  align-items: flex-start;
}

.value--muted {
  color: #475569;
  font-weight: 500;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .profile-page {
    padding: 1rem;
  }

  .page-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .profile-card {
    flex-direction: column;
    padding: 1.25rem;
    gap: 1.25rem;
  }

  .avatar-panel {
    min-width: unset;
    width: 100%;
  }

  .info-panel {
    flex-direction: column;
    gap: 0.5rem;
  }

  .label {
    width: 76px;
    flex-basis: 76px;
  }
}
</style>
