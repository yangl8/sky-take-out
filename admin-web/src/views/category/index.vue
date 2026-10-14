<template>
  <div class="dashboard-container">
    <div class="container">
      <div class="tableBar"
           style="width: 100%">
        <div class="filter-item">
          <label>{{ $t('catalog.categoryName') }}</label>
          <el-input v-model="name"
                    :placeholder="String($t('catalog.categoryRequired'))"
                    clearable
                    @clear="init"
                    @keyup.enter.native="init" />
        </div>

        <div class="filter-item">
          <label>{{ $t('catalog.categoryType') }}</label>
          <el-select v-model="categoryType"
                     :placeholder="String($t('catalog.select'))"
                     clearable
                     @clear="init">
            <el-option v-for="item in options"
                       :key="item.value"
                       :label="item.label"
                       :value="item.value" />
          </el-select>
        </div>

        <div class="table-actions">
          <el-button type="primary"
                     class="continue"
                     @click="addClass('class')">+ {{ $t('catalog.addDishCategory') }}</el-button>
          <el-button type="primary"
                     style="margin-left:20px"
                     @click="addClass('meal')">+ {{ $t('catalog.addSetmealCategory') }}</el-button>
        </div>

        <el-button class="normal-btn continue"
                   @click="init(true)">{{ $t('catalog.search') }}</el-button>
      </div>
      <el-table v-if="tableData.length"
                :data="tableData"
                stripe
                class="tableBox management-table">
        <el-table-column prop="name"
                         :label="String($t('catalog.categoryName'))" min-width="250">
          <template slot-scope="scope">{{ categoryLabel(scope.row.name) }}</template>
        </el-table-column>
        <el-table-column prop="type"
                         :label="String($t('catalog.categoryType'))" min-width="170" align="center" header-align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.type == '1' ? $t('catalog.dishCategory') : $t('catalog.setmealCategory') }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="sort"
                         :label="String($t('catalog.sort'))" width="160" align="center" header-align="center" />
        <el-table-column :label="String($t('catalog.status'))" width="120" align="center" header-align="center" class-name="category-status-column">
          <template slot-scope="scope">
            <div class="tableColumn-status"
                 :class="{ 'stop-use': String(scope.row.status) === '0' }">
              {{ String(scope.row.status) === '0' ? $t('catalog.disabled') : $t('catalog.enabled') }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime"
                         :label="String($t('catalog.operationTime'))" min-width="200" align="center" header-align="center" />
        <el-table-column :label="String($t('catalog.actions'))"
                         width="145"
                         align="center">
          <template slot-scope="scope">
            <div class="management-actions">
            <el-button type="text"
                       size="small"
                       class="blueBug"
                       @click="editHandle(scope.row)">{{ $t('catalog.edit') }}</el-button>
            <el-button type="text"
                       size="small"
                       class="delBut"
                       @click="deleteHandle(scope.row.id)">{{ $t('catalog.delete') }}</el-button>
            <el-button type="text"
                       size="small"
                       class="non"
                       :class="{
                         blueBug: scope.row.status == '0',
                         delBut: scope.row.status != '0'
                       }"
                       @click="statusHandle(scope.row)">
              {{ scope.row.status == '1' ? $t('catalog.disabled') : $t('catalog.enabled') }}
            </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <Empty v-else
             :is-search="isSearch" />
      <el-pagination v-if="counts > 10"
                     class="pageList"
                     :page-sizes="[10, 20, 30, 40]"
                     :page-size="pageSize"
                     layout="total, sizes, prev, pager, next, jumper"
                     :total="counts"
                     @size-change="handleSizeChange"
                     @current-change="handleCurrentChange" />
    </div>
    <el-dialog :title="String($t(classData.title))"
               :visible.sync="classData.dialogVisible"
               width="560px"
               :before-close="handleClose">
      <el-form ref="classData"
               :model="classData"
               class="demo-form-inline"
               :rules="rules"
               label-width="170px">
        <el-form-item :label="String($t('catalog.categoryName'))"
                      prop="name">
          <el-input v-model="classData.name"
                    :placeholder="String($t('catalog.categoryRequired'))"
                    maxlength="20" />
        </el-form-item>
        <el-form-item :label="String($t('catalog.sort'))"
                      prop="sort">
          <el-input v-model="classData.sort"
                    :placeholder="String($t('catalog.sortPlaceholder'))" />
        </el-form-item>
      </el-form>
      <span slot="footer"
            class="dialog-footer">
        <el-button size="medium"
                   @click="
            ;(classData.dialogVisible = false), $refs.classData.resetFields()
                   ">{{ $t('catalog.cancel') }}</el-button>
        <el-button type="primary"
                   :class="{ continue: actionType === 'add' }"
                   size="medium"
                   @click="submitForm()">{{ $t('catalog.confirm') }}</el-button>
        <el-button v-if="action != 'edit'"
                   type="primary"
                   size="medium"
                   @click="submitForm('go')">{{ $t('catalog.saveContinue') }}</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { categoryLabel } from '@/i18n/categories'
