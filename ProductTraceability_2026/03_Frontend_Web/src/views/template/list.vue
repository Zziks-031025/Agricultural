<template>
  <div class="page-container">
    <!-- 模版列表 -->
    <el-card shadow="never" class="table-card">
      <div slot="header" class="card-header">
        <span>溯源模版配置</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAddTemplate">新增模版</el-button>
      </div>

      <el-table v-loading="loading" :data="templateList" border size="small" style="width: 100%"
        :row-class-name="({row}) => activeTemplate && activeTemplate.id === row.id ? 'active-row' : ''">
        <el-table-column prop="templateCode" label="模版编码" min-width="130" />
        <el-table-column prop="templateName" label="模版名称" min-width="130" />
        <el-table-column label="模版类型" width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.templateType === 1 ? 'warning' : 'success'" size="mini">{{ row.templateType === 1 ? '肉鸡' : '西红柿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productCategory" label="产品类别" width="100" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template slot-scope="{ row }">
            <el-switch :value="row.status === 1" active-color="#2d8a56" inactive-color="#dcdfe6" @change="handleToggleTemplate(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="60" align="center" />
        <el-table-column label="操作" min-width="210" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" icon="el-icon-setting" @click="handleManageStages(row)">环节</el-button>
            <el-button type="text" size="mini" icon="el-icon-edit" @click="handleEditTemplate(row)">编辑</el-button>
            <el-button type="text" size="mini" icon="el-icon-delete" class="danger-btn" @click="handleDeleteTemplate(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 环节管理面板 -->
    <el-card v-if="activeTemplate" shadow="never" class="stage-card">
      <div slot="header" class="card-header">
        <span>环节管理 — {{ activeTemplate.templateName }}</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAddStage">新增环节</el-button>
      </div>

      <el-table v-loading="stageLoading" :data="stageList" border size="small" style="width: 100%"
        :row-class-name="({row}) => activeStage && activeStage.id === row.id ? 'active-row' : ''">
        <el-table-column prop="stageCode" label="环节编码" min-width="120" />
        <el-table-column prop="stageName" label="环节名称" min-width="110" />
        <el-table-column label="环节类型" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini">{{ stageTypeLabel(row.stageType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="60" align="center" />
        <el-table-column label="必填" width="60" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.isRequired ? 'danger' : 'info'" size="mini">{{ row.isRequired ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" min-width="200" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" icon="el-icon-document" @click="handleManageFields(row)">字段</el-button>
            <el-button type="text" size="mini" icon="el-icon-edit" @click="handleEditStage(row)">编辑</el-button>
            <el-button type="text" size="mini" icon="el-icon-delete" class="danger-btn" @click="handleDeleteStage(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 字段管理面板 -->
    <el-card v-if="activeStage" shadow="never" class="field-card">
      <div slot="header" class="card-header">
        <span>字段配置 — {{ activeStage.stageName }}</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAddField">新增字段</el-button>
      </div>

      <div class="system-field-panel">
        <div class="system-field-panel__header">
          <span>系统字段目录</span>
          <span v-if="matchedSystemFieldGroups.length" class="system-field-panel__tip">以下字段来自当前真实业务表，可直接参考字段编码</span>
        </div>
        <el-alert
          title="模板字段仍然由你手工配置，但建议优先参考这里的系统真实字段来源，避免模板字段和实际业务数据脱节。"
          type="info"
          :closable="false"
          show-icon
        />
        <div v-loading="systemFieldLoading" class="system-field-groups">
          <template v-if="matchedSystemFieldGroups.length">
            <div v-for="group in matchedSystemFieldGroups" :key="group.stageCode" class="system-field-group">
              <div class="system-field-group__title">
                <span>{{ group.stageName }}</span>
                <el-tag size="mini" type="success">{{ group.sourceTable }}</el-tag>
              </div>
              <div class="system-field-list">
                <div v-for="field in group.fields" :key="group.stageCode + '-' + field.fieldCode" class="system-field-item">
                  <div class="system-field-item__main">
                    <span class="system-field-item__name">{{ field.fieldName }}</span>
                    <code class="system-field-item__code">{{ field.fieldCode }}</code>
                  </div>
                  <div class="system-field-item__meta">
                    <el-tag size="mini" type="info">{{ field.fieldType }}</el-tag>
                    <span>{{ field.sourceColumn }}</span>
                  </div>
                  <div class="system-field-item__desc">{{ field.description }}</div>
                </div>
              </div>
            </div>
          </template>
          <el-empty v-else description="当前环节暂未匹配到系统字段目录" :image-size="80" />
        </div>
      </div>

      <el-table v-loading="fieldLoading" :data="fieldList" border size="small" style="width: 100%">
        <el-table-column prop="fieldCode" label="字段编码" min-width="110" />
        <el-table-column prop="fieldName" label="字段名称" min-width="100" />
        <el-table-column prop="fieldType" label="字段类型" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" type="info">{{ row.fieldType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="60" align="center" />
        <el-table-column label="必填" width="60" align="center">
          <template slot-scope="{ row }">{{ row.isRequired ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="placeholder" label="提示文字" min-width="130" show-overflow-tooltip />
        <el-table-column prop="defaultValue" label="默认值" width="100" />
        <el-table-column label="操作" width="140" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" icon="el-icon-edit" @click="handleEditField(row)">编辑</el-button>
            <el-button type="text" size="mini" icon="el-icon-delete" class="danger-btn" @click="handleDeleteField(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 模版新增/编辑弹窗 -->
    <el-dialog :title="tplDialogTitle" :visible.sync="tplDialogVisible" width="520px">
      <el-form ref="tplForm" :model="tplForm" :rules="tplRules" label-width="90px" size="small">
        <el-form-item label="模版编码" prop="templateCode">
          <el-input v-model="tplForm.templateCode" placeholder="如: TPL_CHICKEN" :disabled="!!tplForm.id" />
        </el-form-item>
        <el-form-item label="模版名称" prop="templateName">
          <el-input v-model="tplForm.templateName" placeholder="请输入模版名称" />
        </el-form-item>
        <el-form-item label="模版类型" prop="templateType">
          <el-select v-model="tplForm.templateType" style="width: 100%">
            <el-option label="肉鸡" :value="1" />
            <el-option label="西红柿" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="产品类别">
          <el-input v-model="tplForm.productCategory" placeholder="如: 禽类/蔬菜" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="tplForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="tplForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="tplDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="tplSubmitting" @click="submitTemplate">确定</el-button>
      </div>
    </el-dialog>

    <!-- 环节新增/编辑弹窗 -->
    <el-dialog :title="stgDialogTitle" :visible.sync="stgDialogVisible" width="520px">
      <el-form ref="stgForm" :model="stgForm" :rules="stgRules" label-width="90px" size="small">
        <el-form-item label="环节编码" prop="stageCode">
          <el-input v-model="stgForm.stageCode" placeholder="如: INIT" />
        </el-form-item>
        <el-form-item label="环节名称" prop="stageName">
          <el-input v-model="stgForm.stageName" placeholder="请输入环节名称" />
        </el-form-item>
        <el-form-item label="环节类型" prop="stageType">
          <el-select v-model="stgForm.stageType" style="width: 100%">
            <el-option label="初始化" :value="1" />
            <el-option label="生长过程" :value="2" />
            <el-option label="加工" :value="3" />
            <el-option label="检疫" :value="4" />
            <el-option label="仓储" :value="5" />
            <el-option label="运输" :value="6" />
            <el-option label="销售" :value="7" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="stgForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="是否必填">
          <el-switch v-model="stgForm.isRequired" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="stgForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="stgDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="stgSubmitting" @click="submitStage">确定</el-button>
      </div>
    </el-dialog>

    <!-- 字段新增/编辑弹窗 -->
    <el-dialog :title="fldDialogTitle" :visible.sync="fldDialogVisible" width="520px">
      <el-form ref="fldForm" :model="fldForm" :rules="fldRules" label-width="90px" size="small">
        <el-form-item label="字段编码" prop="fieldCode">
          <el-input v-model="fldForm.fieldCode" placeholder="如: breed" />
        </el-form-item>
        <el-form-item label="字段名称" prop="fieldName">
          <el-input v-model="fldForm.fieldName" placeholder="如: 品种" />
        </el-form-item>
        <el-form-item label="字段类型" prop="fieldType">
          <el-select v-model="fldForm.fieldType" style="width: 100%">
            <el-option label="文本(text)" value="text" />
            <el-option label="数字(number)" value="number" />
            <el-option label="日期(date)" value="date" />
            <el-option label="下拉(select)" value="select" />
            <el-option label="图片(image)" value="image" />
            <el-option label="视频(video)" value="video" />
          </el-select>
        </el-form-item>
        <el-form-item label="选项配置" v-if="fldForm.fieldType === 'select'">
          <el-input v-model="fldForm.fieldOptions" type="textarea" :rows="2" placeholder='JSON格式, 如: ["选项1","选项2"]' />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="fldForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="是否必填">
          <el-switch v-model="fldForm.isRequired" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="提示文字">
          <el-input v-model="fldForm.placeholder" placeholder="输入框提示" />
        </el-form-item>
        <el-form-item label="默认值">
          <el-input v-model="fldForm.defaultValue" placeholder="字段默认值" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="fldDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="fldSubmitting" @click="submitField">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getTemplateList, addTemplate, updateTemplate, deleteTemplate, toggleTemplateStatus,
  getStageList, addStage, updateStage, deleteStage, getSystemFieldCatalog,
  getFieldList, addField, updateField, deleteField
} from '@/api/template'

const STAGE_TYPE_MAP = { 1: '初始化', 2: '生长过程', 3: '加工', 4: '检疫', 5: '仓储', 6: '运输', 7: '销售' }

export default {
  name: 'TemplateList',
  data() {
    return {
      loading: false,
      templateList: [],
      activeTemplate: null,
      // 环节
      stageLoading: false,
      stageList: [],
      activeStage: null,
      // 字段
      fieldLoading: false,
      fieldList: [],
      systemFieldLoading: false,
      systemFieldCatalog: [],
      // 模版弹窗
      tplDialogVisible: false,
      tplDialogTitle: '',
      tplSubmitting: false,
      tplForm: { id: null, templateCode: '', templateName: '', templateType: 1, productCategory: '', sort: 0, description: '' },
      tplRules: {
        templateCode: [{ required: true, message: '请输入模版编码', trigger: 'blur' }],
        templateName: [{ required: true, message: '请输入模版名称', trigger: 'blur' }],
        templateType: [{ required: true, message: '请选择模版类型', trigger: 'change' }]
      },
      // 环节弹窗
      stgDialogVisible: false,
      stgDialogTitle: '',
      stgSubmitting: false,
      stgForm: { id: null, templateId: null, stageCode: '', stageName: '', stageType: 1, sort: 0, isRequired: 1, description: '' },
      stgRules: {
        stageCode: [{ required: true, message: '请输入环节编码', trigger: 'blur' }],
        stageName: [{ required: true, message: '请输入环节名称', trigger: 'blur' }],
        stageType: [{ required: true, message: '请选择环节类型', trigger: 'change' }]
      },
      // 字段弹窗
      fldDialogVisible: false,
      fldDialogTitle: '',
      fldSubmitting: false,
      fldForm: { id: null, stageId: null, fieldCode: '', fieldName: '', fieldType: 'text', fieldOptions: '', sort: 0, isRequired: 0, placeholder: '', defaultValue: '' },
      fldRules: {
        fieldCode: [{ required: true, message: '请输入字段编码', trigger: 'blur' }],
        fieldName: [{ required: true, message: '请输入字段名称', trigger: 'blur' }],
        fieldType: [{ required: true, message: '请选择字段类型', trigger: 'change' }]
      }
    }
  },
  computed: {
    matchedSystemFieldGroups() {
      if (!this.activeStage) return []
      return this.systemFieldCatalog.filter(group => group.stageType === this.activeStage.stageType)
    }
  },
  created() {
    this.fetchTemplates()
    this.fetchSystemFieldCatalog()
  },
  methods: {
    // ========== 模版 ==========
    fetchSystemFieldCatalog() {
      this.systemFieldLoading = true
      getSystemFieldCatalog().then(res => {
        this.systemFieldCatalog = res.data || []
      }).catch(() => {
        this.systemFieldCatalog = []
      }).finally(() => { this.systemFieldLoading = false })
    },
    fetchTemplates() {
      this.loading = true
      getTemplateList().then(res => {
        const data = res.data
        if (data && data.records) {
          this.templateList = data.records
        } else if (Array.isArray(data)) {
          this.templateList = data
        } else {
          this.templateList = []
        }
      }).catch(() => { this.templateList = [] }).finally(() => { this.loading = false })
    },
    handleAddTemplate() {
      this.tplDialogTitle = '新增模版'
      this.tplForm = { id: null, templateCode: '', templateName: '', templateType: 1, productCategory: '', sort: 0, description: '' }
      this.tplDialogVisible = true
    },
    handleEditTemplate(row) {
      this.tplDialogTitle = '编辑模版'
      this.tplForm = { id: row.id, templateCode: row.templateCode, templateName: row.templateName, templateType: row.templateType, productCategory: row.productCategory || '', sort: row.sort || 0, description: row.description || '' }
      this.tplDialogVisible = true
    },
    submitTemplate() {
      this.$refs.tplForm.validate(valid => {
        if (!valid) return
        this.tplSubmitting = true
        const api = this.tplForm.id ? updateTemplate : addTemplate
        api(this.tplForm).then(() => {
          this.$message.success(this.tplForm.id ? '更新成功' : '新增成功')
          this.tplDialogVisible = false
          this.fetchTemplates()
        }).finally(() => { this.tplSubmitting = false })
      })
    },
    handleDeleteTemplate(row) {
      this.$confirm(`确定删除模版「${row.templateName}」？`, '提示', { type: 'warning' }).then(() => {
        deleteTemplate(row.id).then(() => {
          this.$message.success('删除成功')
          if (this.activeTemplate && this.activeTemplate.id === row.id) {
            this.activeTemplate = null
            this.stageList = []
            this.activeStage = null
            this.fieldList = []
          }
          this.fetchTemplates()
        })
      }).catch(() => {})
    },
    handleToggleTemplate(row) {
      const s = row.status === 1 ? 0 : 1
      toggleTemplateStatus(row.id, s).then(() => {
        this.$message.success(s ? '已启用' : '已禁用')
        this.fetchTemplates()
      })
    },
    // ========== 环节 ==========
    handleManageStages(row) {
      this.activeTemplate = row
      this.activeStage = null
      this.fieldList = []
      this.fetchStages()
    },
    fetchStages() {
      this.stageLoading = true
      getStageList(this.activeTemplate.id).then(res => {
        this.stageList = res.data || []
      }).catch(() => { this.stageList = [] }).finally(() => { this.stageLoading = false })
    },
    handleAddStage() {
      this.stgDialogTitle = '新增环节'
      this.stgForm = { id: null, templateId: this.activeTemplate.id, stageCode: '', stageName: '', stageType: 1, sort: 0, isRequired: 1, description: '' }
      this.stgDialogVisible = true
    },
    handleEditStage(row) {
      this.stgDialogTitle = '编辑环节'
      this.stgForm = { id: row.id, templateId: this.activeTemplate.id, stageCode: row.stageCode, stageName: row.stageName, stageType: row.stageType, sort: row.sort || 0, isRequired: row.isRequired, description: row.description || '' }
      this.stgDialogVisible = true
    },
    submitStage() {
      this.$refs.stgForm.validate(valid => {
        if (!valid) return
        this.stgSubmitting = true
        const api = this.stgForm.id ? updateStage : addStage
        api(this.stgForm).then(() => {
          this.$message.success(this.stgForm.id ? '更新成功' : '新增成功')
          this.stgDialogVisible = false
          this.fetchStages()
        }).finally(() => { this.stgSubmitting = false })
      })
    },
    handleDeleteStage(row) {
      this.$confirm(`确定删除环节「${row.stageName}」？`, '提示', { type: 'warning' }).then(() => {
        deleteStage(row.id).then(() => {
          this.$message.success('删除成功')
          if (this.activeStage && this.activeStage.id === row.id) {
            this.activeStage = null
            this.fieldList = []
          }
          this.fetchStages()
        })
      }).catch(() => {})
    },
    // ========== 字段 ==========
    handleManageFields(row) {
      this.activeStage = row
      this.fetchFields()
    },
    fetchFields() {
      this.fieldLoading = true
      getFieldList(this.activeStage.id).then(res => {
        this.fieldList = res.data || []
      }).catch(() => { this.fieldList = [] }).finally(() => { this.fieldLoading = false })
    },
    handleAddField() {
      this.fldDialogTitle = '新增字段'
      this.fldForm = { id: null, stageId: this.activeStage.id, fieldCode: '', fieldName: '', fieldType: 'text', fieldOptions: '', sort: 0, isRequired: 0, placeholder: '', defaultValue: '' }
      this.fldDialogVisible = true
    },
    handleEditField(row) {
      this.fldDialogTitle = '编辑字段'
      this.fldForm = { id: row.id, stageId: this.activeStage.id, fieldCode: row.fieldCode, fieldName: row.fieldName, fieldType: row.fieldType, fieldOptions: row.fieldOptions || '', sort: row.sort || 0, isRequired: row.isRequired, placeholder: row.placeholder || '', defaultValue: row.defaultValue || '' }
      this.fldDialogVisible = true
    },
    submitField() {
      this.$refs.fldForm.validate(valid => {
        if (!valid) return
        this.fldSubmitting = true
        const api = this.fldForm.id ? updateField : addField
        api(this.fldForm).then(() => {
          this.$message.success(this.fldForm.id ? '更新成功' : '新增成功')
          this.fldDialogVisible = false
          this.fetchFields()
        }).finally(() => { this.fldSubmitting = false })
      })
    },
    handleDeleteField(row) {
      this.$confirm(`确定删除字段「${row.fieldName}」？`, '提示', { type: 'warning' }).then(() => {
        deleteField(row.id).then(() => {
          this.$message.success('删除成功')
          this.fetchFields()
        })
      }).catch(() => {})
    },
    stageTypeLabel(type) { return STAGE_TYPE_MAP[type] || '未知' }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }
.table-card, .stage-card, .field-card {
  border-radius: 8px; margin-bottom: 16px; overflow-x: auto;
  .card-header {
    display: flex; align-items: center; justify-content: space-between;
    font-weight: 600; color: #1a3a2a;
  }
}
.stage-card { border-left: 3px solid #2d8a56; }
.field-card { border-left: 3px solid #409eff; }
.danger-btn { color: #f56c6c !important; }

.system-field-panel {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #e4ecf5;
  border-radius: 8px;
  background: #f8fbff;
}

.system-field-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
  color: #1a3a2a;
}

.system-field-panel__tip {
  font-size: 12px;
  font-weight: 400;
  color: #6b7280;
}

.system-field-groups {
  margin-top: 12px;
}

.system-field-group + .system-field-group {
  margin-top: 12px;
}

.system-field-group__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #234531;
}

.system-field-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.system-field-item {
  padding: 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.system-field-item__main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.system-field-item__name {
  font-weight: 600;
  color: #1f2937;
}

.system-field-item__code {
  padding: 2px 6px;
  border-radius: 4px;
  background: #eef6f0;
  color: #2d8a56;
  font-size: 12px;
}

.system-field-item__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
}

.system-field-item__desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: #4b5563;
}

::v-deep .active-row { background-color: #f0f9eb !important; }
</style>
