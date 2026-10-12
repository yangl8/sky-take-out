<template>
  <el-dialog
    :title="String($t('nav.password'))"
    :visible.sync="dialogFormVisible"
    width="620px"
    class="pwdCon"
    @close="handlePwdClose()"
  >
    <el-form :model="form" label-width="220px" :rules="rules" ref="form">
      <el-form-item :label="String($t('password.old'))" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          :placeholder="String($t('common.enter'))"
        ></el-input>
      </el-form-item>
      <el-form-item :label="String($t('password.new'))" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          :placeholder="String($t('password.format'))"
        ></el-input>
      </el-form-item>
      <el-form-item :label="String($t('password.confirm'))" prop="affirmPassword">
        <el-input
          v-model="form.affirmPassword"
          type="password"
          :placeholder="String($t('common.enter'))"
        ></el-input>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="handlePwdClose()">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" @click="handleSave()">{{ $t('common.save') }}</el-button>
    </div>
  </el-dialog>
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator'
import { Form as ElForm, Input } from 'element-ui'
// 接口
import { editPassword } from '@/api/users'
@Component({
  name: 'Password',
})
export default class extends Vue {
  @Prop() private dialogFormVisible!: any
  private validatePwd = (rule: any, value: any, callback: Function) => {
    const reg = /^[0-9A-Za-z]{6,20}$/
    if (!value) {
      callback(new Error(String(this.$t('common.enter'))))
    } else if (!reg.test(value)) {
      callback(new Error(String(this.$t('password.format'))))
    } else {
      callback()
    }
  }
  private validatePass2 = (rule, value, callback) => {
    if (!value) {
      callback(new Error(String(this.$t('password.repeat'))))
    } else if (value !== this.form.newPassword) {
      callback(new Error(String(this.$t('password.mismatch'))))
    } else {
      callback()
    }
  }
  rules = {
    oldPassword: [{ validator: this.validatePwd, trigger: 'blur' }],
    newPassword: [{ validator: this.validatePwd, trigger: 'blur' }],
    affirmPassword: [{ validator: this.validatePass2, trigger: 'blur' }],
  }
  private form = {} as any
  private affirmPassword = ''
  @Watch('$i18n.locale')
  onLanguageChange() {
    this.$nextTick(() => {
      if (this.$refs.form) (this.$refs.form as ElForm).clearValidate()
    })
  }
  handleSave() {
    ;(this.$refs.form as ElForm).validate(async (valid: boolean) => {
      if (valid) {
        const parnt = {
          oldPassword: this.form.oldPassword,
          newPassword: this.form.newPassword,
        }
        await editPassword(parnt)
        this.$emit('handleclose')
        ;(this.$refs.form as ElForm).resetFields()
      } else {
        return false
      }
    })
  }
  handlePwdClose() {
    ;(this.$refs.form as ElForm).resetFields()
    this.$emit('handleclose')
  }
}
</script>
<style lang="scss">
.navbar {
  .pwdCon {
    .el-dialog__body {
      padding-top: 60px;
      padding: 30px 24px 0;
      word-break: normal;
      overflow-wrap: normal;
    }
    .el-input__inner {
      padding: 0 12px;
    }
    .el-form-item {
      margin-bottom: 26px;
    }
    .el-form-item__label {
      text-align: left;
      line-height: 40px;
      word-break: normal;
      overflow-wrap: normal;
    }
    .el-dialog__footer {
      padding-top: 14px;
    }
  }
}

@media only screen and (max-width: 600px) {
  .navbar {
    .pwdCon {
      .el-form-item__label {
        display: block;
        float: none;
        width: 100% !important;
        line-height: 20px;
        padding: 0 0 6px;
      }
      .el-form-item__content {
        margin-left: 0 !important;
      }
      .el-form-item {
        margin-bottom: 20px;
      }
    }
  }
}
</style>
