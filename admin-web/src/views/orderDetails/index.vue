<template>
  <div class="dashboard-container">
    <TabChange
      :order-statics="orderStatics"
      :default-activity="defaultActivity"
      @tabChange="change"
    />
    <div class="container" :class="{ hContainer: tableData.length }">
      <!-- 搜索项 -->
      <div class="tableBar">
        <div class="filter-item">
          <label>{{ $t('orders.orderNumber') }}：</label>
          <el-input
            v-model="input"
            :placeholder="String($t('orders.enterOrderNumber'))"
            clearable
            @clear="init(orderStatus)"
            @keyup.enter.native="initFun(orderStatus)"
          />
        </div>
        <div class="filter-item">
          <label>{{ $t('orders.phone') }}：</label>
          <el-input
            v-model="phone"
            :placeholder="String($t('orders.enterPhone'))"
            clearable
            @clear="init(orderStatus)"
            @keyup.enter.native="initFun(orderStatus)"
          />
        </div>
        <div class="filter-item filter-date">
          <label>{{ $t('orders.orderTime') }}：</label>
          <el-date-picker
            v-model="valueTime"
            clearable
            value-format="yyyy-MM-dd HH:mm:ss"
            :range-separator="String($t('statistics.to'))"
            :default-time="['00:00:00', '23:59:59']"
            type="daterange"
            :start-placeholder="String($t('orders.startDate'))"
            :end-placeholder="String($t('orders.endDate'))"
            @clear="init(orderStatus)"
          />
        </div>
        <el-button class="normal-btn continue" @click="init(orderStatus, true)">
          {{ $t('common.search') }}
        </el-button>
      </div>
      <el-table
        v-loading="listLoading"
        :data="tableData"
        stripe
        class="tableBox order-table"
      >
        <el-table-column key="number" prop="number" :label="String($t('orders.orderNumber'))" min-width="220" align="center" header-align="center" class-name="order-number-column">
          <template slot-scope="{ row }">
            <span class="order-number-text">{{ row.number }}</span>
            <el-tag v-if="isScheduledDelivery(row)" size="mini" class="scheduled-tag" :title="String($t('orders.scheduledDelivery'))" :aria-label="String($t('orders.scheduledDelivery'))">{{ String($t('orders.scheduledDelivery')).slice(0, 1) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          v-if="[0].includes(orderStatus)"
          key="status"
          prop="status"
          :label="String($t('orders.orderStatus'))"
          min-width="145"
          align="center"
          header-align="center"
        >
          <template slot-scope="{ row }">
            <span>{{ getOrderType(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column key="consignee" prop="consignee" :label="String($t('orders.customerName'))" min-width="130" align="center" header-align="center" />
        <el-table-column
          v-if="[0, 1].includes(orderStatus)"
          key="orderTime"
          prop="orderTime"
          :label="String($t('orders.orderTime'))"
          min-width="180"
          align="center"
          header-align="center"
        />
        <el-table-column
          v-if="[2, 3, 4].includes(orderStatus)"
          key="estimatedDeliveryTime"
          prop="estimatedDeliveryTime"
          :label="String($t('orders.estimatedDeliveryTime'))"
          min-width="190"
          align="center"
          header-align="center"
        />
        <el-table-column v-if="[5].includes(orderStatus)" key="deliveryTime" prop="deliveryTime" :label="String($t('orders.deliveryTime'))" min-width="160" align="center" header-align="center" />
        <el-table-column v-if="[6].includes(orderStatus)" key="cancelTime" prop="cancelTime" :label="String($t('orders.cancelTime'))" min-width="180" align="center" header-align="center" />
        <el-table-column
          key="amount"
          prop="amount"
          :label="String($t('orders.amountReceived'))"
          align="center"
          min-width="180"
        >
          <template slot-scope="{ row }">
            <span>￥{{ formatMoney(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="[6].includes(orderStatus)"
          key="cancelledState"
          :label="String($t('orders.orderStatus'))"
          width="150"
          align="center"
          header-align="center"
        >
          <template slot-scope="{ row }">
            {{ Number(row.payStatus) === 2 ? $t('orders.refunded') : $t('orders.cancelled') }}
          </template>
        </el-table-column>
        <el-table-column
          prop="btn"
          :label="String($t('catalog.actions'))"
          align="center"
          class-name="order-actions-column"
          min-width="210"
        >
          <template slot-scope="{ row }">
            <div class="order-actions">
              <el-button
                v-if="row.status === 2"
                type="text"
                class="blueBug"
                @click="isTableOperateBtn = true; orderAccept(row)"
              >
                {{ $t('orders.accept') }}
              </el-button>
              <el-button
                v-if="row.status === 3"
                type="text"
                class="blueBug"
                @click="cancelOrDeliveryOrComplete(3, row.id)"
              >
                {{ $t('orders.deliver') }}
              </el-button>
              <el-button
                v-if="row.status === 4"
                type="text"
                class="blueBug"
                @click="cancelOrDeliveryOrComplete(4, row.id)"
              >
                {{ $t('orders.complete') }}
              </el-button>
              <el-button
                v-if="row.status === 2"
                type="text"
                class="delBut"
                @click="isTableOperateBtn = true; orderReject(row)"
              >
                {{ $t('orders.reject') }}
              </el-button>
              <el-button
                v-if="[1, 3, 4].includes(row.status)"
                type="text"
                class="delBut"
                @click="cancelOrder(row)"
              >
                {{ $t('common.cancel') }}
              </el-button>
              <el-button
                type="text"
                class="blueBug non"
                @click="goDetail(row.id, row.status, row)"
              >
                {{ $t('orders.view') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <Empty v-if="!listLoading && !tableData.length" :is-search="isSearch" />
      <el-pagination
        v-if="counts > 10"
        class="pageList"
        :page-sizes="[10, 20, 30, 40]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="counts"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 查看弹框部分 -->
    <el-dialog
      :title="String($t('orders.orderInformation'))"
      :visible.sync="dialogVisible"
      width="72%"
      :before-close="handleClose"
      class="order-dialog"
    >
      <el-scrollbar v-loading="detailLoading" style="height: 100%">
        <div class="order-top">
          <div class="order-top-main">
            <div class="order-identifier">
              <label style="font-size: 16px">{{ $t('orders.orderNumber') }}：</label>
              <div class="order-num">
                {{ diaForm.number || '—' }}
              </div>
            </div>
            <div
              class="order-status"
              :class="{ status3: [3, 4].includes(dialogOrderStatus) }"
            >
              {{
                statusLabel(dialogOrderStatus)
              }}
            </div>
            <p class="order-date"><label>{{ $t('orders.orderTime') }}：</label>{{ diaForm.orderTime || '—' }}</p>
          </div>
        </div>

        <div class="order-middle">
          <div class="user-info">
            <div class="user-info-box">
              <div class="delivery-summary" :class="{ scheduled: isScheduledDelivery(diaForm) }">
                <strong>{{ deliveryTypeLabel(diaForm.deliveryStatus) }}</strong>
                <span>{{ deliveryTimeLabel(diaForm.deliveryStatus, dialogOrderStatus) }}：{{ (dialogOrderStatus === 5 && Number(diaForm.deliveryStatus) === 1 ? diaForm.deliveryTime : diaForm.estimatedDeliveryTime) || '—' }}</span>
              </div>
              <div class="user-name">
                <label>{{ $t('orders.customerName') }}：</label>
                <span>{{ diaForm.consignee }}</span>
              </div>
              <div class="user-phone">
                <label>{{ $t('orders.phone') }}：</label>
                <span>{{ diaForm.phone || '—' }}</span>
              </div>
              <div class="user-address">
                <label>{{ $t('orders.address') }}：</label>
                <span :title="diaForm.address ? orderAddressLabel(diaForm.address, $i18n.locale) : '—'">{{ diaForm.address ? orderAddressLabel(diaForm.address, $i18n.locale) : '—' }}</span>
              </div>
            </div>
            <div class="fulfillment-info">
              <div class="user-remark">
                <div>{{ $t('orders.remark') }}</div>
                <span>{{ diaForm.remark || $t('orders.noRemark') }}</span>
              </div>
              <div class="user-remark">
                <div>{{ $t('orders.tablewareNumber') }}</div>
                <span>{{ tablewareLabel(diaForm.tablewareStatus, diaForm.tablewareNumber) }}</span>
              </div>
              <div v-if="dialogOrderStatus === 6" class="user-remark orderCancel">
                <div>{{ $t('orders.cancelReason') }}</div>
                <span>{{ orderReasonLabel(diaForm.cancelReason || diaForm.rejectionReason) || '—' }}</span>
              </div>
            </div>
          </div>

          <div class="dish-info">
            <div class="dish-label">{{ $t('orders.dishes') }}</div>
            <div class="dish-list">
              <div
                v-for="(item, index) in normalizedOrderDetails"
                :key="index"
                class="dish-item"
              >
                <span class="dish-name">{{ menuName(item.name) }}</span>
                  <span class="dish-num">x{{ item.number }}</span>
                <span class="dish-price">
                  ￥{{ item.amount == null ? '—' : formatMoney(item.amount) }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="order-bottom">
          <div class="amount-info">
            <div class="amount-label">{{ $t('orders.fees') }}</div>
            <div class="amount-list">
              <div class="dish-amount">
                <span class="amount-name">{{ $t('orders.dishSubtotal') }}：</span>
                <span class="amount-price"
                  >￥{{
                    formatMoney(dishSubtotal)
                  }}</span
                >
              </div>
              <div class="send-amount">
                <span class="amount-name">{{ $t('orders.deliveryFee') }}：</span>
                <span class="amount-price">￥{{ formatMoney(diaForm.deliveryFee) }}</span>
              </div>
              <div class="package-amount">
                <span class="amount-name">{{ $t('orders.packingFee') }}：</span>
                <span class="amount-price"
                  >￥{{
                    formatMoney(diaForm.packAmount)
                  }}</span
                >
              </div>
              <div class="all-amount">
                <span class="amount-name">{{ $t('orders.total') }}：</span>
                <span class="amount-price"
                  >￥{{
                    formatMoney(diaForm.amount)
                  }}</span
                >
              </div>
              <div class="pay-type" v-if="dialogOrderStatus !== 6 || Number(diaForm.payStatus) === 2">
                <span class="pay-name">{{ $t('orders.paymentMethod') }}：</span>
                <span class="pay-value">{{
                  paymentMethodLabel(diaForm.payMethod)
                }}</span>
              </div>
              <div class="pay-time" v-if="dialogOrderStatus !== 6 || Number(diaForm.payStatus) === 2">
                <span class="pay-name">{{ $t('orders.paymentTime') }}：</span>
                <span class="pay-value">{{ diaForm.checkoutTime }}</span>
              </div>
              <div v-if="dialogOrderStatus === 6 && Number(diaForm.payStatus) === 2" class="refund-status">
                <span class="amount-name">{{ $t('orders.refundStatus') }}：</span>
                <span class="amount-price">{{ $t('orders.refundCompleted') }}</span>
              </div>
              <div v-if="dialogOrderStatus === 6 && Number(diaForm.payStatus) === 2" class="refund-amount">
                <span class="amount-name">{{ $t('orders.refundAmount') }}：</span>
                <span class="amount-price">￥{{ formatMoney(diaForm.amount) }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-scrollbar>
      <span slot="footer" class="dialog-footer">
        <el-button v-if="dialogOrderStatus === 6" @click="dialogVisible = false">{{ $t('catalog.back') }}</el-button>
        <el-checkbox
          v-if="dialogOrderStatus === 2 && orderStatus === 2"
          v-model="isAutoNext"
          >{{ $t('orders.autoNext') }}</el-checkbox
        >
        <el-button
          v-if="dialogOrderStatus === 2"
          @click="isTableOperateBtn = false; orderReject(row)"
          >{{ $t('orders.reject') }}</el-button
        >
        <el-button
          v-if="dialogOrderStatus === 2"
          type="primary"
          @click="isTableOperateBtn = false; orderAccept(row)"
          >{{ $t('orders.accept') }}</el-button
        >

        <el-button
          v-if="[1, 3, 4, 5].includes(dialogOrderStatus)"
          @click="dialogVisible = false"
          >{{ $t('catalog.back') }}</el-button
        >
        <el-button
          v-if="dialogOrderStatus === 3"
          type="primary"
          @click="cancelOrDeliveryOrComplete(3, row.id)"
          >{{ $t('orders.deliver') }}</el-button
        >
        <el-button
          v-if="dialogOrderStatus === 4"
          type="primary"
          @click="cancelOrDeliveryOrComplete(4, row.id)"
          >{{ $t('orders.complete') }}</el-button
        >
        <el-button
          v-if="[1].includes(dialogOrderStatus)"
          type="primary"
          @click="cancelOrder(row)"
          >{{ $t('orders.cancelOrder') }}</el-button
        >
      </span>
    </el-dialog>
    <!-- 拒单，取消弹窗 -->
    <el-dialog
      :title="String($t(cancelDialogTitle === '取消' ? 'orders.confirmCancel' : 'orders.confirmReject'))"
      :visible.sync="cancelDialogVisible"
      width="42%"
      :before-close="() => ((cancelDialogVisible = false), (cancelReason = ''))"
      class="cancelDialog"
    >
      <el-form label-width="90px">
        <el-form-item :label="String($t(cancelDialogTitle === '取消' ? 'orders.cancelReason' : 'orders.rejectionReason')) + '：'">
          <el-select
            v-model="cancelReason"
            :placeholder="String($t('orders.selectReason'))"
          >
            <el-option
              v-for="(item, index) in cancelDialogTitle === '取消'
                ? cancelrReasonList
                : cancelOrderReasonList"
              :key="index"
              :label="orderReasonLabel(item.label)"
              :value="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="cancelReason === '自定义原因'" :label="String($t('orders.reason')) + '：'">
          <el-input
            v-model.trim="remark"
            type="textarea"
            :placeholder="String($t('orders.enterCustomReason'))"
            maxlength="20"
          />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click=";(cancelDialogVisible = false), (cancelReason = '')"
          >{{ $t('common.cancel') }}</el-button
        >
        <el-button type="primary" @click="confirmCancel">{{ $t('common.confirm') }}</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { menuName, menuDescription, orderMenuSummary } from '@/i18n/menu'
import { orderReasonLabel } from '@/i18n/orders'
import { orderAddressLabel } from '@/i18n/orderAddress'
import { isScheduledDelivery as isScheduledDeliveryStatus, resolveTablewareDisplay } from '@/utils/orderFulfillment'
import { Component, Vue } from 'vue-property-decorator'
import HeadLable from '@/components/HeadLable/index.vue'
import InputAutoComplete from '@/components/InputAutoComplete/index.vue'
import TabChange from './tabChange.vue'
import Empty from '@/components/Empty/index.vue'
import {
  getOrderDetailPage,
  queryOrderDetailById,
  completeOrder,
  deliveryOrder,
  orderCancel,
  orderReject,
  orderAccept,
  getOrderListBy,
} from '@/api/order'

@Component({
  components: {
    HeadLable,
    InputAutoComplete,
    TabChange,
    Empty,
  },
})
export default class extends Vue {
  orderMenuSummary = orderMenuSummary
  menuName = menuName
  menuDescription = menuDescription
  orderReasonLabel = orderReasonLabel
  orderAddressLabel = orderAddressLabel
  private defaultActivity: any = 0
  private orderStatics = {}
  private row = {}
  private isAutoNext = true
  private isTableOperateBtn = true
  private currentPageIndex = 0 //记录查看详情数据的index
  private orderId = '' //订单号
  private input = '' //搜索条件的订单号
  private phone = '' //搜索条件的手机号
  private valueTime = []
  private dialogVisible = false //详情弹窗
  private cancelDialogVisible = false //取消，拒单弹窗
  private cancelDialogTitle = '' //取消，拒绝弹窗标题
  private cancelReason = ''
  private remark = '' //自定义原因
  private counts: number = 0
  private page: number = 1
  private pageSize: number = 10
  private tableData = []
  private diaForm: any = {}
  private detailLoading = false
  private listLoading = false
  private autoNextAfterAction = false
  private autoNextExcludeOrderId = ''
  private detailRequestSequence = 0
  private isSearch: boolean = false
  private orderStatus = 0 //列表字段展示所需订单状态,用于分页请求数据
  private dialogOrderStatus = 0 //弹窗所需订单状态，用于详情展示字段
  private cancelOrderReasonList = [
    {
      value: 1,
      label: '订单量较多，暂时无法接单',
    },
    {
      value: 2,
      label: '菜品已销售完，暂时无法接单',
    },
    {
      value: 3,
      label: '餐厅已打烊，暂时无法接单',
    },
    {
      value: 0,
      label: '自定义原因',
    },
  ]

  private cancelrReasonList = [
    {
      value: 1,
      label: '订单量较多，暂时无法接单',
    },
    {
      value: 2,
      label: '菜品已销售完，暂时无法接单',
    },
    {
      value: 3,
      label: '骑手不足无法配送',
    },
    {
      value: 4,
      label: '客户电话取消',
    },
    {
      value: 0,
      label: '自定义原因',
    },
  ]
  get orderList() {
    return [
    {
      label: this.$t('orders.allOrders'),
      value: 0,
    },
    {
      label: this.$t('orders.pendingPayment'),
      value: 1,
    },
    {
      label: this.$t('orders.awaitingAcceptance'),
      value: 2,
    },
    {
      label: this.$t('orders.awaitingDelivery'),
      value: 3,
    },
    {
      label: this.$t('orders.delivering'),
      value: 4,
    },
    {
      label: this.$t('orders.completed'),
      value: 5,
    },
    {
      label: this.$t('orders.cancelled'),
      value: 6,
    },
    ]
  }

  get normalizedOrderDetails() {
    const details = this.diaForm && (this.diaForm.orderDetailList || this.diaForm.orderDetails || this.diaForm.orderItems)
    if (Array.isArray(details) && details.length) {
      return details.map((item: any) => ({
        ...item,
        name: item.name || item.dishName || item.setmealName || item.itemName || '—',
        number: item.number != null ? item.number : (item.quantity != null ? item.quantity : 1),
        amount: this.orderItemAmount(item) == null ? null : this.orderItemAmount(item) * Number(item.number != null ? item.number : (item.quantity != null ? item.quantity : 1))
      }))
    }
    return String(orderMenuSummary(this.diaForm && this.diaForm.orderDishes) || '')
      .split(';')
      .map((item) => item.trim())
      .filter(Boolean)
      .map((item) => {
        const match = item.match(/\*(\d+)$/)
        return { name: match ? item.slice(0, match.index) : item, number: match ? Number(match[1]) : 1 }
      })
  }

  get dishSubtotal() {
    const total = Number(this.diaForm.amount)
    const delivery = Number(this.diaForm.deliveryFee) || 0
    const packing = Number(this.diaForm.packAmount) || 0
    return Number.isFinite(total) ? Math.max(0, total - delivery - packing) : 0
  }

  formatMoney(value: any) {
    const amount = Number(value)
    return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
  }

  orderItemAmount(item: any) {
    const values = [item.amount, item.price, item.unitPrice, item.totalAmount, item.lineAmount]
    const validPrices = values.filter((value) => value !== null && value !== undefined && value !== '' && Number.isFinite(Number(value)))
    return validPrices.find((value) => Number(value) > 0) != null
      ? Number(validPrices.find((value) => Number(value) > 0))
      : validPrices.length ? Number(validPrices[0]) : null
  }

  paymentMethodLabel(value: any) {
    if (Number(value) === 1) return this.$t('orders.wechatPay')
    if (Number(value) === 2) return this.$t('orders.alipayPay')
    return '—'
  }

  statusLabel(status: number) {
    const item = this.orderList.find((entry: any) => entry.value === status)
    return item ? item.label : status === 7 ? this.$t('orders.refunded') : '—'
  }

  confirmOperation(messageKey: string) {
    return this.$confirm(
      String(this.$t(messageKey)),
      String(this.$t('orders.confirmationTitle')),
      {
        confirmButtonText: String(this.$t('common.confirm')),
        cancelButtonText: String(this.$t('common.cancel')),
        type: 'warning'
      }
    )
  }

  created() {
    this.init(Number(this.$route.query.status) || 0)
  }

  mounted() {
    //如果有值说明是消息通知点击进来的
    if (
      this.$route.query.orderId &&
      this.$route.query.orderId !== 'undefined'
    ) {
      this.goDetail(this.$route.query.orderId, 2)
    }
    if (this.$route.query.status) {
      this.defaultActivity = this.$route.query.status
    }
    // console.log(this.$route.query, 'this.$route')
  }

  initFun(orderStatus) {
    this.page = 1
    this.init(orderStatus)
  }

  change(activeIndex) {
    if (activeIndex === this.orderStatus) return
    this.init(activeIndex)
    this.input = ''
    this.phone = ''
    this.valueTime = []
    this.dialogOrderStatus = 0
    this.$router.push('/order')
    console.log(activeIndex, '接收到了子组件的index')
  }

  //获取待处理，待派送，派送中数量
  getOrderListBy3Status() {
    getOrderListBy({})
      .then((res) => {
        if (res.data.code === 1) {
          this.orderStatics = res.data.data
        } else {
          this.$message.error(res.data.msg)
        }
      })
      .catch((err) => {
        this.$message.error(err && err.message ? err.message : String(this.$t('apiErrors.requestFailed')))
      })
  }

  init(activeIndex: number = 0, isSearch?) {
    this.isSearch = isSearch
    this.orderStatus = activeIndex
    const params = {
      page: this.page,
      pageSize: this.pageSize,
      number: this.input || undefined,
      phone: this.phone || undefined,
      beginTime:
        this.valueTime && this.valueTime.length > 0
          ? this.valueTime[0]
          : undefined,
      endTime:
        this.valueTime && this.valueTime.length > 0
          ? this.valueTime[1]
          : undefined,
      status: activeIndex || undefined,
    }
    this.listLoading = true
    getOrderDetailPage({ ...params })
      .then((res) => {
        if (res && res.data && res.data.code === 1 && res.data.data) {
          this.tableData = Array.isArray(res.data.data.records) ? res.data.data.records : []
          this.orderStatus = activeIndex
          this.counts = Number(res.data.data.total)
          this.getOrderListBy3Status()
          if (
            this.dialogOrderStatus === 2 &&
            this.orderStatus === 2 &&
            this.isAutoNext &&
            this.autoNextAfterAction &&
            !this.isTableOperateBtn &&
            this.tableData.length > 0
          ) {
            const nextOrder = this.tableData.find((item: any) => item.id !== this.autoNextExcludeOrderId)
            this.autoNextAfterAction = false
            this.autoNextExcludeOrderId = ''
            if (nextOrder) this.goDetail(nextOrder.id, nextOrder.status, nextOrder)
          } else {
            this.autoNextAfterAction = false
          }
        } else {
          this.tableData = []
          this.counts = 0
          this.$message.error(res && res.data && res.data.msg ? res.data.msg : String(this.$t('orders.requestError')))
        }
      })
      .catch((err) => {
        this.tableData = []
        this.counts = 0
        this.$message.error(err && err.message ? err.message : String(this.$t('apiErrors.requestFailed')))
      })
      .finally(() => {
        this.listLoading = false
      })
  }

  getOrderType(row: any) {
    if (row.status === 1) {
      return this.$t('orders.pendingPayment')
    } else if (row.status === 2) {
      return this.$t('orders.awaitingAcceptance')
    } else if (row.status === 3) {
      return this.$t('orders.awaitingDelivery')
    } else if (row.status === 4) {
      return this.$t('orders.delivering')
    } else if (row.status === 5) {
      return this.$t('orders.completed')
    } else if (row.status === 6) {
      return this.$t('orders.cancelled')
    } else {
      return this.$t('orders.refunded')
    }
  }

  isScheduledDelivery(order: any) {
    return order && isScheduledDeliveryStatus(order.deliveryStatus)
  }

  deliveryTypeLabel(status: any) {
    if (Number(status) === 1) return this.$t('orders.immediateDelivery')
    if (Number(status) === 0) return this.$t('orders.scheduledDelivery')
    return this.$t('orders.deliveryTypeUnknown')
  }

  deliveryTimeLabel(status: any, orderStatus: any) {
    if (Number(status) === 1 && Number(orderStatus) === 5) return this.$t('orders.deliveryTime')
    return Number(status) === 0 ? this.$t('orders.scheduledDeliveryTime') : this.$t('orders.estimatedDeliveryTime')
  }

  tablewareLabel(status: any, number: any) {
    const display = resolveTablewareDisplay(status, number)
    if (display.type === 'none') return this.$t('orders.noCutlery')
    if (display.type === 'sets') return this.$t('orders.tablewareSets', { count: display.count })
    if (display.type === 'byMeal') return this.$t('orders.tablewareByMeal', { count: display.count })
    return this.$t('orders.tablewareUnknown')
  }

  // 查看详情
  async goDetail(id: any, status: number, row?: any) {
    const requestSequence = ++this.detailRequestSequence
    this.diaForm = {}
    this.dialogVisible = true
    this.dialogOrderStatus = status
    this.orderId = id
    this.detailLoading = true
    this.row = row || { id: this.$route.query.orderId, status: status }
    try {
      const response = await queryOrderDetailById({ orderId: id })
      const body = response && response.data
      if (!body || body.code !== 1) {
        throw new Error((body && (body.msg || body.message)) || 'Invalid order detail response')
      }
      const detail = body.data
      if (!detail || typeof detail !== 'object') throw new Error('Invalid order detail response')
      if (requestSequence !== this.detailRequestSequence) return
      this.diaForm = Object.assign({}, detail)
      if (detail.status != null && Number.isFinite(Number(detail.status))) {
        this.dialogOrderStatus = Number(detail.status)
      }
      this.row = Object.assign({ id: id, status: status }, detail)
      if (this.$route.query.orderId) this.$router.push('/order')
    } catch (error) {
      if (requestSequence !== this.detailRequestSequence) return
      const detail = error && error.response && error.response.data
      const reason = (detail && (detail.msg || detail.message)) || (error && error.message) || ''
      this.$message.error(String(this.$t('orders.requestError')) + reason)
    } finally {
      if (requestSequence === this.detailRequestSequence) this.detailLoading = false
    }
  }

  //打开拒单弹窗
  orderReject(row: any) {
    this.cancelDialogVisible = true
    this.orderId = row.id
    this.dialogOrderStatus = row.status
    this.cancelDialogTitle = '拒绝'
    this.dialogVisible = false
    this.cancelReason = ''
  }

  //接单
  orderAccept(row: any) {
    const fromDetail = !this.isTableOperateBtn
    this.orderId = row.id
    this.dialogOrderStatus = row.status
    this.confirmOperation('orders.confirmAccept')
      .then(() => orderAccept({ id: this.orderId }))
      .then((res) => {
        if (res.data.code === 1) {
          this.autoNextAfterAction = fromDetail && this.isAutoNext
          this.autoNextExcludeOrderId = this.orderId
          this.$message.success(String(this.$t('orders.operationSuccessful')))
          this.orderId = ''
          // this.dialogOrderStatus = 0
          this.dialogVisible = false
          this.init(this.orderStatus)
        } else {
          this.$message.error(res.data.msg)
        }
      })
      .catch((err) => {
        if (err === 'cancel' || err === 'close') return
        this.$message.error(err && err.message ? err.message : String(this.$t('apiErrors.requestFailed')))
      })
  }

  //打开取消订单弹窗
  cancelOrder(row: any) {
    this.cancelDialogVisible = true
    this.orderId = row.id
    this.dialogOrderStatus = row.status
    this.cancelDialogTitle = '取消'
    this.dialogVisible = false
    this.cancelReason = ''
  }

  //确认取消或拒绝订单并填写原因
  confirmCancel(type) {
    if (!this.cancelReason) {
      return this.$message.error(String(this.$t('orders.selectReason')))
    } else if (this.cancelReason === '自定义原因' && !this.remark) {
      return this.$message.error(String(this.$t('orders.enterCustomReason')))
    }

    const fromDetail = !this.isTableOperateBtn
    const cancelRequest = this.cancelDialogTitle === '取消' ? orderCancel : orderReject
    cancelRequest({
      id: this.orderId,
      // eslint-disable-next-line standard/computed-property-even-spacing
      [this.cancelDialogTitle === '取消' ? 'cancelReason' : 'rejectionReason']:
        this.cancelReason === '自定义原因' ? this.remark : this.cancelReason,
    })
      .then((res) => {
        if (res.data.code === 1) {
          this.autoNextAfterAction = this.cancelDialogTitle === '拒绝' && fromDetail && this.isAutoNext
          this.autoNextExcludeOrderId = this.orderId
          this.$message.success(String(this.$t('orders.operationSuccessful')))
          this.cancelDialogVisible = false
          this.orderId = ''
          // this.dialogOrderStatus = 0
          this.init(this.orderStatus)
        } else {
          this.$message.error(res.data.msg)
        }
      })
      .catch((err) => {
        this.$message.error(err && err.message ? err.message : String(this.$t('apiErrors.requestFailed')))
      })
  }

  // 派送，完成
  cancelOrDeliveryOrComplete(status: number, id: string) {
    const params = {
      status,
      id,
    }
    this.confirmOperation(status === 3 ? 'orders.confirmDeliver' : 'orders.confirmComplete')
      .then(() => (status === 3 ? deliveryOrder : completeOrder)(params))
      .then((res) => {
        if (res.data.code === 1) {
          this.$message.success(String(this.$t('orders.operationSuccessful')))
          this.orderId = ''
          // this.dialogOrderStatus = 0
          this.dialogVisible = false
          this.init(this.orderStatus)
        } else {
          this.$message.error(res.data.msg)
        }
      })
      .catch((err) => {
        if (err === 'cancel' || err === 'close') return
        this.$message.error(String(this.$t('orders.requestError')) + err.message)
      })
  }

  handleClose() {
    this.detailRequestSequence++
    this.dialogVisible = false
    this.detailLoading = false
    this.diaForm = {}
  }

  private handleSizeChange(val: any) {
    this.pageSize = val
    this.init(this.orderStatus)
  }

  private handleCurrentChange(val: any) {
    this.page = val
    this.init(this.orderStatus)
  }
}
</script>

<style lang="scss" scoped>
.dashboard {
  &-container {
    margin: 30px;
    // height: 100%;
    min-height: 700px;
    .container {
      background: #fff;
      position: relative;
      z-index: 1;
      padding: 30px 28px;
      border-radius: 4px;
      // min-height: 650px;
      height: calc(100% - 55px);

      .tableBar {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 12px 20px;
        margin-bottom: 20px;
        justify-content: flex-start;

        .filter-item {
          display: flex;
          flex: 1 1 300px;
          align-items: center;
          gap: 10px;
          min-width: 0;

          label {
            flex: 0 0 auto;
            white-space: nowrap;
          }

          .el-input,
          .el-date-editor {
            flex: 1 1 auto;
            width: auto !important;
            min-width: 0;
          }
        }

        .filter-date {
          flex: 0 1 500px;

          .el-date-editor {
            max-width: 360px;
          }
        }

        .tableLab {
          span {
            cursor: pointer;
            display: inline-block;
            font-size: 14px;
            padding: 0 20px;
            color: $gray-2;
            border-right: solid 1px $gray-4;
          }
        }
      }

      .tableBox {
        width: 100%;
        border: 1px solid $gray-5;
        border-bottom: 0;
      }

      .order-table {
        .cell {
          white-space: nowrap;
          word-break: keep-all;
        }

        th .cell {
          white-space: normal;
          overflow-wrap: anywhere;
        }

        .order-actions {
          display: flex;
          flex-direction: column;
          align-items: stretch;
          justify-content: center;
          white-space: nowrap;
          gap: 2px;

          .el-button {
            margin: 0;
            padding: 3px 0;
          }
        }
      }

      .pageList {
        text-align: center;
        margin-top: 30px;
      }
      //查询黑色按钮样式
      .normal-btn {
        background: #333333;
        color: white;
        margin-left: 0;
      }
    }
    .hContainer {
      height: auto !important;
    }
  }
}

.search-btn {
  margin-left: 20px;
}

.info-box {
  margin: -15px -44px 20px;
  p {
    display: flex;
    height: 20px;
    line-height: 20px;
    font-size: 14px;
    font-weight: 400;
    color: #666666;
    text-align: left;
    margin-bottom: 14px;
    &:last-child {
      margin-bottom: 0;
    }
    label {
      width: 100px;
      display: inline-block;
      color: #666;
    }
    .des {
      flex: 1;
      color: #333333;
    }
  }
}

.order-top {
  // height: 80px;
  border-bottom: 1px solid #e7e6e6;
  padding-bottom: 26px;
  padding-left: 22px;
  padding-right: 22px;
  // margin: 0 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  .order-top-main {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    width: 100%;
    min-width: 0;
  }
  .order-identifier {
    display: flex;
    align-items: baseline;
    gap: 6px;
    white-space: nowrap;
  }
  .order-date {
    white-space: nowrap;
  }
  .order-status {
    width: auto;
    min-width: 100px;
    padding: 0 12px;
    height: 27px;
    background: #333333;
    border-radius: 13.5px;
    color: white;
    margin-left: 0;
    text-align: center;
    line-height: 27px;
    flex-shrink: 0;
  }
  .status3 {
    background: #f56c6c;
  }
  p {
    color: #333;
    label {
      color: #666;
    }
  }
  .order-num {
    font-size: 16px;
    color: #2a2929;
    font-weight: bold;
    display: inline-block;
  }
}

.order-middle {
  .user-info {
    min-height: 140px;
    background: #fbfbfa;
    margin-top: 23px;

    padding: 20px 43px;
    color: #333;
    .user-info-box {
      min-height: 55px;
      display: grid;
      grid-template-columns: minmax(0, 0.82fr) minmax(0, 1.18fr);
      column-gap: 36px;
      row-gap: 16px;
      align-items: center;

      > div {
        display: flex;
        align-items: center;
        min-width: 0;
      }

      .user-name {
        grid-column: 1;
        grid-row: 2;
      }
      .user-phone {
        grid-column: 2;
        grid-row: 2;
      }
      .delivery-summary {
        grid-column: 1 / -1;
        grid-row: 1;
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 12px;
        min-width: 0;
        padding: 10px 14px;
        border-radius: 4px;
        background: #fff8e8;
        border-left: 4px solid #e6a23c;
        color: #303133;
        strong {
          color: #b87412;
          font-size: 16px;
          white-space: nowrap;
        }
        span {
          white-space: normal;
          overflow-wrap: anywhere;
        }
        &.scheduled {
          background: #eef5ff;
          border-left-color: #409eff;
          strong {
            color: #2174c7;
          }
        }
      }
      label {
        flex: 0 0 auto;
        margin-right: 12px;
        color: #666;
        white-space: nowrap;
      }
      span {
        min-width: 0;
        text-align: left;
      }
      .user-name span,
      .user-phone span {
        white-space: nowrap;
      }

      .user-address {
        grid-column: 1 / -1;
        grid-row: 3;
        white-space: normal;
      }

      .user-address span {
        overflow-wrap: anywhere;
        white-space: normal;
      }
    }
    .user-remark {
      min-height: 43px;
      line-height: 1.4;
      background: #fffbf0;
      border: 1px solid #fbe396;
      border-radius: 4px;
      margin-top: 0;
      padding: 6px;
      display: flex;
      align-items: center;
      div {
        display: inline-block;
        flex: 0 0 120px;
        min-width: 120px;
        min-height: 32px;
        background: #fbe396;
        border-radius: 4px;
        text-align: center;
        line-height: 1.4;
        padding: 7px 4px;
        color: #333;
        margin-right: 12px;
        // padding: 12px 6px;
      }
      span {
        color: #f2a402;
        line-height: 1.4;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        min-width: 0;
      }
    }
    .orderCancel > div {
      flex: 0 0 auto;
      min-width: 0;
      white-space: nowrap;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 7px 10px;
    }
    .fulfillment-info {
      display: grid;
      gap: 8px;
      margin-top: 14px;
    }
    .orderCancel {
      background: #ffffff;
      border: 1px solid #b6b6b6;

      div {
        padding: 7px 10px;
        background-color: #e5e4e4;
      }
      span {
        color: #f56c6c;
      }
    }
  }
  .dish-info {
    // min-height: 180px;
    display: flex;
    flex-wrap: wrap;
    padding: 20px 40px;
    border-bottom: 1px solid #e7e6e6;
    .dish-label {
      color: #666;
    }
    .dish-list {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-wrap: wrap;
      .dish-item {
        flex: 0 0 100%;
        margin-bottom: 14px;
        color: #333;
        display: flex;
        align-items: baseline;
        gap: 16px;
        .dish-name {
          flex: 1 1 auto;
          min-width: 0;
          word-break: normal;
          overflow-wrap: break-word;
          hyphens: auto;
        }
        .dish-num,
        .dish-price {
          white-space: nowrap;
        }
        .dish-price {
          margin-left: auto;
          min-width: 100px;
          text-align: right;
        }
      }
    }
    .dish-label {
      margin-right: 65px;
    }
    .dish-all-amount {
      flex: 1;
      padding-left: 92px;
      margin-top: 10px;
      label {
        color: #333333;
        font-weight: bold;
        margin-right: 5px;
      }
      span {
        color: #f56c6c;
      }
    }
  }
}

.order-bottom {
  .amount-info {
    // min-height: 180px;
    display: flex;
    flex-wrap: wrap;
    padding: 20px 40px;
    padding-bottom: 0px;
    .amount-label {
      color: #666;
      margin-right: 65px;
    }
    .amount-list {
      flex: 80%;
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
      column-gap: 32px;
      row-gap: 16px;
      align-items: start;
      color: #333;
      line-height: 1.5;
      > div {
        display: flex;
        align-items: baseline;
        gap: 12px;
        min-width: 0;
      }
      .amount-name,
      .pay-name,
      .pay-value,
      .amount-price {
        white-space: nowrap;
      }
      .all-amount {
        .amount-name {
          margin-right: 0;
        }
        .amount-price {
          color: #f56c6c;
        }
      }
    }
  }
}
@media (max-width: 760px) {
  .order-middle .user-info .user-info-box {
    grid-template-columns: minmax(0, 1fr);
    .user-name,
    .user-phone,
    .user-address {
      grid-column: 1;
      grid-row: auto;
    }
    .delivery-summary {
      grid-column: 1;
    }
    .user-address,
    .user-address span {
      white-space: normal;
      overflow-wrap: anywhere;
    }
  }
}
</style>

<style lang="scss">
.dashboard-container {
  .order-table td .cell {
    white-space: nowrap;
    word-break: normal;
  }

  .cancelReason {
    padding-left: 40px;
  }
  .cancelTime {
    padding-left: 50px;
  }
  .orderTime {
    padding-left: 50px;
  }
  td.operate .cell {
    .before,
    .middle,
    .after {
      height: 39px;
      width: 48px;
    }
  }
  td.operate .cell,
  td.otherOperate .cell {
    display: flex;
    flex-wrap: nowrap;
    justify-content: center;
  }
  .order-dialog {
    .el-dialog {
      max-height: 764px !important;
      display: flex;
      flex-direction: column;
      margin: 0 !important;
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      max-height: calc(100% - 30px);
      max-width: calc(100% - 30px);
    }
    .el-dialog__body {
      height: 520px !important;
    }
  }
}
.el-dialog__body {
  padding-top: 34px;
  padding-left: 30px;
  padding-right: 30px;
}
.cancelDialog {
  .el-form-item__label {
    width: 175px !important;
    white-space: nowrap;
  }
  .el-dialog__body {
    padding-left: 40px;
  }
  .el-select,
  .el-textarea {
    width: 293px;
  }
  .el-textarea textarea {
    height: 114px;
  }
}
.el-dialog__footer {
  .el-checkbox {
    float: left;
    margin-left: 40px;
  }
  .el-checkbox__label {
    color: #333333 !important;
  }
}
.empty-box {
  display: flex;
  align-items: center;
  justify-content: center;
  img {
    margin-top: 0 !important;
  }
}
</style>