import { Component, Vue, Watch } from 'vue-property-decorator'
import HeadLable from '@/components/HeadLable/index.vue'
import {
  getCategoryPage,
  deleCategory,
  editCategory,
  addCategory,
  enableOrDisableEmployee
} from '@/api/category'
import Empty from '@/components/Empty/index.vue'

@Component({
  name: 'Category',
  components: {
    HeadLable,
    Empty
  }
})
export default class extends Vue {
  categoryLabel = categoryLabel
  @Watch('$i18n.locale')
  onLanguageChange() {
    this.$nextTick(() => {
      const form = this.$refs.classData as any
      if (form) form.clearValidate()
    })
  }

  get options() {
    return [
    {
      value: 1,
      label: String(this.$t('catalog.dishCategory'))
    },
    {
      value: 2,
      label: String(this.$t('catalog.setmealCategory'))
    }
  ]
  }
  private actionType: string = ''
  private id = ''
  private status = ''
  private categoryType: number = null
  private name: string = ''
  private action: string = ''
  private counts: number = 0
  private page: number = 1
  private pageSize: number = 10
  private tableData = []
  private type = ''
  private isSearch: boolean = false
  private classData: any = {
    title: 'catalog.addDishCategory',
    dialogVisible: false,
    categoryId: '',
    name: '',
    sort: ''
  }

