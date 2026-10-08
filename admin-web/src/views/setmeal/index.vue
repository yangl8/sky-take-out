<template>
  <div class="dashboard-container">
    <div class="container">
      <div class="tableBar">
        <div class="filter-item">
          <label>{{ $t('catalog.setmealName') }}</label>
          <el-input v-model="input"
                    :placeholder="String($t('catalog.setmealRequired'))"
                    clearable
                    @clear="init"
                    @keyup.enter.native="initFun" />
        </div>

        <div class="filter-item">
          <label>{{ $t('catalog.setmealCategory') }}</label>
          <el-select v-model="categoryId"
                     :placeholder="String($t('catalog.select'))"
                     clearable
                     @clear="init">
            <el-option v-for="item in dishCategoryList"
                       :key="item.value"
                       :label="categoryLabel(item.label)"
                       :value="item.value" />
          </el-select>
        </div>

        <div class="filter-item">
          <label>{{ $t('catalog.saleStatus') }}</label>
          <el-select v-model="dishStatus"
                     :placeholder="String($t('catalog.select'))"
                     clearable
                     @clear="init">
            <el-option v-for="item in saleStatus"
                       :key="item.value"
                       :label="item.label"
                       :value="item.value" />
          </el-select>
        </div>
        <el-button class="normal-btn continue"
                   @click="init(true)">{{ $t('catalog.search') }}</el-button>
      </div>
      <div class="table-actions-row">
          <span class="delBut non"
                @click="deleteHandle('批量')">{{ $t('catalog.batchDelete') }}</span>
          <!-- <span class="blueBug non" @click="statusHandle('1')">批量启售</span>
          <span
            style="border: none"
            class="delBut non"
            @click="statusHandle('0')"
            >批量停售</span
          > -->
          <el-button type="primary"
                     style="margin-left: 15px"
                     @click="addSetMeal('add')">+ {{ $t('catalog.newSetmeal') }}</el-button>
      </div>
      <el-table v-if="tableData.length"
                :data="tableData"
                stripe
                class="tableBox management-table"
                @selection-change="handleSelectionChange">
        <el-table-column type="selection"
                         width="25" />
        <el-table-column prop="name"
                         :label="String($t('catalog.setmealName'))" min-width="220">
          <template slot-scope="scope">{{ menuName(scope.row.name) }}</template>
        </el-table-column>
        <el-table-column prop="image"
                         :label="String($t('catalog.image'))" width="110">
          <template slot-scope="{ row }">
            <el-image style="width: 80px; height: 40px; border: none; cursor: pointer"
                      :src="row.image">
              <div slot="error"
                   class="image-slot">
                <img src="./../../assets/noImg.png"
                     style="width: auto; height: 40px; border: none">
              </div>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName"
                         :label="String($t('catalog.setmealCategory'))" min-width="180">
          <template slot-scope="scope">{{ categoryLabel(scope.row.categoryName) }}</template>
        </el-table-column>
        <el-table-column prop="price"
                         :label="String($t('catalog.setmealAmount'))" min-width="110" align="center" header-align="center">
          <template slot-scope="scope">
            <span>￥{{ ((scope.row.price ).toFixed(2) * 100) / 100 }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="String($t('catalog.saleStatus'))" min-width="125">
          <template slot-scope="scope">
            <div class="tableColumn-status"
                 :class="{ 'stop-use': String(scope.row.status) === '0' }">
              {{ String(scope.row.status) === '0' ? $t('catalog.offSale') : $t('catalog.onSale') }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime"
                         :label="String($t('catalog.updated'))" min-width="195">
          <!-- <template slot-scope="scope">
            {{ moment(scope.row.lastUpdateTime).format('YYYY-MM-DD h:m:s') }}
          </template> -->
        </el-table-column>
        <el-table-column :label="String($t('catalog.actions'))"
                         width="145"
                         align="center">
          <template slot-scope="scope">
            <div class="management-actions">
            <el-button type="text"
                       size="small"
                       class="blueBug"
                       @click="addSetMeal(scope.row)">{{ $t('catalog.edit') }}</el-button>
            <el-button type="text"
                       size="small"
                       class="delBut"
                       @click="deleteHandle('单删', scope.row.id)">{{ $t('catalog.delete') }}</el-button>
            <el-button type="text"
                       size="small"
                       class="blueBug non"
                       :class="{
                         blueBug: scope.row.status == '0',
                         delBut: scope.row.status != '0'
                       }"
                       @click="statusHandle(scope.row)">
              {{ scope.row.status == '0' ? $t('catalog.onSale') : $t('catalog.offSale') }}
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
  </div>
</template>

<script lang="ts">
import { menuName, menuDescription } from '@/i18n/menu'
import { categoryLabel } from '@/i18n/categories'
import { Component, Vue } from 'vue-property-decorator'
import HeadLable from '@/components/HeadLable/index.vue'
import {
  getSetmealPage,
  editSetmeal,
  deleteSetmeal,
  setmealStatusByStatus,
  dishCategoryList
} from '@/api/setMeal'
import InputAutoComplete from '@/components/InputAutoComplete/index.vue'
import Empty from '@/components/Empty/index.vue'
import { baseUrl } from '@/config.json'

@Component({
  name: 'package',
  components: {
    HeadLable,
    InputAutoComplete,
    Empty
  }
})
export default class extends Vue {
  menuName = menuName
  menuDescription = menuDescription
  categoryLabel = categoryLabel
  private input: any = ''
  private counts: number = 0
  private page: number = 1
  private pageSize: number = 10
  private checkList: any[] = []
  private tableData: [] = []
  private dishCategoryList = []
  private categoryId = ''
  private dishStatus = ''
  private isSearch: boolean = false
  get saleStatus() {
    return [
    {
      value: 0,
      label: String(this.$t('catalog.offSale'))
    },
    {
      value: 1,
      label: String(this.$t('catalog.onSale'))
    }
  ]
  }

  created() {
    this.init()
    this.getDishCategoryList()
  }

  initProp(val) {
    this.input = val
    this.initFun()
  }

  initFun() {
    this.page = 1
    this.init()
  }

  private async init(isSearch?) {
    this.isSearch = isSearch
    await getSetmealPage({
      page: this.page,
      pageSize: this.pageSize,
      name: this.input || undefined,
      categoryId: this.categoryId || undefined,
      status: this.dishStatus
    })
      .then(res => {
        if (res && res.data && res.data.code === 1) {
          this.tableData = res.data.data.records
          this.counts = Number(res.data.data.total)
        } else {
          this.$message.error(res.data.msg)
        }
      })
      .catch(err => {
        this.$message.error(String(this.$t('catalog.requestFailed')))
      })
  }

  // 添加更改
  private addSetMeal(st: any) {
    if (st === 'add') {
      this.$router.push({ path: '/setmeal/add' })
    } else {
      this.$router.push({ path: '/setmeal/add', query: { id: st.id } })
    }
  }

  // 删除
  private deleteHandle(type: string, id: any) {
    if (type === '批量' && id == null) {
      if (this.checkList.length === 0) {
        return this.$message.error(String(this.$t('catalog.selectDelete')))
      }
    }
    const confirmMessage = type === '批量'
      ? String(this.$t('catalog.setmealDeleteSelected'))
      : String(this.$t('catalog.setmealDelete'))
    this.$confirm(confirmMessage, String(this.$t('catalog.deleteTitle')), {
      confirmButtonText: String(this.$t('catalog.delete')),
      cancelButtonText: String(this.$t('catalog.cancel')),
      type: 'warning'
    }).then(() => {
      deleteSetmeal(type === '批量' ? this.checkList.join(',') : id)
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

  //状态更改
  private statusHandle(row: any) {
    let params: any = {}
    if (typeof row === 'string') {
      if (this.checkList.length == 0) {
        this.$message.error(String(this.$t('catalog.selectBatch')))
        return false
      }
      params.ids = this.checkList.join(',')
      params.status = row
    } else {
      params.ids = row.id
      params.status = row.status ? '0' : '1'
    }

    this.$confirm(String(this.$t('catalog.setmealConfirm')), String(this.$t('catalog.notice')), {
      confirmButtonText: String(this.$t('catalog.confirm')),
      cancelButtonText: String(this.$t('catalog.cancel')),
      type: 'warning'
    }).then(() => {
      setmealStatusByStatus(params)
        .then(res => {
          if (res.data.code === 1) {
            this.$message.success(String(this.$t('catalog.setmealChanged')))
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

  //获取套餐分类下拉数据
  private getDishCategoryList() {
    dishCategoryList({
      type: 2
    })
      .then(res => {
        if (res && res.data && res.data.code === 1) {
          this.dishCategoryList = (
            res.data &&
            res.data.data &&
            res.data.data
          ).map(item => {
            return { value: item.id, label: item.name }
          })
        }
      })
      .catch(() => {})
  }

  // 全部操作
  private handleSelectionChange(val: any) {
    let checkArr: string[] = []
    val.forEach((n: any) => {
      checkArr.push(n.id)
    })
    this.checkList = checkArr
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
<style lang="scss">
.el-table-column--selection .cell {
  padding-left: 10px;
}
</style>
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
        margin-bottom: 20px;
        .tableLab {
          float: right;
          span {
            cursor: pointer;
            display: inline-block;
            font-size: 14px;
            padding: 0 20px;
            color: $gray-2;
          }
        }
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
      //查询黑色按钮样式
      .normal-btn {
        background: #333333;
        color: white;
        margin-left: 20px;
      }
    }
  }
}
</style>

<style scoped>
.tableBar { display: flex; flex-wrap: wrap; align-items: center; gap: 12px 8px; height: auto; }
.tableBar .filter-item { display: flex; flex: 1 1 0; min-width: 250px; align-items: center; gap: 10px; }
.tableBar .filter-item label { flex: 0 0 auto; white-space: nowrap; }
.tableBar .filter-item .el-input, .tableBar .filter-item .el-select { flex: 1 1 auto; width: auto !important; min-width: 0; }
.tableBar .normal-btn { flex: 0 0 auto; margin-left: 0 !important; }
.table-actions-row { width: 100%; display: flex; flex-wrap: wrap; justify-content: flex-end; align-items: center; gap: 8px; margin: 0 0 20px; }
</style>
