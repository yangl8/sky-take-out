<template>
  <div class="addBrand-container">
    <HeadLable :title="String($t(title))"
               :goback="true" />
    <div class="container">
      <el-form ref="ruleForm"
               :model="ruleForm"
               :rules="rules"
               :inline="false"
               label-width="180px"
               class="demo-ruleForm">
        <!--            </el-select>-->
        <!--          </el-form-item>-->
        <el-form-item :label="String($t('catalog.username'))"
                      prop="username">
          <el-input v-model="ruleForm.username"
                    :placeholder="String($t('catalog.usernameRequired'))"
                    maxlength="20" />
        </el-form-item>
        <el-form-item :label="String($t('catalog.employeeName'))"
                      prop="name">
          <el-input v-model="ruleForm.name"
                    :placeholder="String($t('catalog.employeeNameRequired'))"
                    maxlength="12" />
        </el-form-item>






















        <el-form-item :label="String($t('catalog.phone'))"
                      prop="phone">
          <el-input v-model="ruleForm.phone"
                    :placeholder="String($t('catalog.phoneRequired'))"
                    maxlength="11" />
        </el-form-item>
        <el-form-item :label="String($t('catalog.sex'))"
                      prop="sex">
          <el-radio-group v-model="ruleForm.sex">
            <el-radio label="男">{{ $t('catalog.male') }}</el-radio>
            <el-radio label="女">{{ $t('catalog.female') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="String($t('catalog.idNumber'))"
                      prop="idNumber"
                      class="idNumber">
          <el-input v-model="ruleForm.idNumber"
                    :placeholder="String($t('catalog.idRequired'))"
                    maxlength="20" />
        </el-form-item>
        <div class="subBox address">
          <!-- <el-form-item> -->
          <el-button @click="() => $router.push('/employee')">{{ $t('catalog.cancel') }}</el-button>
          <el-button type="primary"
                     :class="{ continue: actionType === 'add' }"
                     @click="submitForm('ruleForm', false)">{{ $t('catalog.save') }}</el-button>
          <el-button v-if="actionType == 'add'"
                     type="primary"
                     @click="submitForm('ruleForm', true)">{{ $t('catalog.saveContinue') }}</el-button>
          <!-- </el-form-item> -->
        </div>
      </el-form>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator'
import HeadLable from '@/components/HeadLable/index.vue'
import { queryEmployeeById, addEmployee, editEmployee } from '@/api/employee'

@Component({
  name: 'addShop',
  components: {
    HeadLable
  }
})
export default class extends Vue {
  @Watch('$i18n.locale')
  onLanguageChange() {
    this.$nextTick(() => {
      const form = this.$refs.ruleForm as any
      if (form) form.clearValidate()
    })
  }

  private title = 'catalog.addEmployee'
  private actionType = ''
  private ruleForm = {
    name: '',
    phone: '',
    // 'password': '',
    // 'rePassword': '',
    sex: '男',
    idNumber: '',
    username: ''
  }

  // private validateRepassword (rule:any, value:any, callback:any) {
  //   if (value === '') {
  //   } else if (value !== this.ruleForm.password) {
  //   } else {
  //     callback()
  //   }
  // }

  private isCellPhone(val: any) {
    if (!/^1(3|4|5|6|7|8)\d{9}$/.test(val)) {
      return false
    } else {
      return true
    }
  }

  private checkphone(rule: any, value: any, callback: any) {
    // let phoneReg = /(^1[3|4|5|6|7|8|9]\d{9}$)|(^09\d{8}$)/;
    if (value == '') {
      callback(new Error(String(this.$t('catalog.phoneRequired'))))
    } else if (!this.isCellPhone(value)) {
      callback(new Error(String(this.$t('catalog.phoneInvalid'))))
    } else {
      callback()
    }
  }

  private validID(rule: any, value: any, callback: any) {
    let reg = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/
    if (value == '') {
      callback(new Error(String(this.$t('catalog.idRequired'))))
    } else if (reg.test(value)) {
      callback()
    } else {
      callback(new Error(String(this.$t('catalog.idInvalid'))))
    }
  }

  get rules() {
    return {
      name: [
        {
          required: true,
          // 'message': String(this.$t('catalog.employeeNameRequired')),
          validator: (rule: any, value: string, callback: Function) => {
            if (!value) {
              callback(new Error(String(this.$t('catalog.employeeNameRequired'))))
            } else {
              // const reg = /^[\u4e00-\u9fa5_a-zA-Z]{1,12}$/
              // if (!reg.test(value)) {
              // } else {
              //   callback()
              // }
              callback()
            }
          },
          trigger: 'blur'
        }
      ],
      username: [
        {
          required: true,
          // message: String(this.$t('catalog.usernameRequired')),
          validator: (rule: any, value: string, callback: Function) => {
            if (!value) {
              callback(new Error(String(this.$t('catalog.usernameRequired'))))
            } else {
              const reg = /^([a-z]|[0-9]){3,20}$/
              if (!reg.test(value)) {
                callback(new Error(String(this.$t('catalog.usernameInvalid'))))
              } else {
                callback()
              }
            }
          },
          trigger: 'blur'
        }
      ],
      phone: [{ required: true, validator: this.checkphone, trigger: 'blur' }],
      idNumber: [{ required: true, validator: this.validID, trigger: 'blur' }]
    }
  }

  created() {
    this.actionType = this.$route.query.id ? 'edit' : 'add'
    if (this.$route.query.id) {
      this.title = 'catalog.editEmployee'
      this.init()
    }
  }

  private async init() {
    const id = this.$route.query.id
    queryEmployeeById(id).then((res: any) => {
      // String(res.status) === '200'
      if (res.data.code === 1) {
        this.ruleForm = res.data.data
        this.ruleForm.sex = res.data.data.sex === '0' ? '女' : '男'
        // this.ruleForm.password = ''
      } else {
        this.$message.error(res.data.msg)
      }
      // if (res.data.code == 200) {
      //   const { data } = res.data
      //   this.ruleForm = data
      //   this.ruleForm.password = ''
      //   // this.ruleForm.rePassword = '' //JSON.parse(JSON.stringify(data.password));
      // } else {
      //   this.$message.error(res.data.desc)
      // }
    })
  }

  private submitForm(formName: any, st: any) {
    ;(this.$refs[formName] as any).validate((valid: any) => {
      if (valid) {
        if (this.actionType === 'add') {
          const params = {
            ...this.ruleForm,
            sex: this.ruleForm.sex === '女' ? '0' : '1'
          }
          addEmployee(params)
            .then((res: any) => {
              if (res.data.code === 1) {
                this.$message.success(String(this.$t('catalog.employeeAdded')))
                if (!st) {
                  this.$router.push({ path: '/employee' })
                } else {
                  this.ruleForm = {
                    username: '',
                    name: '',
                    phone: '',
                    // 'password': '',
                    // 'rePassword': '',/
                    sex: '男',
                    idNumber: ''
                  }
                }
              } else {
                this.$message.error(res.data.msg)
              }
            })
            .catch(() => {
              // this.$message.error(String(this.$t('catalog.requestFailed')))
            })
        } else {
          const params = {
            ...this.ruleForm,
            sex: this.ruleForm.sex === '女' ? '0' : '1'
          }
          editEmployee(params)
            .then((res: any) => {
              if (res.data.code === 1) {
                this.$message.success(String(this.$t('catalog.employeeEdited')))
                this.$router.push({ path: '/employee' })
              } else {
                this.$message.error(res.data.msg)
              }
            })
            .catch(() => {
              // this.$message.error(String(this.$t('catalog.requestFailed')))
            })
        }
      } else {
        return false
      }
    })
  }
}
</script>

<style lang="scss" scoped>
.addBrand {
  &-container {
    margin: 30px;
    margin-top: 0px;
    .HeadLable {
      background-color: transparent;
      margin-bottom: 0px;
      padding-left: 0px;
    }
    .container {
      position: relative;
      z-index: 1;
      background: #fff;
      padding: 30px;
      border-radius: 4px;
      // min-height: 500px;
      .subBox {
        padding-top: 30px;
        text-align: center;
        border-top: solid 1px $gray-5;
      }
    }
    .idNumber {
      margin-bottom: 39px;
      white-space: nowrap;

      ::v-deep .el-form-item__label {
        white-space: nowrap;
        width: 250px !important;
      }

      ::v-deep .el-form-item__content {
        margin-left: 250px !important;
      }
    }

    .el-form-item {
      margin-bottom: 29px;
    }
    .el-input {
      width: 293px;
    }
  }
}
</style>
