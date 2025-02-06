<template>
  <div class="app-container">
    <!-- 顶部标题栏 -->
    <div class="app-header">
      <div class="header-content">
        <div class="logo">
          <el-icon><Link /></el-icon>
        </div>
        <h1>区块链备份验证工具</h1>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <div class="tools-container">
        <!-- 左侧功能卡片 -->
        <div class="tool-card">
          <div class="card-header">
            <div class="header-icon">
              <el-icon><Download /></el-icon>
            </div>
            <div class="header-title">
              <h2>EOS区块数据获取</h2>
              <el-tag size="small" :type="status === '就绪' ? 'info' : 'primary'">
                {{ status }}
              </el-tag>
            </div>
          </div>

          <div class="card-body">
            <div class="form-container">
              <el-form :model="formData" :rules="rules" ref="formRef" label-position="top">
                <el-form-item label="选择EOS节点" prop="apiSource">
                  <el-radio-group v-model="formData.apiSource" @change="handleApiSourceChange">
                    <el-radio label="preset">使用预设节点</el-radio>
                    <el-radio label="custom">自定义节点</el-radio>
                  </el-radio-group>
                </el-form-item>

                <el-form-item 
                  v-if="formData.apiSource === 'preset'" 
                  label="预设节点列表"
                  prop="apiUrl"
                >
                  <el-select 
                    v-model="selectedApi" 
                    placeholder="请选择节点" 
                    class="full-width"
                    @change="handleApiSelect">
                    <el-option
                      v-for="api in predefinedApis"
                      :key="api.url"
                      :label="api.name"
                      :value="api.url"
                    >
                      <span class="api-option">
                        <span>{{ api.name }}</span>
                        <el-tag size="small" type="success">{{ api.location }}</el-tag>
                      </span>
                    </el-option>
                  </el-select>
                </el-form-item>

                <el-form-item 
                  v-if="formData.apiSource === 'custom'" 
                  label="自定义API地址" 
                  prop="apiUrl"
                >
                  <el-input 
                    v-model="formData.apiUrl" 
                    placeholder="例如：https://api.main.alohaeos.com/v1/chain/get_block">
                  </el-input>
                </el-form-item>
                
                <div class="block-range">
                  <el-form-item label="开始区块" prop="startBlock">
                    <el-input-number 
                      v-model="formData.startBlock" 
                      :min="1"
                      controls-position="right">
                    </el-input-number>
                  </el-form-item>
                  
                  <el-form-item label="结束区块" prop="endBlock">
                    <el-input-number 
                      v-model="formData.endBlock" 
                      :min="1"
                      controls-position="right">
                    </el-input-number>
                  </el-form-item>
                </div>

                <div class="form-footer">
                  <el-button type="primary" @click="submitForm" :loading="loading">
                    {{ loading ? '正在获取数据...' : '开始获取数据' }}
                  </el-button>
                  <el-button @click="resetForm">重置表单</el-button>
                </div>
              </el-form>

              <!-- 进度显示部分 -->
              <div v-if="loading || (status !== '就绪' && status !== '完成')" 
                   class="progress-section">
                <div class="progress-header">
                  <span class="progress-title">数据获取进度</span>
                  <span class="current-block" v-if="currentBlock">
                    正在获取区块: {{ currentBlock }}
                  </span>
                </div>
                
                <el-progress 
                  :percentage="progressPercentage"
                  :format="progressFormat"
                  :status="progressStatus"
                  :stroke-width="20"
                  :color="progressColor"
                />
                
                <div class="progress-stats">
                  <div class="stat-item">
                    <span class="stat-value">{{ processedBlocks }}</span>
                    <span class="stat-label">已处理区块</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-value">{{ totalBlocksCount }}</span>
                    <span class="stat-label">总区块数</span>
                  </div>
                  <div class="stat-item time">
                    <span class="stat-value">{{ estimatedTimeRemaining }}</span>
                    <span class="stat-label">预计剩余</span>
                  </div>
                </div>
              </div>

              <!-- 历史记录部分 -->
              <div v-if="history.length > 0" class="history-section">
                <h3>最近操作记录</h3>
                <el-timeline>
                  <el-timeline-item
                    v-for="(record, index) in history"
                    :key="index"
                    :timestamp="record.time"
                    :type="getStatusTag(record.status).type">
                    区块 {{ record.startBlock }} - {{ record.endBlock }}
                    <el-tag 
                      size="small" 
                      :type="getStatusTag(record.status).type">
                      {{ getStatusTag(record.status).text }}
                    </el-tag>
                  </el-timeline-item>
                </el-timeline>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧功能卡片 -->
        <div class="tool-card">
          <div class="card-header">
            <div class="header-icon">
              <el-icon><Check /></el-icon>
            </div>
            <div class="header-title">
              <h2>区块数据在线验证</h2>
              <el-tag size="small" :type="verifyStatus === '就绪' ? 'info' : 'primary'">
                {{ verifyStatus }}
              </el-tag>
            </div>
          </div>

          <div class="card-body">
            <div class="upload-container">
              <el-upload
                class="upload-demo"
                drag
                action="#"
                :auto-upload="false"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
                :limit="1"
                :file-list="fileList"
                accept=".json,.txt"
                :before-upload="handleBeforeUpload"
              >
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">
                  拖拽文件到此处或 <em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">
                    请上传包含区块信息的JSON或TXT文件（大小不超过100GB）
                  </div>
                </template>
              </el-upload>

              <!-- 修改验证控件部分 -->
              <div class="verify-controls" v-if="fileList.length > 0">
                <el-form-item label="选择节点来源">
                  <el-radio-group v-model="verifyApiSource" @change="handleVerifyApiSourceChange">
                    <el-radio label="preset">使用预设节点</el-radio>
                    <el-radio label="custom">自定义节点</el-radio>
                  </el-radio-group>
                </el-form-item>
                
                <div class="verify-api-select-container">
                  <!-- 预设节点选择 -->
                  <el-select 
                    v-if="verifyApiSource === 'preset'"
                    v-model="selectedVerifyApi" 
                    placeholder="选择验证用的EOS节点" 
                    class="verify-api-select">
                    <el-option
                      v-for="api in predefinedApis"
                      :key="api.url"
                      :label="api.name"
                      :value="api.url"
                    />
                  </el-select>
                  
                  <!-- 自定义节点输入 -->
                  <el-input
                    v-if="verifyApiSource === 'custom'"
                    v-model="customVerifyApi"
                    placeholder="请输入自定义API地址"
                    class="verify-api-input"
                  />
                </div>
                <div class="verify-buttons">
                  <el-button 
                    type="primary" 
                    @click="startVerification"
                    :loading="verifying"
                    size="large">
                    {{ verifying ? '正在验证...' : '开始验证' }}
                  </el-button>
                </div>
              </div>

              <!-- 添加验证进度显示部分 -->
              <div v-if="verifying || verificationResults.value && verificationResults.value.length > 0" 
                   class="progress-section verify-progress">
                <div class="progress-header">
                  <span class="progress-title">验证进度</span>
                  <span class="current-block" v-if="currentVerifyBlock">
                    正在验证区块: {{ currentVerifyBlock }}
                  </span>
                </div>
                
                <el-progress 
                  :percentage="verifyProgressPercentage"
                  :format="verifyProgressFormat"
                  :status="verifyProgressStatus"
                  :stroke-width="20"
                  :color="verifyProgressColor"
                />
                
                <div class="progress-stats">
                  <div class="stat-item">
                    <span class="stat-value">{{ verifiedBlocks }}</span>
                    <span class="stat-label">已验证区块</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-value">{{ totalVerifyBlocks }}</span>
                    <span class="stat-label">总区块数</span>
                  </div>
                  <div class="stat-item time">
                    <span class="stat-value">{{ estimatedVerifyTimeRemaining }}</span>
                    <span class="stat-label">预计剩余</span>
                  </div>
                </div>
              </div>

              <!-- 验证结果表格 -->
              <div v-if="verificationResults.value && verificationResults.value.length > 0" class="verification-results">
                <h3>验证结果</h3>
                <el-table :data="verificationResults.value" style="width: 100%" border stripe>
                  <el-table-column prop="blockNum" label="区块号" width="100" />
                  <el-table-column label="数据匹配" width="120">
                    <template #default="scope">
                      <el-tag :type="scope.row.dataMatch ? 'success' : 'danger'">
                        {{ scope.row.dataMatch ? '匹配' : '不匹配' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="哈希匹配" width="120">
                    <template #default="scope">
                      <el-tag :type="scope.row.hashMatch ? 'success' : 'danger'">
                        {{ scope.row.hashMatch ? '匹配' : '不匹配' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="链上哈希" min-width="280">
                    <template #default="scope">
                      <el-tooltip 
                        :content="scope.row.chainHash" 
                        placement="top"
                        :disabled="!scope.row.chainHash || scope.row.chainHash === '获取中...'">
                        <span class="hash-text">{{ scope.row.chainHash }}</span>
                      </el-tooltip>
                    </template>
                  </el-table-column>
                  <el-table-column label="本地哈希" min-width="280">
                    <template #default="scope">
                      <el-tooltip 
                        :content="scope.row.localHash" 
                        placement="top"
                        :disabled="!scope.row.localHash || scope.row.localHash === '计算中...'">
                        <span class="hash-text">{{ scope.row.localHash }}</span>
                      </el-tooltip>
                    </template>
                  </el-table-column>
                  <el-table-column label="本地连续性" width="120">
                    <template #default="scope">
                      <el-tag :type="scope.row.isLocalContinuous ? 'success' : 'danger'">
                        {{ scope.row.isLocalContinuous ? '连续' : '不连续' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="链上连续性" width="120">
                    <template #default="scope">
                      <el-tag :type="scope.row.isChainContinuous ? 'success' : 'danger'">
                        {{ scope.row.isChainContinuous ? '连续' : '不连续' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                </el-table>

                <!-- 验证统计信息 -->
                <div class="verification-summary">
                  <div class="summary-item">
                    <span class="label">总区块数:</span>
                    <span class="value">{{ verificationResults.value.length }}</span>
                  </div>
                  <div class="summary-item">
                    <span class="label">完全匹配:</span>
                    <span class="value success">{{ getMatchCount('success') }}</span>
                  </div>
                  <div class="summary-item">
                    <span class="label">部分匹配:</span>
                    <span class="value warning">{{ getMatchCount('partial') }}</span>
                  </div>
                  <div class="summary-item">
                    <span class="label">不匹配:</span>
                    <span class="value danger">{{ getMatchCount('error') }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { UploadFilled } from '@element-plus/icons-vue'

// 添加所有响应式变量的声明
const formRef = ref(null)
const loading = ref(false)
const status = ref('就绪')
const processedBlocks = ref(0)
const totalBlocksCount = ref(0)
const selectedApi = ref('')
const apiSource = ref('preset')
const processStartTime = ref(null)
const lastProgressUpdate = ref(null)
const currentBlock = ref(null)
const lastProcessTime = ref(Date.now())
const blockProcessTimes = ref([])
const progressInterval = ref(null)
const isProcessing = ref(false)

// 验证相关的响应式变量
const fileList = ref([])
const verifyStatus = ref('就绪')
const verifying = ref(false)
const verificationResults = ref([])
const currentVerifyBlock = ref(null)
const verifiedBlocks = ref(0)
const totalVerifyBlocks = ref(0)
const verifyStartTime = ref(null)
const selectedVerifyApi = ref('')
const verifyProgressInterval = ref(null)
const verifyApiSource = ref('preset')
const customVerifyApi = ref('')

const predefinedApis = [
  // 主网节点
  { 
    name: 'Greymass', 
    url: 'https://eos.greymass.com/v1/chain/get_block',
    location: '主网',
    network: 'mainnet'
  },
  { 
    name: 'EOSphere Node2', 
    url: 'https://node2.eosphere.io/v1/chain/get_block',
    location: '主网',
    network: 'mainnet'
  },
  {
    name: 'Aloha EOS',
    url: 'https://api.main.alohaeos.com/v1/chain/get_block',
    location: '主网',
    network: 'mainnet'
  }
]

const formData = reactive({
  apiSource: 'preset',
  apiUrl: '',
  startBlock: 1,
  endBlock: 5
})

const history = ref([])

const rules = {
  apiUrl: [
    { 
      required: true, 
      message: '请输入API地址', 
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (apiSource.value === 'custom' && !value) {
          callback(new Error('请输入API地址'))
        } else {
          callback()
        }
      }
    }
  ],
  apiSource: [
    { required: true, message: '请选择API来源', trigger: 'change' }
  ],
  startBlock: [
    { required: true, message: '请输入开始区块', trigger: 'blur' },
    { 
      type: 'number',
      min: 1,
      message: '开始区块必须大于0',
      trigger: 'blur'
    }
  ],
  endBlock: [
    { required: true, message: '请输入结束区块', trigger: 'blur' },
    { 
      type: 'number',
      min: 1,
      message: '结束区块必须大于0',
      trigger: 'blur'
    },
    {
      validator: (rule, value, callback) => {
        if (value && formData.startBlock && value <= formData.startBlock) {
          callback(new Error('结束区块必须大于开始区块'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change']
    }
  ]
}

const totalBlocks = computed(() => {
  return formData.endBlock - formData.startBlock + 1
})

const progressPercentage = computed(() => {
  if (!processedBlocks.value || !totalBlocksCount.value) return 0
  const percentage = Math.round((processedBlocks.value / totalBlocksCount.value) * 100)
  // 确保只有在真正完成时才显示100%
  return Math.min(percentage, isProcessing.value ? 99 : 100)
})

const progressFormat = (percentage) => {
  return `${percentage}% (${processedBlocks.value}/${totalBlocksCount.value})`
}

const progressStatus = computed(() => {
  if (!loading.value) return ''
  if (status.value.includes('失败')) return 'exception'
  if (status.value.includes('重试')) return 'warning'
  if (processedBlocks.value === totalBlocksCount.value && !isProcessing.value) return 'success'
  return ''
})

const estimatedTimeRemaining = computed(() => {
  if (!processStartTime.value || processedBlocks.value === 0) {
    return '计算中...'
  }

  const elapsedTime = Date.now() - processStartTime.value
  const blocksRemaining = totalBlocksCount.value - processedBlocks.value
  const averageTimePerBlock = elapsedTime / processedBlocks.value

  // 如果已经完成
  if (blocksRemaining <= 0) {
    return '即将完成'
  }

  const estimatedRemainingMs = blocksRemaining * averageTimePerBlock

  // 转换为更友好的格式
  if (estimatedRemainingMs < 1000) {
    return '即将完成'
  }

  const minutes = Math.floor(estimatedRemainingMs / 60000)
  const seconds = Math.floor((estimatedRemainingMs % 60000) / 1000)

  if (minutes > 0) {
    return `${minutes}分${seconds}秒`
  }
  return `${seconds}秒`
})

const handleApiSourceChange = (value) => {
  formData.apiSource = value
  if (value === 'preset') {
    formData.apiUrl = selectedApi.value || ''
  } else {
    formData.apiUrl = ''
  }
}

const handleApiSelect = async (url) => {
  formData.apiUrl = url
  try {
    const validateResponse = await axios.post('http://localhost:8081/api/blocks/validate', {
      apiUrl: url
    })
    
    if (!validateResponse.data.success) {
      ElMessage.warning('当前节点可能不可用：' + validateResponse.data.message)
    }
  } catch (error) {
    ElMessage.warning('节点验证失败，请检查网络连接')
  }
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
    processedBlocks.value = 0
    totalBlocksCount.value = 0
    status.value = '就绪'
    isProcessing.value = false
    if (progressInterval.value) {
      clearInterval(progressInterval.value)
      progressInterval.value = null
    }
    loading.value = false
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  
  try {
    if (formData.apiSource === 'preset' && !formData.apiUrl) {
      ElMessage.error('请选择预设节点')
      return
    }
    
    await formRef.value.validate()
    
    // 先验证API
    const validateResponse = await axios.post('http://localhost:8081/api/blocks/validate', {
      apiUrl: formData.apiUrl
    })
    
    if (!validateResponse.data.success) {
      ElMessage.error('API验证失败：' + validateResponse.data.message)
      return
    }
    
    // 重置进度相关状态
    processedBlocks.value = 0
    totalBlocksCount.value = 0
    processStartTime.value = Date.now()
    isProcessing.value = true
    
    const response = await axios.post('http://localhost:8081/api/blocks/fetch', {
      apiUrl: formData.apiUrl,
      startBlock: formData.startBlock,
      endBlock: formData.endBlock
    })
    
    if (response.data.success) {
      ElMessage.success('数据获取任务已开始')
      startProgressPolling()
    } else {
      ElMessage.error('数据获取失败：' + response.data.message)
      loading.value = false
      status.value = '错误'
      isProcessing.value = false
    }
  } catch (error) {
    ElMessage.error(error.message || '表单验证失败或提交出错')
    console.error(error)
    loading.value = false
    status.value = '错误'
    isProcessing.value = false
  }
}

// 计算进度条颜色
const progressColor = computed(() => {
  if (blockProcessTimes.value.length < 2) return '#409EFF' // 默认蓝色
  
  // 计算最近的处理速度（毫秒/区块）
  const recentTimes = blockProcessTimes.value.slice(-5)
  const avgTime = recentTimes.reduce((a, b) => a + b, 0) / recentTimes.length
  
  // 根据速度返回不同的颜色
  if (avgTime < 1000) return '#409EFF'      // 蓝色 - 很快（<1秒/区块）
  if (avgTime < 2000) return '#67C23A'      // 绿色 - 正常（<2秒/区块）
  if (avgTime < 5000) return '#E6A23C'      // 黄色 - 较慢（<5秒/区块）
  return '#F56C6C'                          // 红色 - 很慢（>=5秒/区块）
})

// 修改进度轮询逻辑
const startProgressPolling = () => {
  if (progressInterval.value) {
    clearInterval(progressInterval.value)
  }
  
  status.value = '处理中'
  loading.value = true
  
  progressInterval.value = setInterval(async () => {
    try {
      const response = await axios.get(`http://localhost:8081/api/blocks/progress`)
      if (response.data) {
        const { processedBlocks: processed, totalBlocks: total, isProcessing, hasError, currentBlockNum } = response.data
        
        // 更新进度信息
        processedBlocks.value = processed
        totalBlocksCount.value = total
        currentBlock.value = currentBlockNum
        
        // 计算处理时间
        const now = Date.now()
        if (processed > processedBlocks.value) {
          const timePerBlock = (now - lastProcessTime.value)
          blockProcessTimes.value.push(timePerBlock)
          // 只保留最近10个时间记录
          if (blockProcessTimes.value.length > 10) {
            blockProcessTimes.value.shift()
          }
        }
        lastProcessTime.value = now
        
        // 只有在后端确实停止处理，并且所有区块都处理完成时才结束
        if (!isProcessing && processed === total) {
          clearInterval(progressInterval.value)
          progressInterval.value = null
          currentBlock.value = null
          
          if (hasError) {
            status.value = '部分失败'
            ElMessage.warning('部分区块数据获取失败，请查看日志了解详情')
          } else {
            processedBlocks.value = total
            setTimeout(() => {
              status.value = '完成'
              ElMessage.success('数据获取完成')
              loading.value = false
            }, 500)
          }
          
          history.value.unshift({
            time: new Date().toLocaleString(),
            startBlock: formData.startBlock,
            endBlock: formData.endBlock,
            status: hasError ? 'partial' : 'success'
          })
        }
      }
    } catch (error) {
      console.error('获取进度失败:', error)
      status.value = '进度获取失败'
    }
  }, 1000)
}

// 更新历史记录的显示
const getStatusTag = (status) => {
  switch (status) {
    case 'success':
      return { type: 'success', text: '成功' }
    case 'partial':
      return { type: 'warning', text: '部分成功' }
    case 'error':
      return { type: 'danger', text: '失败' }
    default:
      return { type: 'info', text: '未知' }
  }
}

// 组件卸载时清理定时器
onUnmounted(() => {
  if (progressInterval.value) {
    clearInterval(progressInterval.value)
  }
})

// 添加验证进度百分比计算
const verifyProgressPercentage = computed(() => {
  if (!verifiedBlocks.value || !totalVerifyBlocks.value) return 0
  const percentage = Math.round((verifiedBlocks.value / totalVerifyBlocks.value) * 100)
  return Math.min(percentage, verifying.value ? 99 : 100)
})

// 添加验证进度格式化函数
const verifyProgressFormat = (percentage) => {
  return `${percentage}% (${verifiedBlocks.value}/${totalVerifyBlocks.value})`
}

// 添加验证进度状态计算
const verifyProgressStatus = computed(() => {
  if (!verifying.value) return ''
  if (verifyStatus.value.includes('失败')) return 'exception'
  if (verifyStatus.value === '验证完成') return 'success'
  return ''
})

// 添加验证剩余时间计算
const estimatedVerifyTimeRemaining = computed(() => {
  if (!verifyStartTime.value || verifiedBlocks.value === 0) {
    return '计算中...'
  }

  const elapsedTime = Date.now() - verifyStartTime.value
  const blocksRemaining = totalVerifyBlocks.value - verifiedBlocks.value
  const averageTimePerBlock = elapsedTime / verifiedBlocks.value

  if (blocksRemaining <= 0) {
    return '即将完成'
  }

  const estimatedRemainingMs = blocksRemaining * averageTimePerBlock

  if (estimatedRemainingMs < 1000) {
    return '即将完成'
  }

  const minutes = Math.floor(estimatedRemainingMs / 60000)
  const seconds = Math.floor((estimatedRemainingMs % 60000) / 1000)

  if (minutes > 0) {
    return `${minutes}分${seconds}秒`
  }
  return `${seconds}秒`
})

// 修改开始验证函数
const startVerification = async () => {
  const apiUrl = verifyApiSource.value === 'preset' ? selectedVerifyApi.value : customVerifyApi.value
  if (!apiUrl) {
    ElMessage.error('请选择EOS节点')
    return
  }

  if (fileList.value.length === 0) {
    ElMessage.error('请先上传文件')
    return
  }

  try {
    verifying.value = true
    verifyStatus.value = '验证中'
    verifyStartTime.value = Date.now()
    verifiedBlocks.value = 0
    verificationResults.value = []

    const formData = new FormData()
    formData.append('file', fileList.value[0].raw)
    formData.append('apiUrl', apiUrl)

    // 开始验证进度轮询
    startVerifyProgressPolling()

    const response = await axios.post('http://localhost:8081/api/blocks/verify', 
      formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }
    )

    if (!response.data.success) {
      throw new Error(response.data.message || '验证失败')
    }

  } catch (error) {
    console.error('验证失败:', error)
    ElMessage.error('验证过程发生错误: ' + error.message)
    verifyStatus.value = '验证失败'
    verifying.value = false
    stopVerifyProgressPolling()
  }
}

// 修改验证进度轮询函数
const startVerifyProgressPolling = () => {
  if (verifyProgressInterval.value) {
    clearInterval(verifyProgressInterval.value)
  }

  verifyProgressInterval.value = setInterval(async () => {
    try {
      const response = await axios.get('http://localhost:8081/api/blocks/verify/progress')
      console.log('Progress response:', response.data)
      
      if (response.data) {
        const { currentBlock, verifiedCount, totalBlocks, isVerifying, verificationResults: results } = response.data
        
        // 更新进度信息
        currentVerifyBlock.value = currentBlock
        verifiedBlocks.value = verifiedCount
        totalVerifyBlocks.value = totalBlocks || 0

        // 如果有验证结果，直接使用它
        if (results && Array.isArray(results)) {
          console.log('Setting verification results:', results)
          verificationResults.value = results.map(result => ({
            blockNum: result.blockNum,
            dataMatch: result.dataMatch,
            hashMatch: result.hashMatch,
            chainHash: result.chainHash || '-',
            localHash: result.localHash || '-',
            isLocalContinuous: result.isLocalContinuous,
            isChainContinuous: result.isChainContinuous
          }))
        }

        if (!isVerifying) {
          const finalResponse = await axios.get('http://localhost:8081/api/blocks/verify/results')
          console.log('Final verification results:', finalResponse.data)
          
          if (finalResponse.data.success && Array.isArray(finalResponse.data.results)) {
            console.log('Setting final results:', finalResponse.data.results)
            verificationResults.value = finalResponse.data.results.map(result => ({
              blockNum: result.blockNum,
              dataMatch: result.dataMatch,
              hashMatch: result.hashMatch,
              chainHash: result.chainHash || '-',
              localHash: result.localHash || '-',
              isLocalContinuous: result.isLocalContinuous,
              isChainContinuous: result.isChainContinuous
            }))
            
            console.log('Final processed results:', verificationResults.value)
            verifyStatus.value = '验证完成'
            ElMessage.success('验证完成')
            // 清除上传的文件
            fileList.value = []
          }
          verifying.value = false
          stopVerifyProgressPolling()
        }
      }
    } catch (error) {
      console.error('获取验证进度失败:', error)
      ElMessage.error('获取验证进度失败: ' + error.message)
      // 发生错误时也清除文件
      fileList.value = []
    }
  }, 1000)
}

// 修改停止验证进度轮询函数
const stopVerifyProgressPolling = () => {
  if (verifyProgressInterval.value) {
    clearInterval(verifyProgressInterval.value)
    verifyProgressInterval.value = null
  }
}

// 修改进度条颜色计算属性的名称
const verifyProgressColor = computed(() => {
  const percentage = verifyProgressPercentage.value
  if (percentage < 30) return '#909399'
  if (percentage < 70) return '#E6A23C'
  return '#67C23A'
})

// 修改验证结果统计函数
const getMatchCount = (status) => {
  const results = verificationResults.value
  if (!results || results.length === 0) return 0
  
  console.log('Calculating match count for status:', status, 'with results:', results)
  
  switch (status) {
    case 'success':
      return results.filter(r => r.dataMatch && r.hashMatch).length
    case 'partial':
      return results.filter(r => (r.dataMatch || r.hashMatch) && !(r.dataMatch && r.hashMatch)).length
    case 'error':
      return results.filter(r => !r.dataMatch && !r.hashMatch).length
    default:
      return 0
  }
}

// 确保在组件卸载时清理定时器
onUnmounted(() => {
  stopVerifyProgressPolling()
})

// 修改文件上传处理函数
const handleFileChange = (uploadFile) => {
  console.log('File changed:', uploadFile)
  
  // 检查文件类型
  const validTypes = ['.json', '.txt']
  const fileExtension = uploadFile.name.substring(uploadFile.name.lastIndexOf('.')).toLowerCase()
  
  if (!validTypes.includes(fileExtension)) {
    ElMessage.error('只支持 JSON 或 TXT 格式的文件')
    fileList.value = []
    return false
  }
  
  // 更新文件列表
  fileList.value = [uploadFile]
  console.log('Current fileList:', fileList.value)
  return true
}

// 修改文件移除处理函数
const handleFileRemove = () => {
  console.log('File removed')
  fileList.value = []
  console.log('Current fileList after remove:', fileList.value)
}

// 修改上传前处理函数
const handleBeforeUpload = (file) => {
  console.log('Before upload:', file) // 添加日志
  
  // 检查文件大小（100GB = 100 * 1024 * 1024 * 1024 字节）
  const maxSize = 100 * 1024 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过100GB')
    return false
  }
  return true
}

// 确保在组件卸载时清理文件列表
onUnmounted(() => {
  fileList.value = []
  stopVerifyProgressPolling()
})

// 由于只有主网节点，简化分组逻辑
const apiGroups = computed(() => {
  return [{
    label: '主网节点',
    apis: predefinedApis
  }]
})

// 简化标签类型函数
const getNetworkTagType = () => 'success'

// 添加验证控件部分
const handleVerifyApiSourceChange = (value) => {
  verifyApiSource.value = value
  if (value === 'preset') {
    selectedVerifyApi.value = ''
  } else {
    customVerifyApi.value = ''
  }
}
</script>

<style scoped>
/* 基础容器样式优化 */
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: linear-gradient(135deg, #f6f8fc 0%, #f0f4f8 100%);
}

/* 顶部标题栏调整 */
.app-header {
  background: #fff;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.03);
  padding: 0;
  height: 80px;
  position: fixed;
  width: 100%;
  top: 0;
  z-index: 100;
  display: flex;
  justify-content: center;
}

.header-content {
  width: 100%;
  max-width: 1600px;
  height: 100%;
  padding: 0 40px;
  display: flex;
  align-items: center;
  justify-content: center; /* 添加居中对齐 */
  gap: 20px;
}

.logo {
  position: absolute; /* 将logo定位到左侧 */
  left: 40px;
}

.header-content h1 {
  font-size: 24px;
  color: #1a1a1a;
  font-weight: 600;
}

/* 主内容区域调整 */
.main-content {
  margin-top: 80px;
  padding: 40px 0;
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 80px);
  width: 100%;
}

.tools-container {
  width: 100%;
  max-width: 1600px;
  display: flex;
  justify-content: center; /* 添加水平居中 */
  gap: 40px;
  height: auto;
  padding: 0 40px;
}

/* 功能卡片调整 */
.tool-card {
  flex: 1;
  max-width: 760px; /* 限制最大宽度 */
  min-width: 680px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  min-height: 750px;
  max-height: calc(100vh - 140px);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.tool-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 36px rgba(0, 0, 0, 0.1);
}

/* 卡片头部调整 */
.card-header {
  padding: 32px 36px;
  display: flex;
  align-items: center;
  justify-content: flex-start; /* 确保头部内容左对齐 */
  gap: 24px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(to right, #ffffff, #f8faff);
}

.header-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f0f7ff 0%, #e6f1ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409EFF;
  font-size: 26px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.12);
}

.header-title {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

/* 表单容器调整 */
.form-container {
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
  padding: 32px;
}

/* 响应式布局优化 */
@media (max-width: 1600px) {
  .tools-container {
    flex-direction: column;
    align-items: center;
    padding: 0 20px;
  }

  .tool-card {
    max-width: 900px;
    min-width: auto;
    width: 100%;
  }

  .logo {
    left: 20px;
  }
}

@media (max-width: 960px) {
  .tools-container {
    padding: 0 16px;
  }

  .header-content {
    padding: 0 16px;
  }

  .logo {
    left: 16px;
  }

  .tool-card {
    max-width: 100%;
  }
}

/* 添加卡片内容过渡动画 */
.card-body {
  flex: 1;
  padding: 36px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.4s ease;
}

/* 美化表单组件 */
.el-form-item {
  margin-bottom: 32px;
  transition: all 0.3s ease;
}

.el-input,
.el-select,
.el-input-number {
  --el-input-border-radius: 8px;
  --el-input-height: 44px;
  font-size: 15px;
}

.el-button {
  height: 44px;
  padding: 0 32px;
  font-size: 15px;
  border-radius: 8px;
}

/* 进度条容器美化 */
.progress-section {
  background: linear-gradient(to right, #f8fafc, #f5f8ff);
  border-radius: 16px;
  padding: 28px;
  margin-top: 36px;
  border: 1px solid #edf2f7;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03);
}

/* 上传区域美化 */
.upload-container {
  padding: 40px;
  background: linear-gradient(to bottom, #ffffff, #f8faff);
  border-radius: 16px;
  margin-top: 20px;
}

.el-upload {
  border: 2px dashed #e4e7eb;
  border-radius: 16px;
  padding: 40px;
  transition: all 0.3s ease;
  background: #ffffff;
}

.el-upload:hover {
  border-color: #409EFF;
  background: rgba(64, 158, 255, 0.02);
  transform: translateY(-2px);
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.verification-results {
  animation: fadeIn 0.3s ease;
}

/* 滚动条美化 */
::-webkit-scrollbar {
  width: 8px;
}

::-webkit-scrollbar-thumb {
  background-color: #e4e7eb;
  border-radius: 4px;
}

::-webkit-scrollbar-track {
  background-color: #f8fafc;
}

/* 添加验证控件样式 */
.verify-controls {
  margin-top: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.verify-api-select-container {
  width: 100%;
}

.verify-api-select,
.verify-api-input {
  width: 100%;
}

.verify-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 确保选择框和按钮高度一致 */
.verify-controls .el-select,
.verify-controls .el-button {
  height: 44px;
}

/* 修改上传组件样式 */
.upload-demo {
  width: 100%;
  margin-bottom: 20px;
}

.el-upload {
  width: 100%;
  display: block;
}

.el-upload-dragger {
  width: 100% !important;
  height: 200px !important;
  display: flex !important;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed var(--el-border-color) !important;
  border-radius: 16px !important;
  background: var(--el-bg-color) !important;
  transition: all 0.3s ease !important;
}

.el-upload-dragger:hover,
.el-upload-dragger.is-dragover {
  border-color: var(--el-color-primary) !important;
  background: var(--el-color-primary-light-9) !important;
}

.el-icon--upload {
  font-size: 48px !important;
  color: #409EFF !important;
  margin-bottom: 16px !important;
}

.el-upload__text {
  font-size: 16px !important;
  color: #606266 !important;
  margin: 10px 0 !important;
}

.el-upload__text em {
  color: #409EFF !important;
  font-style: normal !important;
  font-weight: 600 !important;
}

.el-upload__tip {
  margin-top: 12px !important;
  font-size: 14px !important;
  color: #909399 !important;
}

/* 添加文件列表样式 */
.el-upload-list {
  margin-top: 16px;
}

.el-upload-list__item {
  transition: all 0.3s ease;
}

.el-upload-list__item:hover {
  background-color: #f5f7fa;
}

/* 添加哈希文本样式 */
.hash-text {
  font-family: monospace;
  font-size: 13px;
  color: #606266;
  display: inline-block;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  transition: all 0.3s ease;
}

.hash-text:hover {
  color: #409EFF;
}

/* 添加加载中文本样式 */
.loading-text {
  color: #909399;
  font-style: italic;
}

/* 调整表格样式 */
.verification-results .el-table {
  margin-top: 20px;
}

.verification-results .el-table th {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.verification-results .el-table td {
  padding: 8px 0;
}
</style> 