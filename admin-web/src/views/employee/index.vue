<template>
  <div class="dashboard-container">
    <div class="container">
      <div class="tableBar">
        <div class="filter-item">
          <label>{{ $t('catalog.employeeName') }}</label>
          <el-input
            v-model="input"
            :placeholder="String($t('catalog.employeeNameRequired'))"
            clearable
            @clear="init"
            @keyup.enter.native="initFun"
          />
        </div>
        <el-button class="normal-btn continue" @click="init(true)"
          >{{ $t('catalog.search') }}</el-button
        >
        <el-button
          type="primary"
          style="float: right"
          @click="addEmployeeHandle('add')"
        >+ {{ $t('catalog.addEmployee') }}</el-button>
      </div>
      <el-table
        :data="tableData"
        stripe
        v-if="tableData.length"
        class="tableBox management-table"
      >
        <el-table-column prop="name" :label="String($t('catalog.employeeName'))" min-width="170" align="center" header-align="center">
          <template slot-scope="scope">
            {{ scope.row.name }}
          </template>
        </el-table-column>
        <el-table-column prop="username" :label="String($t('catalog.username'))" min-width="170" align="center" header-align="center" />
        <el-table-column prop="phone" :label="String($t('catalog.phone'))" min-width="180" align="center" header-align="center" />
        <el-table-column :label="String($t('catalog.accountStatus'))" min-width="130" align="center" header-align="center">
          <template slot-scope="scope">
            <div
              class="tableColumn-status"
              :class="{ 'stop-use': String(scope.row.status) === '0' }"
            >
              {{ String(scope.row.status) === '0' ? $t('catalog.disabled') : $t('catalog.enabled') }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" :label="String($t('catalog.updated'))" min-width="200" align="center" header-align="center" />
        <el-table-column :label="String($t('catalog.actions'))" width="145" align="center">
          <template slot-scope="scope">
            <div class="management-actions">
            <el-button
              type="text"
              size="small"
              class="blueBug"
              :class="{ 'disabled-text': scope.row.username === 'admin' }"
              :disabled="scope.row.username === 'admin'"
              @click="addEmployeeHandle(scope.row.id, scope.row.username)"
            >{{ $t('catalog.edit') }}</el-button>
            <el-button
              :disabled="scope.row.username === 'admin'"
              type="text"
              size="small"
              class="non"
              :class="{
                'disabled-text': scope.row.username === 'admin',
                blueBug: scope.row.status == '0',
                delBut: scope.row.status != '0',
              }"
              @click="statusHandle(scope.row)"
            >
              {{ scope.row.status == '1' ? $t('catalog.disabled') : $t('catalog.enabled') }}
            </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <Empty v-else :is-search="isSearch" />
      <el-pagination
        class="pageList"
        :page-sizes="[10, 20, 30, 40]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="counts"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator'
import HeadLable from '@/components/HeadLable/index.vue'
import { getEmployeeList, enableOrDisableEmployee } from '@/api/employee'
import { UserModule } from '@/store/modules/user'
import InputAutoComplete from '@/components/InputAutoComplete/index.vue'
import Empty from '@/components/Empty/index.vue'

@Component({
  name: 'Employee',
  components: {
    HeadLable,
    InputAutoComplete,
    Empty,
  },
})
export default class extends Vue {
  private input: any = ''
  private counts: number = 0
  private page: number = 1
  private pageSize: number = 10
  private tableData = []
  private id = ''
  private status = ''
  private isSearch: boolean = false

  created() {
    this.init()
  }

  initProp(val) {
    this.input = val
    this.initFun()
  }

  initFun() {
    this.page = 1
    this.init()
  }

  get userName() {
    return UserModule.username
  }

  private async init(isSearch?: boolean) {
    this.isSearch = isSearch
    const params = {
      page: this.page,
      pageSize: this.pageSize,
      name: this.input ? this.input : undefined,
    }
    await getEmployeeList(params)
      .then((res: any) => {
        if (String(res.data.code) === '1') {
          this.tableData = res.data && res.data.data && res.data.data.records
          this.counts = res.data.data.total
        }
        // if (!res.data.data.records.length && type === 'search') {
        // }
      })
      .catch((err) => {
        this.$message.error(String(this.$t('catalog.requestFailed')))
      })
  }

  private addEmployeeHandle(st: string, username: string) {
    if (st === 'add') {
      this.$router.push({ path: '/employee/add' })
    } else {
      if (username === 'admin') {
        return
      }
      this.$router.push({ path: '/employee/add', query: { id: st } })
    }
  }

  private statusHandle(row: any) {
    if (row.username === 'admin') {
      return
    }
    this.id = row.id
    this.status = row.status
    this.$confirm(String(this.$t('catalog.accountConfirm')), String(this.$t('catalog.notice')), {
      confirmButtonText: String(this.$t('catalog.confirm')),
      cancelButtonText: String(this.$t('catalog.cancel')),
      type: 'warning',
    }).then(() => {
      enableOrDisableEmployee({ id: this.id, status: !this.status ? 1 : 0 })
        .then((res) => {
          if (String(res.status) === '200') {
            this.$message.success(String(this.$t('catalog.accountChanged')))
            this.init()
          }
        })
        .catch((err) => {
          this.$message.error(String(this.$t('catalog.requestFailed')))
        })
    })
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
.disabled-text {
  color: #bac0cd !important;
}
</style>

<style scoped>
.tableBar { display: flex; flex-wrap: wrap; align-items: center; gap: 12px 8px; height: auto; }
.tableBar .filter-item { display: flex; flex: 1 1 300px; max-width: 520px; min-width: 0; align-items: center; gap: 10px; }
.tableBar .filter-item label { flex: 0 0 auto; white-space: nowrap; }
.tableBar .filter-item .el-input { flex: 1 1 auto; width: auto !important; min-width: 0; }
.tableBar .normal-btn { margin-left: 0 !important; }
.tableBar > .el-button--primary { margin-left: auto; }
.tableBar .tableLab { margin-left: auto; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
</style>
