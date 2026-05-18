<template>
  <div class="rag-manager">
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="添加文档" name="add">
        <div class="tab-content">
          <el-form :model="addForm" label-width="100px">
            <el-form-item label="文档内容">
              <el-input
                v-model="addForm.text"
                type="textarea"
                :rows="6"
                placeholder="请输入要添加到知识库的文档内容"
              />
            </el-form-item>
            <el-form-item label="元数据">
              <el-input
                v-model="addForm.metadataJson"
                type="textarea"
                :rows="3"
                placeholder='可选，JSON格式，如：{"category": "症状", "source": "医学手册"}'
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleAddDocument" :loading="adding">
                添加文档
              </el-button>
              <el-button @click="resetAddForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="PDF上传" name="pdf">
        <div class="tab-content">
          <el-upload
            ref="uploadRef"
            class="pdf-upload"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".pdf"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽PDF文件到此处，或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                仅支持PDF文件，文件将被自动拆分为多个文档片段入库
              </div>
            </template>
          </el-upload>

          <div v-if="selectedFile" class="selected-file">
            <el-tag type="info" size="large" closable @close="clearFile">
              {{ selectedFile.name }} ({{ formatFileSize(selectedFile.size) }})
            </el-tag>
          </div>

          <el-button
            type="primary"
            @click="handleUploadPdf"
            :loading="uploading"
            :disabled="!selectedFile"
            class="upload-btn"
          >
            上传并入库
          </el-button>

          <div v-if="uploadResult" class="upload-result">
            <el-alert
              :title="`PDF入库成功，共 ${uploadResult} 个文档片段`"
              type="success"
              show-icon
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="检索测试" name="search">
        <div class="tab-content">
          <el-form :model="searchForm" label-width="100px">
            <el-form-item label="检索问题">
              <el-input
                v-model="searchForm.query"
                placeholder="请输入检索问题"
                @keypress.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item label="返回条数">
              <el-input-number v-model="searchForm.topK" :min="1" :max="20" />
            </el-form-item>
            <el-form-item label="相似度阈值">
              <el-slider v-model="searchForm.similarityThreshold" :min="0" :max="1" :step="0.1" show-input />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch" :loading="searching">
                检索
              </el-button>
            </el-form-item>
          </el-form>

          <div v-if="searchResults.length > 0" class="search-results">
            <h4>检索结果 ({{ searchResults.length }} 条)</h4>
            <el-card v-for="(result, index) in searchResults" :key="result.id" class="result-card">
              <template #header>
                <span>结果 {{ index + 1 }} - ID: {{ result.id }}</span>
              </template>
              <div class="result-text">{{ result.text }}</div>
              <div v-if="result.metadata" class="result-metadata">
                <el-tag v-for="(value, key) in result.metadata" :key="key" size="small">
                  {{ key }}: {{ value }}
                </el-tag>
              </div>
            </el-card>
          </div>
          <el-empty v-else-if="hasSearched" description="未找到相关文档" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import type { UploadInstance, UploadFile, UploadRawFile } from 'element-plus';
import { addRagDocuments, searchRagDocuments, uploadPdf, type RagSearchResult } from '@/api/admin/rag';

const activeTab = ref('add');

// 添加文档表单
const addForm = reactive({
  text: '',
  metadataJson: '',
});

// 检索表单
const searchForm = reactive({
  query: '',
  topK: 5,
  similarityThreshold: 0.5,
});

const adding = ref(false);
const searching = ref(false);
const searchResults = ref<RagSearchResult[]>([]);
const hasSearched = ref(false);

// PDF上传相关
const uploadRef = ref<UploadInstance>();
const selectedFile = ref<File | null>(null);
const uploading = ref(false);
const uploadResult = ref<number | null>(null);

const handleFileChange = (file: UploadFile) => {
  if (file.raw) {
    selectedFile.value = file.raw;
    uploadResult.value = null;
  }
};

const handleExceed = (files: File[]) => {
  uploadRef.value?.clearFiles();
  if (files[0]) {
    selectedFile.value = files[0];
    uploadResult.value = null;
  }
};

const clearFile = () => {
  selectedFile.value = null;
  uploadResult.value = null;
  uploadRef.value?.clearFiles();
};

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
};

const handleUploadPdf = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择PDF文件');
    return;
  }

  uploading.value = true;
  try {
    const count = await uploadPdf(selectedFile.value);
    uploadResult.value = count;
    ElMessage.success(`PDF入库成功，共 ${count} 个文档片段`);
    clearFile();
  } catch {
    uploadResult.value = null;
  } finally {
    uploading.value = false;
  }
};

const resetAddForm = () => {
  addForm.text = '';
  addForm.metadataJson = '';
};

const handleAddDocument = async () => {
  if (!addForm.text.trim()) {
    ElMessage.warning('请输入文档内容');
    return;
  }

  let metadata: Record<string, unknown> | undefined;
  if (addForm.metadataJson.trim()) {
    try {
      metadata = JSON.parse(addForm.metadataJson);
    } catch {
      ElMessage.error('元数据JSON格式错误');
      return;
    }
  }

  adding.value = true;
  try {
    const count = await addRagDocuments([
      {
        text: addForm.text,
        metadata,
      },
    ]);
    ElMessage.success(`文档添加成功，共入库 ${count} 条`);
    resetAddForm();
  } catch {
    // 错误已在API层处理
  } finally {
    adding.value = false;
  }
};

const handleSearch = async () => {
  if (!searchForm.query.trim()) {
    ElMessage.warning('请输入检索问题');
    return;
  }

  searching.value = true;
  hasSearched.value = true;
  try {
    searchResults.value = await searchRagDocuments(
      searchForm.query,
      searchForm.topK,
      searchForm.similarityThreshold
    );
  } catch {
    searchResults.value = [];
  } finally {
    searching.value = false;
  }
};
</script>

<style lang="scss" scoped>
.rag-manager {
  padding: 1rem;
  height: 100%;
  overflow: auto;
}

.tab-content {
  padding: 1rem;
}

.pdf-upload {
  width: 100%;
  max-width: 500px;
}

.selected-file {
  margin: 1rem 0;
}

.upload-btn {
  margin-top: 1rem;
}

.upload-result {
  margin-top: 1rem;
  max-width: 500px;
}

.search-results {
  margin-top: 1.5rem;

  h4 {
    margin-bottom: 1rem;
    color: #303133;
  }
}

.result-card {
  margin-bottom: 1rem;

  .result-text {
    white-space: pre-wrap;
    word-break: break-all;
    line-height: 1.6;
  }

  .result-metadata {
    margin-top: 0.5rem;
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
  }
}
</style>