  get rules() {
    return {
      name: [
        {
          required: true,
          trigger: 'blur',
          validator: (rule: any, value: string, callback: Function) => {
            // const reg = /[\u4e00-\u9fa5]/
            const reg = /^[\p{L}\p{M} '\u2019-]+$/u
            if (!value || !value.trim()) {
              callback(new Error(String(this.$t('catalog.categoryRequired'))))
            } else if (value.length < 2 || value.length > 20) {
              callback(new Error(String(this.$t('catalog.categoryLength'))))
            } else if (!reg.test(value)) {
              callback(new Error(String(this.$t('catalog.categoryCharacters'))))
            } else {
              callback()
            }
          }
        }
      ],
      sort: [
        {
          required: true,
          trigger: 'blur',
          validator: (rule: any, value: string, callback: Function) => {
            if (value || String(value) === '0') {
              const reg = /^\d+$/
              if (!reg.test(value)) {
                callback(new Error(String(this.$t('catalog.sortNumber'))))
              } else if (Number(value) > 99) {
                callback(new Error(String(this.$t('catalog.sortRange'))))
              } else {
                callback()
              }
            } else {
              callback(new Error(String(this.$t('catalog.sortRequired'))))
            }
          }
        }
      ]
    }
  }

  created() {
    this.init()
  }

  private async init(isSearch?) {
    this.isSearch = isSearch
    await getCategoryPage({
      page: this.page,
      pageSize: this.pageSize,
      name: this.name ? this.name : undefined,
      type: this.categoryType ? this.categoryType : undefined
    })
      .then(res => {
        if (String(res.data.code) === '1') {
          this.tableData =
            res && res.data && res.data.data && res.data.data.records
          this.counts = Number(res.data.data.total)
        } else {
          this.$message.error(res.data.desc)
        }
      })
      .catch(err => {
        console.log(err, 'err')
        this.$message.error(String(this.$t('catalog.requestFailed')))
      })
  }

  private addClass(st: any) {
    if (st == 'class') {
      this.classData.title = 'catalog.addDishCategory'
      this.type = '1'
    } else {
      this.classData.title = 'catalog.addSetmealCategory'
      this.type = '2'
    }
    this.action = 'add'
    this.classData.name = ''
    this.classData.sort = ''
    this.classData.dialogVisible = true
    this.actionType = 'add'
  }

  private editHandle(dat: any) {
    this.classData.title = 'catalog.editCategory'
    this.action = 'edit'
    this.classData.name = dat.name
    this.classData.sort = dat.sort
    this.classData.id = dat.id
    this.classData.dialogVisible = true
    this.actionType = 'edit'
  }

  private handleClose(st: string) {
    console.log(this.$refs.classData, 'this.$refs.classData')
    this.classData.dialogVisible = false
    this.$refs.classData.resetFields()
  }

  private statusHandle(row: any) {
    this.id = row.id
    this.status = row.status
    this.$confirm(String(this.$t('catalog.categoryConfirm')), String(this.$t('catalog.notice')), {
      confirmButtonText: String(this.$t('catalog.confirm')),
      cancelButtonText: String(this.$t('catalog.cancel')),
      type: 'warning',
      customClass: 'customClass'
    }).then(() => {
      enableOrDisableEmployee({ id: this.id, status: !this.status ? 1 : 0 })
        .then(res => {
          if (String(res.status) === '200') {
            this.$message.success(String(this.$t('catalog.categoryChanged')))
            this.init()
          }
        })
        .catch(err => {
          this.$message.error(String(this.$t('catalog.requestFailed')))
        })
    })
  }

  private deleteHandle(id: any) {
    this.$confirm(String(this.$t('catalog.categoryDelete')), String(this.$t('catalog.deleteTitle')), {
      confirmButtonText: String(this.$t('catalog.delete')),
      cancelButtonText: String(this.$t('catalog.cancel')),
      type: 'warning'
    }).then(() => {
      deleCategory(id)
        .then(res => {
          if (res.data.code === 1) {
            this.$message.success(String(this.$t('catalog.deleted')))
            this.init()
          } else {
            this.$message.error(res.data.msg)
          }
        })
        .catch(err => {
          this.$message.error(String(this.$t('catalog.requestFailed')))
        })
    })
  }

  $refs!: {
    classData: any
  }

  submitForm(st: any) {
    if (this.action === 'add') {
      this.$refs.classData.validate((value: boolean) => {
        if (value) {
          addCategory({
            name: this.classData.name,
            type: this.type,
            sort: this.classData.sort
          })
            .then(res => {
              if (res.data.code === 1) {
                this.$message.success(String(this.$t('catalog.categoryAdded')))
                this.$refs.classData.resetFields()
                if (!st) {
                  this.classData.dialogVisible = false
                }
                this.init()
              } else {
                this.$message.error(res.data.desc || res.data.msg)
              }
            })
            .catch(err => {
              this.$message.error(String(this.$t('catalog.requestFailed')))
            })
        }
      })
    } else {
      this.$refs.classData.validate((value: boolean) => {
        if (value) {
          editCategory({
            id: this.classData.id,
            name: this.classData.name,
            sort: this.classData.sort
          })
            .then(res => {
              if (res.data.code === 1) {
                this.$message.success(String(this.$t('catalog.categoryEdited')))
                this.classData.dialogVisible = false
                this.$refs.classData.resetFields()
                this.init()
              } else {
                this.$message.error(res.data.desc || res.data.msg)
              }
            })
            .catch(err => {
              this.$message.error(String(this.$t('catalog.requestFailed')))
            })
        }
      })
    }
  }

  private handleSizeChange(val: any) {
    this.pageSize = val
    this.init()
  }

  private handleCurrentChange(val: any) {
    this.page = val
    this.init()
  }
}
</script>
<style lang="scss" scoped>
.dashboard {
  &-container {
    margin: 30px;

    .container {
      background: #fff;
      position: relative;
      z-index: 1;
      padding: 30px 28px;
      border-radius: 4px;

      .tableBar {
        display: flex;
        margin-bottom: 20px;
        justify-content: space-between;
      }

      .tableBox {
        width: 100%;
        border: 1px solid $gray-5;
        border-bottom: 0;
      }

      .pageList {
        text-align: center;
        margin-top: 30px;
      }
      .normal-btn {
        background: #333333;
        color: white;
        margin-left: 20px;
      }
    }
  }
}
</style>
<style lang='scss'>
// .customClass {
//   .el-button--primary {
//     background-color: #ffc200 !important ;
//   }
// }
</style>

<style scoped>
.tableBar { display: flex; flex-wrap: wrap; align-items: center; gap: 12px 8px; height: auto; }
.tableBar .filter-item { display: flex; flex: 1 1 300px; min-width: 0; align-items: center; gap: 10px; }
.tableBar .filter-item label { flex: 0 0 auto; white-space: nowrap; }
.tableBar .filter-item .el-input, .tableBar .filter-item .el-select { flex: 1 1 auto; width: auto !important; min-width: 0; }
.tableBar .normal-btn { margin-left: 0 !important; }
.tableBar .table-actions { flex: 1 0 100%; display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; order: 2; }
.tableBar .tableLab { margin-left: auto; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.category-status-column .tableColumn-status { justify-content: center; }
</style>
