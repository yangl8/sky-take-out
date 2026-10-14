<template>
  <div class="addBrand-container">
    <div class="container">
      <el-form ref="ruleForm"
               :model="ruleForm"
               :rules="rules"
               :inline="true"
               label-width="180px"
               class="demo-ruleForm">
        <div>
          <el-form-item :label="String($t('catalog.setmealName'))"
                        prop="name">
            <el-input v-model="ruleForm.name"
                      :placeholder="String($t('catalog.setmealRequired'))"
                      maxlength="20" />
            <p v-if="menuName(ruleForm.name) !== ruleForm.name" class="translation-preview">{{ $t('catalog.translationPreview') }}: {{ menuName(ruleForm.name) }}</p>
          </el-form-item>
          <el-form-item :label="String($t('catalog.setmealCategory'))"
                        prop="idType">
            <el-select v-model="ruleForm.idType"
                       :placeholder="String($t('catalog.chooseSetmealCategory'))"
                       @change="$forceUpdate()">
              <el-option v-for="(item, index) in setMealList"
                         :key="index"
                         :label="categoryLabel(item.name)"
                         :value="item.id" />
            </el-select>
          </el-form-item>
        </div>
        <div>
          <el-form-item :label="String($t('catalog.setmealPrice'))"
                        prop="price">
            <el-input v-model="ruleForm.price"
                      :placeholder="String($t('catalog.pricePlaceholder'))" />
          </el-form-item>
        </div>
        <div>
          <el-form-item :label="String($t('catalog.setmealDishes'))"
                        required>
            <el-form-item>
              <div class="addDish">
                <span v-if="dishTable.length == 0"
                      class="addBut"
                      @click="openAddDish('new')">+ {{ $t('catalog.addDish') }}</span>
                <div v-if="dishTable.length != 0"
                     class="content">
                  <div class="addBut"
                       style="margin-bottom: 20px"
                       @click="openAddDish('change')">+ {{ $t('catalog.addDish') }}</div>
                  <div class="table">
                    <el-table :data="dishTable"
                              style="width: 100%">
                      <el-table-column prop="name"
                                       :label="String($t('catalog.name'))"
                                       width="180"
                                       align="center">
          <template slot-scope="scope">{{ menuName(scope.row.name) }}</template>
        </el-table-column>
                      <el-table-column prop="price"
                                       :label="String($t('catalog.originalPrice'))"
                                       width="180"
                                       align="center">
                        <template slot-scope="scope">
                          {{ (Number(scope.row.price).toFixed(2) * 100) / 100 }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="address"
                                       :label="String($t('catalog.quantity'))"
                                       align="center">
                        <template slot-scope="scope">
                          <el-input-number v-model="scope.row.copies"
                                           size="small"
                                           :min="1"
                                           :max="99"
                                           :label="String($t('catalog.descriptionText'))" />
                        </template>
                      </el-table-column>
                      <el-table-column prop="address"
                                       :label="String($t('catalog.actions'))"
                                       width="180px;"
                                       align="center">
                        <template slot-scope="scope">
                          <el-button type="text"
                                     size="small"
                                     class="delBut non"
                                     @click="delDishHandle(scope.$index)">{{ $t('catalog.delete') }}</el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                </div>
              </div>
            </el-form-item>
          </el-form-item>
        </div>
        <div>
          <el-form-item :label="String($t('catalog.setmealImage'))"
                        required
                        prop="image">
            <image-upload :prop-image-url="imageUrl"
                          @imageChange="imageChange">{{ $t('catalog.imageSize') }}<br>{{ $t('catalog.imageTypes') }}<br>{{ $t('catalog.imageDimensions') }}</image-upload>
          </el-form-item>
        </div>

        <div class="address">
          <el-form-item :label="String($t('catalog.setmealDescription'))">
            <el-input v-model="ruleForm.description"
                      type="textarea"
                      :rows="3"
                      maxlength="200"
                      :placeholder="String($t('catalog.descriptionHint'))" />
            <p v-if="menuDescription(ruleForm.description) !== ruleForm.description" class="translation-preview">{{ $t('catalog.translationPreview') }}: {{ menuDescription(ruleForm.description) }}</p>
          </el-form-item>
        </div>
        <div class="subBox address">
          <el-form-item>
            <el-button @click="() => $router.back()">{{ $t('catalog.cancel') }}</el-button>
            <el-button type="primary"
                       :class="{ continue: actionType === 'add' }"
                       @click="submitForm('ruleForm', false)">{{ $t('catalog.save') }}</el-button>
            <el-button v-if="actionType == 'add'"
                       type="primary"
                       @click="submitForm('ruleForm', true)">{{ $t('catalog.saveContinue') }}</el-button>
          </el-form-item>
        </div>
      </el-form>
    </div>
    <el-dialog v-if="dialogVisible"
               :title="String($t('catalog.addDish'))"
               class="addDishList"
               :visible.sync="dialogVisible"
               width="60%"
               :before-close="handleClose">
      <el-input v-model="value"
                class="seachDish"
                :placeholder="String($t('catalog.searchDish'))"
                style="width: 293px; height: 40px"
                size="small"
                clearable>
        <i slot="prefix"
           class="el-input__icon el-icon-search"
           style="cursor: pointer"
           @click="seachHandle" />
      </el-input>
      <AddDish v-if="dialogVisible"
               ref="adddish"
               :check-list="checkList"
               :seach-key="seachKey"
               :dish-list="dishList"
               @checkList="getCheckList" />
      <span slot="footer"
            class="dialog-footer">
        <el-button @click="handleClose">{{ $t('catalog.cancel') }}</el-button>
        <el-button type="primary"
                   @click="addTableList">{{ $t('catalog.add') }}</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { menuName, menuDescription } from '@/i18n/menu'
import { categoryLabel } from '@/i18n/categories'
import { Component, Vue, Watch } from 'vue-property-decorator'
import HeadLable from '@/components/HeadLable/index.vue'
import ImageUpload from '@/components/ImgUpload/index.vue'
import AddDish from './components/AddDish.vue'
import { querySetmealById, addSetmeal, editSetmeal } from '@/api/setMeal'
import { getCategoryList } from '@/api/dish'
import { baseUrl } from '@/config.json'

@Component({
  name: 'addShop',
  components: {
    HeadLable,
    AddDish,
    ImageUpload
  }
})
export default class extends Vue {
  menuName = menuName
  menuDescription = menuDescription
  categoryLabel = categoryLabel
  @Watch('$i18n.locale')
  onLanguageChange() {
    this.$nextTick(() => {
      const form = this.$refs.ruleForm as any
      if (form) form.clearValidate()
    })
  }

  private value: string = ''
  private setMealList: [] = []
  private seachKey: string = ''
  private dishList: [] = []
  private imageUrl: string = ''
  private actionType: string = ''
  private dishTable: [] = []
  private dialogVisible: boolean = false
  private checkList: any[] = []
  private ruleForm = {
    name: '',
    categoryId: '',
    price: '',
    code: '',
    image: '',
    description: '',
    dishList: [],
    status: true,
    idType: ''
  }

  get rules() {
    return {
      name: {
        required: true,
        validator: (rule: any, value: string, callback: Function) => {
          if (!value || !value.trim()) {
            callback(new Error(String(this.$t('catalog.setmealRequired'))))
          } else {
            const reg = /^[\p{L}\p{M}\p{N} '\u2019-]{2,20}$/u
            if (!reg.test(value)) {
              callback(new Error(String(this.$t('catalog.setmealLength'))))
            } else {
              callback()
            }
          }
        },
        trigger: 'blur'
      },
      idType: {
        required: true,
        message: String(this.$t('catalog.chooseSetmealCategory')),
        trigger: 'change'
      },
      image: {
        required: true,
        message: String(this.$t('catalog.imageRequired'))
      },
      price: {
        required: true,
        validator: (rules: any, value: string, callback: Function) => {
          const reg = /^([1-9]\d{0,5}|0)(\.\d{1,2})?$/
          if (!reg.test(value) || Number(value) <= 0) {
            callback(
              new Error(
                String(this.$t('catalog.priceInvalid'))
              )
            )
          } else {
            callback()
          }
        },
        trigger: 'blur'
      },
      code: { required: true, message: String(this.$t('catalog.codeRequired')), trigger: 'blur' }
    }
  }

  created() {
    this.getDishTypeList()
    this.actionType = this.$route.query.id ? 'edit' : 'add'
    if (this.actionType == 'edit') {
      this.init()
    }
  }

  private async init() {
    querySetmealById(this.$route.query.id).then(res => {
      if (res && res.data && res.data.code === 1) {
        this.ruleForm = res.data.data
        this.ruleForm.status = res.data.data.status == '1'
        ;(this.ruleForm as any).price = res.data.data.price
        // this.imageUrl = `http://172.17.2.120:8080/common/download?name=${res.data.data.image}`
        this.imageUrl = res.data.data.image
        this.checkList = res.data.data.setmealDishes
        this.dishTable = res.data.data.setmealDishes.reverse()
        this.ruleForm.idType = res.data.data.categoryId
      } else {
        this.$message.error(res.data.msg)
      }
    })
  }
  private seachHandle() {
    this.seachKey = this.value
  }
  private getDishTypeList() {
    getCategoryList({ type: 2, page: 1, pageSize: 1000 }).then(res => {
      if (res && res.data && res.data.code === 1) {
        this.setMealList = res.data.data.map((obj: any) => ({
          ...obj,
          idType: obj.id
        }))
      } else {
        this.$message.error(res.data.msg)
      }
    })
  }

  // private getDishList (id:number) {
  //   getDishListType({id}).then(res => {
  //     if (res.data.code == 200) {
  //       const { data } = res.data
  //       this.dishList = data
  //     } else {
  //       this.$message.error(res.data.desc)
  //     }
  //   })
  // }

  delDishHandle(index: any) {
    this.dishTable.splice(index, 1)
    this.checkList = this.dishTable
    // this.checkList.splice(index, 1)
  }

  private getCheckList(value: any) {
    this.checkList = [...value].reverse()
  }

  openAddDish(st: string) {
    this.seachKey = ''
    this.dialogVisible = true
  }
  handleClose(done: any) {
    // this.$refs.adddish.close()
    this.dialogVisible = false
    this.checkList = JSON.parse(JSON.stringify(this.dishTable))
    // this.dialogVisible = false
  }

  public addTableList() {
    this.dishTable = JSON.parse(JSON.stringify(this.checkList))
    this.dishTable.forEach((n: any) => {
      n.copies = 1
    })
    this.dialogVisible = false
  }

  public submitForm(formName: any, st: any) {
    ;(this.$refs[formName] as any).validate((valid: any) => {
      if (valid) {
        if (this.dishTable.length === 0) {
          return this.$message.error(String(this.$t('catalog.dishesRequired')))
        }
        if (!this.ruleForm.image) return this.$message.error(String(this.$t('catalog.setmealImageRequired')))
        let prams = { ...this.ruleForm } as any
        prams.setmealDishes = this.dishTable.map((obj: any) => ({
          copies: obj.copies,
          dishId: obj.dishId,
          name: obj.name,
          price: obj.price
        }))
        ;(prams as any).status =
          this.actionType === 'add' ? 0 : this.ruleForm.status ? 1 : 0
        prams.categoryId = this.ruleForm.idType
        // delete prams.dishList
        if (this.actionType == 'add') {
          delete prams.id
          addSetmeal(prams)
            .then(res => {
              if (res && res.data && res.data.code === 1) {
                this.$message.success(String(this.$t('catalog.setmealAdded')))
                if (!st) {
                  this.$router.push({ path: '/setmeal' })
                } else {
                  ;(this as any).$refs.ruleForm.resetFields()
                  this.dishList = []
                  this.dishTable = []
                  this.ruleForm = {
                    name: '',
                    categoryId: '',
                    price: '',
                    code: '',
                    image: '',
                    description: '',
                    dishList: [],
                    status: true,
                    id: '',
                    idType: ''
                  } as any
                  this.imageUrl = ''
                }
              } else {
                this.$message.error(res.data.msg)
              }
            })
            .catch(err => {
              this.$message.error(String(this.$t('catalog.requestFailed')))
            })
        } else {
          delete prams.updateTime
          editSetmeal(prams)
            .then(res => {
              if (res.data.code === 1) {
                this.$message.success(String(this.$t('catalog.setmealEdited')))
                this.$router.push({ path: '/setmeal' })
              } else {
                // this.$message.error(res.data.desc || res.data.message)
              }
            })
            .catch(err => {
              this.$message.error(String(this.$t('catalog.requestFailed')))
            })
        }
      } else {
        // console.log('error submit!!')
        return false
      }
    })
  }

  imageChange(value: any) {
    this.ruleForm.image = value
  }
}
</script>
<style>
.avatar-uploader .el-icon-plus:after {
  position: absolute;
  display: inline-block;
  content: ' ' !important;
  left: calc(50% - 20px);
  top: calc(50% - 40px);
  width: 40px;
  height: 40px;
  background: url('./../../assets/icons/icon_upload@2x.png') center center
    no-repeat;
  background-size: 20px;
}
</style>
<style lang="scss">
// .el-form-item__error {
//   top: 90%;
// }
.addBrand-container {
  .avatar-uploader .el-upload {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
  }

  .avatar-uploader .el-upload:hover {
    border-color: #ffc200;
  }

  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 200px;
    height: 160px;
    line-height: 160px;
    text-align: center;
  }

  .avatar {
    width: 200px;
    height: 160px;
    display: block;
  }

  // .el-form--inline .el-form-item__content {
  //   width: 293px;
  // }

  .el-input {
    width: 293px;
  }

  .address {
    .el-form-item__content {
      width: 777px !important;
    }
  }
  .el-input__prefix {
    top: 2px;
  }

  .addDish {
    .el-input {
      width: 130px;
    }

    .el-input-number__increase {
      border-left: solid 1px #fbe396;
      background: #fffbf0;
    }

    .el-input-number__decrease {
      border-right: solid 1px #fbe396;
      background: #fffbf0;
    }

    input {
      border: 1px solid #fbe396;
    }

    .table {
      border: solid 1px #ebeef5;
      border-radius: 3px;

      th {
        padding: 5px 0;
      }

      td {
        padding: 7px 0;
      }
    }
  }

  .addDishList {
    .seachDish {
      position: absolute;
      top: 12px;
      right: 20px;
    }

    .el-dialog__footer {
      padding-top: 27px;
    }

    .el-dialog__body {
      padding: 0;
      border-bottom: solid 1px #efefef;
    }
    .seachDish {
      .el-input__inner {
        height: 40px;
        line-height: 40px;
      }
    }
  }
}
</style>
<style lang="scss" scoped>
.addBrand {
  &-container {
    margin: 30px;

    .container {
      position: relative;
      z-index: 1;
      background: #fff;
      padding: 30px;
      border-radius: 4px;
      min-height: 500px;

      .subBox {
        padding-top: 30px;
        text-align: center;
        border-top: solid 1px $gray-5;
      }
      .el-input {
        width: 350px;
      }
      .addDish {
        width: 777px;

        .addBut {
          background: #ffc200;
          display: inline-block;
          padding: 0px 20px;
          border-radius: 3px;
          line-height: 40px;
          cursor: pointer;
          border-radius: 4px;
          color: #333333;
          font-weight: 500;
        }

        .content {
          background: #fafafb;
          padding: 20px;
          border: solid 1px #d8dde3;
          border-radius: 3px;
        }
      }
    }
  }
}
</style>

<style scoped>
.translation-preview { max-width: 480px; margin: 6px 0 0; color: #666; font-size: 12px; line-height: 1.5; overflow-wrap: break-word; }
</style>
